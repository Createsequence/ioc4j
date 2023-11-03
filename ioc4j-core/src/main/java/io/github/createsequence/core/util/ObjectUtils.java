package io.github.createsequence.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Common object utils.
 *
 * @author huangchengxing
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ObjectUtils {

    /**
     * <p>Get type of element from the target.<br />
     * Support {@link List}, {@link Collection}, {@link Map}, {@link Iterator}, {@link Iterable}, Array.
     *
     * @param target target
     * @return element type
     */
    @Nullable
    public static Class<?> getElementType(Object target) {
        if (Objects.isNull(target)) {
            return null;
        }
        Object firstNonNull = target;
        if (target instanceof Iterator) {
            firstNonNull = CollectionUtils.getFirstNotNull((Iterator<?>)target);
        }
        else if (target instanceof Iterable) {
            firstNonNull = CollectionUtils.getFirstNotNull((Iterable<?>)target);
        }
        else if (target.getClass().isArray()) {
            firstNonNull = ArrayUtils.getFirstNotNull((Object[])target);
        }
        return Objects.isNull(firstNonNull) ? null : firstNonNull.getClass();
    }

    /**
     * <p>Get target or default value if target is null.
     *
     * @param target target
     * @param defaultValue default value
     * @param <T> element type
     * @return element
     */
    public static <T> T defaultIfNull(T target, T defaultValue) {
        return Objects.isNull(target) ? defaultValue : target;
    }

    /**
     * <p>Get a specified index element from the target.<br />
     * Support {@link List}, {@link Collection}, {@link Map}, {@link Iterator}, {@link Iterable}, {@link Object[]}.
     *
     * @param target target
     * @param <T> element type
     * @return element
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T get(Object target, int index) {
        if (Objects.isNull(target)) {
            return null;
        }
        if (target instanceof List) {
            return CollectionUtils.get((List<T>)target, index);
        }
        if (target instanceof Iterator) {
            Iterator<T> iterator = (Iterator<T>)target;
            return CollectionUtils.get(iterator, index);
        }
        if (target instanceof Iterable) {
            return CollectionUtils.get((Iterable<T>)target, index);
        }
        if (target instanceof Map) {
            return get(((Map<?, T>)target).values(), index);
        }
        if (target.getClass().isArray()) {
            // if index is out of bounds, return null
            T[] array = (T[])target;
            return ArrayUtils.get(array, index);
        }
        return null;
    }

    /**
     * <p>Determine whether the target is empty.<br />
     * Support {@link Map}, {@link Collection}, {@link Iterator}, {@link Iterable}, Array, {@link CharSequence}
     *
     * @param target target
     * @return boolean
     */
    public static boolean isEmpty(Object target) {
        if (Objects.isNull(target)) {
            return true;
        }
        if (target instanceof Map) {
            return CollectionUtils.isEmpty((Map<?, ?>)target);
        }
        if (target instanceof Iterable) {
            return CollectionUtils.isEmpty((Iterable<?>)target);
        }
        if (target instanceof Iterator) {
            return CollectionUtils.isEmpty((Iterator<?>)target);
        }
        if (target.getClass().isArray()) {
            return ArrayUtils.isEmpty((Object[])target);
        }
        if (target instanceof CharSequence cs) {
            return StringUtils.isEmpty(cs);
        }
        return false;
    }

    /**
     * Determine whether the target is not empty.
     *
     * @param target target
     * @return boolean
     */
    public static boolean isNotEmpty(Object target) {
        return !isEmpty(target);
    }
}
