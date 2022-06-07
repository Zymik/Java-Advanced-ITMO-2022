package info.kgeorgiy.ja.kosolapov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class ParallelMapperImpl implements ParallelMapper {
    public static final int MAX_QUEUE_SIZE = 15_000;

    private final Queue<Runnable> queue = new ArrayDeque<>();
    private final List<Thread> threads;

    /**
     * Create {@code ParallelMapperImpl} that run on {@code thread} threads
     *
     * @param thread count of threads
     */
    public ParallelMapperImpl(final int thread) {
        final Runnable runner = () -> {
            try {
                while (!Thread.interrupted()) {
                    pollTask().run();
                }
            } catch (final InterruptedException ignored) {
            }
        };

        threads = Stream.generate(() -> new Thread(runner))
                .limit(thread)
                .peek(Thread::start)
                .toList();
    }

    private Runnable pollTask() throws InterruptedException {
        synchronized (queue) {
            while (queue.isEmpty()) {
                queue.wait();
            }
            final Runnable runnable = queue.poll();
            queue.notifyAll();
            return runnable;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <T, R> List<R> map(
            final Function<? super T, ? extends R> f,
            final List<? extends T> args
    ) throws InterruptedException {
        final TaskGroup taskGroup = new TaskGroup(args.size());
        final List<Task<T, R>> tasks = args
                .stream()
                .map(x -> new Task<T, R>(taskGroup, x, f))
                .toList();
        for (final var t : tasks) {
            addToQueue(t);
        }

        taskGroup.waitAll();
        RuntimeException exception = null;

        for (final var i : tasks) {
            final RuntimeException e = i.getException();
            if (e != null) {
                if (exception == null) {
                    exception = e;
                } else {
                    exception.addSuppressed(e);
                }
            }
        }

        if (exception != null) {
            throw exception;
        }

        return tasks.stream().map(Task::getResult).toList();
    }

    private <T, R> void addToQueue(final Task<T, R> task) throws InterruptedException {
        synchronized (queue) {
            while (queue.size() > MAX_QUEUE_SIZE) {
                queue.wait();
            }
            queue.add(task);
            queue.notifyAll();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        ThreadFinisher.finishThreads(threads);
    }

    private static final class TaskGroup {
        private int counter;

        private TaskGroup(final int expected) {
            this.counter = expected;
        }


        private synchronized void decrementAndTest() {
            if (--counter == 0) {
                notify();
            }
        }

        private synchronized void waitAll() throws InterruptedException {
            while (counter > 0) {
                wait();
            }
        }
    }

    private static class Task<T, R> implements Runnable {
        private final T task;
        // :NOTE: volatile
        private R result;
        private RuntimeException exception;
        private final TaskGroup taskGroup;
        private final Function<? super T, ? extends R> function;

        private Task(
                final TaskGroup taskGroup, final T task,
                     final Function<? super T, ? extends R> function
        ) {
            this.task = task;
            this.taskGroup = taskGroup;
            this.function = function;
        }

        @Override
        public void run() {
            try {
                result = function.apply(task);
            } catch (final RuntimeException e) {
                exception = e;
            } finally {
                taskGroup.decrementAndTest();
            }
        }

        public RuntimeException getException() {
            return exception;
        }

        public R getResult() {
            return result;
        }
    }

}
