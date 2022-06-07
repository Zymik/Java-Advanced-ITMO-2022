package info.kgeorgiy.ja.kosolapov.arrayset;

import java.util.*;
import java.util.function.ToIntFunction;

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private enum ComparatorOrder {
        NATURAL_ORDER {
            public ComparatorOrder reverse() {
                return REVERSED_NATURAL_ORDER;
            }
        },
        REVERSED_NATURAL_ORDER {
            public ComparatorOrder reverse() {
                return NATURAL_ORDER;
            }
        },
        UNKNOWN_ORDER {
            public ComparatorOrder reverse() {
                return UNKNOWN_ORDER;
            }
        };

        public abstract ComparatorOrder reverse();
    }

    public Comparator<? super E> comparator;

    private final ReversibleSortedList<E> list;
    private final ComparatorOrder order;

    private ArraySet(Collection<? extends E> collection, Comparator<? super E> comparator,
                     ComparatorOrder order) {
        this.order = order;
        this.comparator = comparator;
        NavigableSet<E> set = new TreeSet<>(comparator);
        set.addAll(collection);
        this.list = new ReversibleSortedList<>(List.copyOf(set), comparator);
    }

    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comparator) {
        this(collection, Objects.requireNonNullElseGet(comparator, ArraySet::getNaturalOrder),
                comparator == null ? ComparatorOrder.NATURAL_ORDER : ComparatorOrder.UNKNOWN_ORDER);
    }

    public ArraySet() {
        this(Collections.emptyList());
    }


    public ArraySet(Collection<? extends E> collection) {
        // :NOTE: invoke constructor upper (solved)
        this(collection, getNaturalOrder(), ComparatorOrder.NATURAL_ORDER);

    }

    private ArraySet(ReversibleSortedList<E> list, Comparator<? super E> comparator, ComparatorOrder order) {
        this.list = list;
        this.comparator = comparator;
        this.order = order;
    }

    private static <E> Comparator<E> getNaturalOrder() {
        // :NOTE: move to constant
        // А как константа и дженирики?
        return (a, b) -> {
            @SuppressWarnings("unchecked")
            var t = (Comparable<E>) a;
            return t.compareTo(b);
        };
    }


    private E bound(E e, ToIntFunction<E> boundFunction, int nullPoint) {
        int i = boundFunction.applyAsInt(e);
        return i == nullPoint ? null : list.get(i);
    }

    @Override
    public E lower(E e) {
        // :NOTE: copypaste(solved)
        return bound(e, list::lower, -1);
    }

    @Override
    public E floor(E e) {
        return bound(e, list::floor, -1);
    }

    @Override
    public E ceiling(E e) {
        return bound(e, list::ceiling, size());
    }

    @Override
    public E higher(E e) {
        return bound(e, list::higher, size());
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return list.iterator();
    }


    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(list.reverse(),
                Collections.reverseOrder(comparator),
                order.reverse());
    }

    @Override
    public Iterator<E> descendingIterator() {
        return list.descentIterator();
    }

    private int getCeilingBorder(E element, ToIntFunction<E> include,
                                 ToIntFunction<E> exclude, boolean inclusive) {
        if (inclusive) {
            return include.applyAsInt(element);
        }
        return exclude.applyAsInt(element);
    }

    private int getCeilingInclusive(E element, boolean inclusive) {
        // :NOTE: copypaste(solved)
        return getCeilingBorder(element, list::higher, list::ceiling, inclusive);
    }

    private int getFloorInclusive(E element, boolean inclusive) {
        return getCeilingBorder(element, list::ceiling, list::higher, inclusive);
    }

    private ArraySet<E> subIndexes(int i, int j) {
        return new ArraySet<>(list.subList(i, j), comparator, order);
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        if (comparator.compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("fromKey > toKey");
        }
        int i = getFloorInclusive(fromElement, fromInclusive);
        int j = getCeilingInclusive(toElement, toInclusive);
        return subIndexes(i, j);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        int j = getCeilingInclusive(toElement, inclusive);
        return subIndexes(0, j);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        int j = getFloorInclusive(fromElement, inclusive);
        return subIndexes(j, size());
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public Comparator<? super E> comparator() {
        return order == ComparatorOrder.NATURAL_ORDER ? null : comparator;
    }

    @Override
    public E first() {
        if (isEmpty()) {
            throw new NoSuchElementException("Unable to find first element in empty set");
        }
        return list.first();
    }

    @Override
    public E last() {
        if (isEmpty()) {
            throw new NoSuchElementException("Unable to find last element in empty set");
        }
        return list.last();
    }

}
