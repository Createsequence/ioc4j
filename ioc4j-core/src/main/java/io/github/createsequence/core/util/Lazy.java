package io.github.createsequence.core.util;

import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

/**
 * A lazy initialization holder class.
 *
 * @author huangchengxing
 * @since 1.3.0
 */
@RequiredArgsConstructor
public class Lazy<T> implements Supplier<T> {

    private static final Object UNINITIALIZED_VALUE = new Object();

    private volatile Object value = UNINITIALIZED_VALUE;
    private final Supplier<T> supplier;

    /**
     * Initializes the value if it hasn't been already then returns it.
     *
     * @return the value
     */
    @SuppressWarnings("unchecked")
    @Override
    public T get() {
        if (value == UNINITIALIZED_VALUE) {
            synchronized (this) {
                if (value == UNINITIALIZED_VALUE) {
                    value = supplier.get();
                }
            }
        }
        return (T) value;
    }

    public synchronized void refresh() {
        this.value = UNINITIALIZED_VALUE;
    }

    /**
     * Returns `true` if a value for this Lazy instance has been already initialized, and `false` otherwise.
     */
    public Boolean isInitialized() {
        return value != UNINITIALIZED_VALUE;
    }
}
