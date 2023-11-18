package io.github.createsequence.core.util;

import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * @author huangchengxing
 */
@RequiredArgsConstructor
public class DistinctLinkedList<E> implements Deque<E> {

    /**
     * 去重集合
     */
    private final Set<E> accessed;

    /**
     * 在去重后是否移除访问标记
     */
    private boolean cancelAccessAfterRemove;

    /**
     * 双向链表
     */
    private final Deque<E> delegate;

    @Override
    public void addFirst(E e) {
        operateDelegateIfNotAccessed(e, Deque::addFirst);
    }

    @Override
    public void addLast(E e) {
        operateDelegateIfNotAccessed(e, Deque::addLast);
    }

    @Override
    public boolean offerFirst(E e) {
        return false;
    }

    @Override
    public boolean offerLast(E e) {
        return false;
    }

    @Override
    public E removeFirst() {
        return null;
    }

    @Override
    public E removeLast() {
        return null;
    }

    @Override
    public E pollFirst() {
        return null;
    }

    @Override
    public E pollLast() {
        return null;
    }

    @Override
    public E getFirst() {
        return null;
    }

    @Override
    public E getLast() {
        return null;
    }

    @Override
    public E peekFirst() {
        return null;
    }

    @Override
    public E peekLast() {
        return null;
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        return false;
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        return false;
    }

    @Override
    public boolean add(E e) {
        return false;
    }

    @Override
    public boolean offer(E e) {
        return false;
    }

    @Override
    public E remove() {
        return null;
    }

    @Override
    public E poll() {
        return null;
    }

    @Override
    public E element() {
        return null;
    }

    @Override
    public E peek() {
        return null;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public void push(E e) {

    }

    @Override
    public E pop() {
        return null;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Iterator<E> iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }

    @Override
    public Iterator<E> descendingIterator() {
        return null;
    }

    private void operateDelegateIfNotAccessed(E e, BiConsumer<Deque<E>, E> operation) {
        if (!accessed.contains(e)) {
            operation.accept(delegate, e);
            accessed.add(e);
        }
    }

    private <R> R operateDelegateIfNotAccessed(E e, BiFunction<Deque<E>, E, R> operation, R defaultVal) {
        if (!accessed.contains(e)) {
            R r = operation.apply(delegate, e);
            accessed.add(e);
            return r;
        }
        return defaultVal;
    }
}
