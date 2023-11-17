package io.github.createsequence.core.support.annotation;

import io.github.createsequence.core.util.AnnotationUtils;
import io.github.createsequence.core.util.ArrayUtils;
import io.github.createsequence.core.util.Asserts;
import io.github.createsequence.core.util.CollectionUtils;
import io.github.createsequence.core.util.Streamable;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 组合注解，由复数{@link ResolvedAnnotation}按特定规则聚合而成
 *
 * @author huangchengxing
 * @see ResolvedAnnotation
 * @see #from
 * @see #of
 */
public abstract class ResolvedAnnotations implements Streamable<ResolvedAnnotation> {

    @Getter
    protected final ResolvedAnnotation root;
    @Getter
    protected final boolean resolveAttribute;
    protected final Map<Class<? extends Annotation>, ResolvedAnnotation> annotations = new LinkedHashMap<>();

    /**
     * 创建一个组合注解，该注解由指定的一批注解聚合而成
     *
     * @param root 根注解
     * @param metaAnnotations 待聚合的元注解
     * @return 组合注解
     */
    public static Combination of(@NonNull Annotation root, Annotation... metaAnnotations) {
        return of(root, true, metaAnnotations);
    }

    /**
     * 创建一个组合注解，该注解由指定的一批注解聚合而成
     *
     * @param root 根注解
     * @param resolveAttribute 是否需要进行属性解析
     * @param metaAnnotations 待聚合的元注解
     * @return 组合注解
     */
    public static Combination of(@NonNull Annotation root, boolean resolveAttribute, Annotation... metaAnnotations) {
        Combination ca = new Combination(root, resolveAttribute);
        if (ArrayUtils.isNotEmpty(metaAnnotations)) {
            Arrays.asList(metaAnnotations).forEach(ca::append);
        }
        return ca;
    }

    /**
     * 创建一个组合注解，该注解由根注解和其层级结构中的元注解聚合而成，默认忽略java自带的注解
     *
     * @param annotation 根注解
     * @return 组合注解
     */
    public static ResolvedAnnotations from(@NonNull Annotation annotation) {
        return from(annotation, true);
    }

    /**
     * 创建一个组合注解，该注解由根注解和其层级结构中的元注解聚合而成，默认忽略java自带的注解
     *
     * @param root 根注解
     * @param resolveAttribute 是否需要进行属性解析
     * @return 组合注解
     */
    public static ResolvedAnnotations from(
        @NonNull Annotation root, boolean resolveAttribute) {
        Predicate<? super Annotation> filter = a -> AnnotationUtils.isNotJdkMetaAnnotation(a.annotationType());
        return from(root, resolveAttribute, filter);
    }

    /**
     * 创建一个组合注解，该注解由根注解和其层级结构中的元注解聚合而成，该注解由根注解和其层级结构中的元注解聚合而成
     *
     * @param root 根注解
     * @param resolveAttribute 是否需要进行属性解析
     * @param filter 注解过滤器，过滤的注解及其元注解不会被收集
     * @return 组合注解
     */
    public static ResolvedAnnotations from(
        @NonNull Annotation root, boolean resolveAttribute, @NonNull Predicate<? super Annotation> filter) {
        return new Hierarchy(root, resolveAttribute, filter);
    }

    /**
     * 创建一个组合注解
     *
     * @param root 根注解
     * @param resolveAttribute 是否需要进行属性解析
     */
    ResolvedAnnotations(
        @NonNull Annotation root, boolean resolveAttribute) {
        Asserts.isNotNull(root, "root annotation must not null");
        this.root = ResolvedAnnotation.create(root, resolveAttribute);
        this.resolveAttribute = resolveAttribute;
    }

    /**
     * 获取合成注解
     *
     * @param annotationType 注解类型
     * @return 合成注解
     */
    public <A extends Annotation> Optional<A> synthesis(@NonNull Class<A> annotationType) {
        return Optional.ofNullable(annotations.get(annotationType))
            .map(ResolvedAnnotation::synthesis)
            .map(annotationType::cast);
    }

    /**
     * 获取合成注解
     *
     * @param annotationType 注解类型
     * @return 合成注解
     */
    public <A extends Annotation> Optional<A> get(@NonNull Class<A> annotationType) {
        return Optional.ofNullable(annotations.get(annotationType))
            .map(ResolvedAnnotation::getAnnotation)
            .map(annotationType::cast);
    }

    /**
     * 获取所有的元注解
     *
     * @return 元注解
     */
    public List<Annotation> getMetaAnnotations() {
        return annotations.values().stream()
            .filter(ra -> !ra.isRoot())
            .map(ResolvedAnnotation::getAnnotation)
            .toList();
    }

    /**
     * 获取迭代器
     *
     * @return 迭代器
     */
    @Override
    public Iterator<ResolvedAnnotation> iterator() {
        return annotations.values().iterator();
    }

    /**
     * 由根注解和其层级结构中的元注解聚合成的组合注解
     */
    public static class Hierarchy extends ResolvedAnnotations {

        /**
         * 创建一个组合注解
         *
         * @param root 根注解
         * @param resolveAttribute 是否需要进行属性解析
         * @param filter 注解过滤器，过滤的注解及其元注解不会被收集
         */
        Hierarchy(@NonNull Annotation root, boolean resolveAttribute, @NonNull Predicate<? super Annotation> filter) {
            super(root, resolveAttribute);
            Asserts.isNotNull(filter, "The annotations filter must not null!");
            init(filter);
        }

        protected void init(Predicate<? super Annotation> filter) {
            // 按广度优先收集该注解上的元注解
            Deque<ResolvedAnnotation> queue = CollectionUtils.newCollection(LinkedList::new, root);
            while (queue.isEmpty()) {
                ResolvedAnnotation source = queue.removeFirst();
                Class<? extends Annotation> sourceType = source.annotationType();
                if (annotations.containsKey(sourceType)) {
                    continue;
                }
                annotations.put(sourceType, source);
                // 收集元注解
                Stream.of(sourceType.getAnnotations())
                    .filter(annotation -> !annotations.containsKey(annotation.annotationType()))
                    .filter(filter)
                    .map(annotation -> ResolvedAnnotation.create(source, annotation, resolveAttribute))
                    .forEach(queue::addLast);
            }
        }
    }

    /**
     * 基于一批注解聚合成的组合注解
     */
    public static class Combination extends ResolvedAnnotations {

        /**
         * 创建一个组合注解
         *
         * @param root             根注解
         * @param resolveAttribute 是否需要进行属性解析
         */
        Combination(@NonNull Annotation root, boolean resolveAttribute) {
            super(root, resolveAttribute);
        }

        /**
         * 合并注解，该注解将追加为目前的最高层元注解
         *
         * @param annotation 待合并的注解
         */
        public Combination append(@NonNull Annotation annotation) {
            Asserts.isNotNull(annotation, "The annotation must not null!");
            Asserts.isFalse(
                annotations.containsKey(annotation.annotationType()),
                "The annotation [{}] already exists!", annotation.annotationType()
            );
            ResolvedAnnotation last = CollectionUtils.get(annotations.values(), annotations.size() - 1);
            ResolvedAnnotation resolvedAnnotation = ResolvedAnnotation.create(last, annotation, resolveAttribute);
            annotations.put(annotation.annotationType(), resolvedAnnotation);
            return this;
        }
    }
}
