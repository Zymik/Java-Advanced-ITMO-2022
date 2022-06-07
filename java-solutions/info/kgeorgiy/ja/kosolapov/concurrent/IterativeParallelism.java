package info.kgeorgiy.ja.kosolapov.concurrent;


import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {
    private final ParallelMapper parallelMapper;

    /**
     * Create {@code IterativeParallelsim} that runs on {@code parallelMapper}
     *
     * @param parallelMapper parallel mapper to run on
     */
    public IterativeParallelism(final ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    /**
     * Creates {@code IIterativeParallelism} that generates own threads to run on
     */
    public IterativeParallelism() {
        this(null);
    }

    /**
     * Split list into {@code count} lists with size difference no more than one
     *
     * @param count expected count of list
     * @param list  list that will be split
     * @param <T>   type of list arguments
     * @return split list of lists. If {@code count <= list.size()} return {@code list.size()} list with size 1
     */
    public static <T> List<List<? extends T>> splitToLists(final int count, final List<? extends T> list) {
        return IterativeParallelism.<T>split(count, list).toList();
    }

    private static <T> Stream<List<? extends T>> split(final int count, final List<? extends T> list) {
        final int size = list.size() / count;
        final int mod = list.size() % count;
        final int countOfThreads = Math.min(count, list.size());

        return IntStream.range(0, countOfThreads)
                .mapToObj(i -> {
                            final int leftBorder = Math.min(i, mod) + i * size;
                            final int rightBorder = leftBorder + size + (i < mod ? 1 : 0);
                            return list.subList(leftBorder, rightBorder);
                        }
                );
    }

    private <T, E> Stream<? extends E> runOnThreadsAndGetResult(
            final Stream<List<? extends T>> listStream,
            final Function<List<? extends T>, ? extends E> function
    ) throws InterruptedException {
        if (parallelMapper != null) {
            return parallelMapper.map(function, listStream.toList())
                    .stream();
        }

        // :NOTE: Переусложнение
        final List<RunnableResult<T, E>> runnableList =
                listStream
                        .map(list -> new RunnableResult<T, E>(list, function))
                        .toList();

        final var threads =
                runnableList
                        .stream()
                        .map(Thread::new)
                        .peek(Thread::start)
                        .toList();
        try {
            for (var i : threads) {
                i.join();
            }
        } catch (InterruptedException e) {
            ThreadFinisher.finishThreads(threads);
            throw new InterruptedException("Unexpected Interruption in IterativeParallelism(): " + e.getMessage());
        }

        return runnableList
                .stream()
                .map(RunnableResult::getResult);
    }

    private <T, E, P> P applyFunctionWithThreadsAndCollect(
            final int threads, final List<? extends T> list,
            final Function<List<? extends T>, ? extends E> function,
            final Function<Stream<? extends E>, ? extends P> collector) throws InterruptedException {
        return collector.apply(runOnThreadsAndGetResult(split(threads, list), function));
    }


    private <T> T applyStreamFunctionWithThreadsAndCollect(final int threads, final List<? extends T> values,
                                                           final Function<Stream<? extends T>, ? extends T> function)
            throws InterruptedException {
        return applyStreamFunctionWithThreadsAndCollect(
                threads,
                values,
                function,
                function
        );
    }

    private <T, E, P> P applyStreamFunctionWithThreadsAndCollect(final int threads, final List<? extends T> values,
                                                                 final Function<Stream<? extends T>, ? extends E> function,
                                                                 final Function<Stream<? extends E>, ? extends P> collector)
            throws InterruptedException {
        return applyFunctionWithThreadsAndCollect(
                threads,
                values,
                l -> function.apply(l.stream()),
                collector
        );
    }

    private <T, E> List<E> applyWithFlattenCollector(final int threads, final List<? extends T> values,
                                                     final Function<Stream<? extends T>, Stream<? extends E>> function)
            throws InterruptedException {
        return applyStreamFunctionWithThreadsAndCollect(
                threads,
                values,
                l -> function.apply(l).toList(),
                s -> s.<E>flatMap(Collection::stream).toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return applyStreamFunctionWithThreadsAndCollect(
                threads,
                values,
                l -> l.map(String::valueOf).collect(Collectors.joining()),
                s -> s.collect(Collectors.joining()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        // :NOTE: Дубли
        return applyWithFlattenCollector(
                threads,
                values,
                s -> s.filter(predicate));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f)
            throws InterruptedException {
        return applyWithFlattenCollector(
                threads,
                values,
                s -> s.map(f));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator)
            throws InterruptedException {
        return minimum(threads, values, comparator.reversed());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator)
            throws InterruptedException {
        return applyStreamFunctionWithThreadsAndCollect(
                threads,
                values,
                l -> l.min(comparator).orElse(null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return applyStreamFunctionWithThreadsAndCollect(
                threads,
                values,
                l -> l.allMatch(predicate),
                s -> s.allMatch(Boolean::booleanValue));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }


    private static class RunnableResult<T, E> implements Runnable {
        private final List<? extends T> list;
        private final Function<List<? extends T>, ? extends E> function;
        private E result;

        RunnableResult(final List<? extends T> list,
                       final Function<List<? extends T>, ? extends E> function) {
            this.list = list;
            this.function = function;
        }

        @Override
        public void run() {
            result = function.apply(list);
        }

        public E getResult() {
            return result;
        }
    }

}
