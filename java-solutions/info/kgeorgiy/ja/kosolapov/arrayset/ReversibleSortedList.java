package info.kgeorgiy.ja.kosolapov.arrayset;

import java.util.*;

public class ReversibleSortedList<E>
        extends AbstractList<E> implements List<E>, RandomAccess {
    private final List<E> list;
    private final boolean reversed;
    private final Comparator<? super E> comparator;

    public ReversibleSortedList(List<E> sortedImmutableList, Comparator<? super E> comparator) {
        this(Collections.unmodifiableList(sortedImmutableList), false, comparator);
    }

    private ReversibleSortedList(List<E> list,
                                 boolean reversed,
                                 Comparator<? super E> comparator) {
        this.list = list;
        this.reversed = reversed;
        this.comparator = comparator;
    }


    public ReversibleSortedList<E> reverse() {
        return new ReversibleSortedList<>(list, !reversed, Collections.reverseOrder(comparator));
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean contains(Object o) {
        @SuppressWarnings("unchecked")
        E e = (E) o;
        return binarySearch(e) >= 0;
    }

    public Iterator<E> descentIterator() {
        return reverse().iterator();
    }

    private int index(int i) {
        return !reversed ? i : size() - i - 1;
    }

    @Override
    public E get(int index) {
        return list.get(index(index));
    }

    @Override
    public int indexOf(Object o) {
        @SuppressWarnings("unchecked")
        E e = (E) o;
        int i = binarySearch(e);
        return i >= 0 && i != size() ? i : -1;
    }

    //Contract of Collections.binarySearch(list, e, comparator)
    public int binarySearch(E e) {
        return Collections.binarySearch(this, e, comparator);
    }

    private int insertionPoint(int i) {
        return -i - 1;
    }

    public int lower(E e) {
        int i = binarySearch(e);
        if (i >= 0) {
            return i - 1;
        }
        return insertionPoint(i) - 1;
    }

    public int floor(E e) {
        int i = binarySearch(e);
        if (i >= 0 && i < size()) {
            return i;
        }
        if (i != size()) {
            i = insertionPoint(i);
        }
        return i - 1;
    }

    public int ceiling(E e) {
        int i = binarySearch(e);
        if (i >= 0) {
            return i;
        }
        return insertionPoint(i);
    }


    public int higher(E e) {
        int i = binarySearch(e);
        if (i >= 0 && i < size()) {
            return i + 1;
        }
        if (i == size()) {
            return size();
        }
        return insertionPoint(i);
    }

    public E first() {
        return get(0);
    }

    public E last() {
        return get(size() - 1);
    }

    @Override
    public int lastIndexOf(Object o) {
        return indexOf(o);
    }

    @Override
    public ReversibleSortedList<E> subList(int fromIndex, int toIndex) {
        if (reversed) {
            int i = fromIndex;
            fromIndex = index(toIndex - 1);
            toIndex = index(i) + 1;
        }
        return new ReversibleSortedList<>(list.subList(fromIndex, toIndex), reversed, comparator);
    }
}
