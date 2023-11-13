package io.github.createsequence.core.support.annotation;

import io.github.createsequence.core.util.Asserts;
import io.github.createsequence.core.util.ClassUtils;
import io.github.createsequence.core.util.CollectionUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *
 *
 * @author huangchengxing
 */
public class ComposableAnnotations implements Iterable<AttributeResolvableAnnotation> {

    private final AttributeResolvableAnnotation root;
    private final boolean resolveAttribute;
    private final Predicate<? super Annotation> filter;
    private final Map<Class<? extends Annotation>, AttributeResolvableAnnotation> annotations = new LinkedHashMap<>();

    /**
     * 创建一个组合注解，默认忽略java自带的注解
     *
     * @param annotation 根注解
     * @return 组合注解
     */
    public static ComposableAnnotations from(@NonNull Annotation annotation) {
        return from(annotation, true);
    }

    /**
     * 创建一个组合注解，默认忽略java自带的注解
     *
     * @param root 根注解
     * @param resolveAttribute 是否需要进行属性解析
     * @return 组合注解
     */
    public static ComposableAnnotations from(
        @NonNull Annotation root, boolean resolveAttribute) {
        Predicate<? super Annotation> filter = a -> !ClassUtils.isJdkClass(a.annotationType());
        return from(root, resolveAttribute, filter);
    }

    /**
     * 创建一个组合注解
     *
     * @param root 根注解
     * @param resolveAttribute 是否需要进行属性解析
     * @param filter 过滤器
     * @return 组合注解
     */
    public static ComposableAnnotations from(
        @NonNull Annotation root, boolean resolveAttribute, @NonNull Predicate<? super Annotation> filter) {
        return new ComposableAnnotations(root, resolveAttribute, filter);
    }

    /**
     * 创建一个组合注解
     *
     * @param root 根注解
     * @param resolveAttribute 是否需要进行属性解析
     * @param filter 过滤器
     */
    ComposableAnnotations(
        @NonNull Annotation root, boolean resolveAttribute, @NonNull Predicate<? super Annotation> filter) {
        Asserts.isNotNull(root, "root annotation must not null");
        Asserts.isNotNull(filter, "annotation filter must not null");
        this.root = AttributeResolvableAnnotation.create(root, resolveAttribute);
        this.resolveAttribute = resolveAttribute;
        this.filter = filter;
        init();
    }

    public void init() {
        // 按广度优先收集该注解上的元注解
        Deque<AttributeResolvableAnnotation> queue = CollectionUtils.newCollection(LinkedList::new, root);
        while (queue.isEmpty()) {
            AttributeResolvableAnnotation source = queue.removeFirst();
            Class<? extends Annotation> sourceType = source.annotationType();
            if (annotations.containsKey(sourceType)) {
                continue;
            }
            annotations.put(sourceType, source);
            // 收集元注解
            Stream.of(sourceType.getAnnotations())
                .filter(annotation -> !annotations.containsKey(annotation.annotationType()))
                .filter(filter)
                .map(annotation -> AttributeResolvableAnnotation.create(source, annotation, resolveAttribute))
                .forEach(queue::addLast);
        }
    }

    /**
     * 获取合成注解
     *
     * @param annotationType 注解类型
     * @return 合成注解
     */
    @Nullable
    public <A extends Annotation> A synthesis(@NonNull Class<A> annotationType) {
        return Optional.ofNullable(annotations.get(annotationType))
            .map(AttributeResolvableAnnotation::synthesis)
            .map(annotationType::cast)
            .orElse(null);
    }

    /**
     * 获取合成注解
     *
     * @param annotationType 注解类型
     * @return 合成注解
     */
    @Nullable
    public <A extends Annotation> A get(@NonNull Class<A> annotationType) {
        return Optional.ofNullable(annotations.get(annotationType))
            .map(AttributeResolvableAnnotation::getAnnotation)
            .map(annotationType::cast)
            .orElse(null);
    }

    /**
     * 获取迭代器
     *
     * @return 迭代器
     */
    @Override
    public Iterator<AttributeResolvableAnnotation> iterator() {
        return annotations.values().iterator();
    }

    /**
     * 获取包括当前元素在内，层级结构中的所有元素构成的流
     *
     * @return 流
     */
    public Stream<AttributeResolvableAnnotation> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
}
