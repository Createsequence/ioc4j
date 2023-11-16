package io.github.createsequence.core.support.annotation;

import io.github.createsequence.core.util.AnnotationUtils;
import io.github.createsequence.core.util.ArrayUtils;
import io.github.createsequence.core.util.Streamable;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * <p>表示一个具备层级结构的{@link AnnotatedElement}，比如类或方法。<br/>
 * 在{@link AnnotatedElement}的基础上扩展了一些关于层级结构的API，比如：
 * 通过{@link #getParents()}可以获得该元素的直接上级元素，
 * 而通过{@link #stream()}则可以获得包含当前元素在内的层级结构中的所有元素。
 *
 * <p>不同于{@link AnnotatedElement}，当调用{@link #getAnnotation}或{@link #getAnnotations()}方法时，
 * 搜索的范围为层级结构中的所有元素，而不仅是父类或父接口中带有{@link java.lang.annotation.Inherited}元注解的注解。
 *
 * @author huangchengxing
 * @param <E> 元素类型
 * @param <I> 实现类类型
 * @see GeneralHierarchicalAnnotatedElement
 */
public interface HierarchicalAnnotatedElement<E extends AnnotatedElement, I extends HierarchicalAnnotatedElement<E, I>> extends AnnotatedElement, Streamable<I> {

    /**
     * 获取元素
     *
     * @return 元数据的来源
     */
    @NonNull
    E getSource();
    
    /**
     * 从层级结构中的所有的元素中查找首个匹配的注解
     *
     * @param annotationType 注解类型
     * @param <T> 注解类型
     * @return 注解
     */
    @Override
    @Nullable
    default <T extends Annotation> T getAnnotation(@NonNull Class<T> annotationType) {
        return stream()
            .map(parent -> parent.getDeclaredAnnotation(annotationType))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取层级结构中的所有的元素上的全部注解
     *
     * @return 注解列表
     */
    @Override
    default Annotation[] getAnnotations() {
        return stream()
            .map(HierarchicalAnnotatedElement::getDeclaredAnnotations)
            .flatMap(Stream::of)
            .toArray(Annotation[]::new);
    }

    /**
     * 从层级结构中的所有的元素上获取所有指定类型的注解，包括被{@link Repeatable}标记的可重复注解
     *
     * @param annotationType 注解类型
     * @return 注解
     */
    @Override
    default <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
        return stream()
            .map(parent -> parent.getDeclaredAnnotationsByType(annotationType))
            .flatMap(Stream::of)
            .toArray(len -> ArrayUtils.newInstance(annotationType, len));
    }

    /**
     * 从当前元素上查找首个匹配的注解
     *
     * @param annotationType 注解类型
     * @param <A> 注解类型
     * @return 注解
     */
    @Override
    @Nullable
    default <A extends Annotation> A getDeclaredAnnotation(@NonNull Class<A> annotationType) {
        return getSource().getDeclaredAnnotation(annotationType);
    }

    /**
     * 从当前元素上获取所有注解
     *
     * @return 注解对象
     */
    @Override
    default Annotation[] getDeclaredAnnotations() {
        return getSource().getDeclaredAnnotations();
    }

    /**
     * 从当前元素上获取指定所有类型的注解，包括被{@link Repeatable}标记的可重复注解
     *
     * @param annotationType 注解类型
     * @return 注解对象
     */
    @Override
    default <A extends Annotation> A[] getDeclaredAnnotationsByType(Class<A> annotationType) {
        List<A> annotations = AnnotationUtils.getDeclaredRepeatableAnnotations(getSource(), annotationType);
        return ArrayUtils.toArray(annotations, annotationType);
    }

    /**
     * 获取当前元素的直接上级元素
     *
     * @return 父级元数据，返回的列表不可变
     */
    @NonNull
    default Collection<I> getParents() {
        return Collections.emptyList();
    }

    /**
     * 获取迭代器，该迭代器可用于按广度优先迭代包括当前元素在内，层级结构中的所有元素
     *
     * @return 迭代器实例
     * @see HierarchicalAnnotatedElementIterator
     */
    @SuppressWarnings("unchecked")
    @Override
    default Iterator<I> iterator() {
        return new HierarchicalAnnotatedElementIterator<>((I)this);
    }

    /**
     * 内部迭代器，用于按广度优先迭代包括当前元素在内，层级结构中的所有元素
     *
     * @author huangchengxing
     */
    @RequiredArgsConstructor
    class HierarchicalAnnotatedElementIterator<S extends AnnotatedElement, M extends HierarchicalAnnotatedElement<S, M>> implements Iterator<M> {
        private final Set<S> accessed = new HashSet<>();
        private final Deque<M> queue = new LinkedList<>();
        public HierarchicalAnnotatedElementIterator(M root) {
            queue.add(root);
        }
        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }
        @Override
        public M next() {
            if (queue.isEmpty()) {
                throw new NoSuchElementException();
            }
            M m = queue.removeFirst();
            accessed.add(m.getSource());
            for (M p : m.getParents()) {
                if (!accessed.contains(p.getSource())) {
                    queue.add(p);
                }
            }
            return m;
        }
    }

}
