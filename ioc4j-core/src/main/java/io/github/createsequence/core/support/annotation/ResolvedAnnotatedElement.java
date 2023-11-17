package io.github.createsequence.core.support.annotation;

import io.github.createsequence.core.util.AnnotationUtils;
import io.github.createsequence.core.util.ArrayUtils;
import io.github.createsequence.core.util.Asserts;
import io.github.createsequence.core.util.Streamable;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>用于将直接存在于{@link AnnotatedElement}的注解转为{@link ResolvedAnnotations}。<br/>
 * 通过实例获得的注解默认都支持增强注解机制，
 * 并且当通过通过{@link #getAnnotations()}或{@link #getAnnotationsByType(Class)}方法获取注解时，
 * 将会查找以元注解形式间接存在于{@link AnnotatedElement}上的注解。<br/>
 * 为了与{@link HierarchicalAnnotatedElement}进行区分，注解<b>不支持获取父类或父接口上带有{@link java.lang.annotation.Inherited}注解的可继承注解</b>
 *
 * @author huangchengxing
 * @see ResolvedAnnotations
 */
@Getter
public class ResolvedAnnotatedElement<E extends AnnotatedElement>
    implements Streamable<ResolvedAnnotations>, WrappedAnnotatedElement<E> {

    private final E source;
    private final List<ResolvedAnnotations> resolvedAnnotations;

    /**
     * 构建一个{@link ResolvedAnnotatedElement}实例
     *
     * @param element 要包装的元素
     * @return {@link ResolvedAnnotatedElement}实例
     */
    @SuppressWarnings("unchecked")
    public static <E extends AnnotatedElement> ResolvedAnnotatedElement<E> of(E element) {
        return element instanceof ResolvedAnnotatedElement<?> rae ?
            (ResolvedAnnotatedElement<E>)rae : new ResolvedAnnotatedElement<>(element);
    }

    private ResolvedAnnotatedElement(E source) {
        Asserts.isFalse(source instanceof ResolvedAnnotatedElement<?>, "source is already wrapped: {}", source);
        this.source = source;
        this.resolvedAnnotations = Arrays.stream(source.getDeclaredAnnotations())
            .map(ResolvedAnnotations::from)
            .toList();
    }

    /**
     * 获取用于迭代所有注解的迭代器
     *
     * @return 迭代器
     */
    @Override
    public Iterator<ResolvedAnnotations> iterator() {
        return resolvedAnnotations.iterator();
    }

    /**
     * 获取直接或间接存在于此元素上的指定注解
     *
     * @param annotationType 注解类型
     * @return 注解
     */
    @Nullable
    @Override
    public <T extends Annotation> T getAnnotation(@NonNull Class<T> annotationType) {
        return resolvedAnnotations.stream()
            .map(ras -> ras.synthesis(annotationType))
            .mapMulti(Optional::ifPresent)
            .findFirst()
            .map(annotationType::cast)
            .orElse(null);
    }

    /**
     * 获取直接或间接存在于此元素上的所有注解，若注解类型是可重复注解，则一并其容器注解中的注解
     *
     * @param annotationType 注解类型
     * @return 注解
     */
    @SuppressWarnings("all")
    @Override
    public <T extends Annotation> T[] getAnnotationsByType(@NonNull Class<T> annotationType) {
        List<T> annotations = resolvedAnnotations.stream()
            .map(ras -> ras.synthesis(annotationType))
            .<T>mapMulti(Optional::ifPresent)
            .collect(Collectors.toList());
        // 若注解类型是可重复注解，则还需要收集其容器注解中的注解
        AnnotationUtils.determineRepeatableContainerType(annotationType).ifPresent(containerType ->
            resolvedAnnotations.stream()
                .map(ras -> ras.synthesis(containerType))
                .mapMulti(Optional::ifPresent)
                .map(container -> AnnotationUtils.getAnnotationFromRepeatableContainer(annotationType, (Annotation) container))
                .flatMap(Arrays::stream)
                .forEach(annotations::add)
        );
        return annotations.toArray(len -> ArrayUtils.newInstance(annotationType, len));
    }

    /**
     * 获取直接或间接存在于此元素上的所有注解
     *
     * @return 注解
     */
    @SuppressWarnings("all")
    @Override
    public Annotation[] getAnnotations() {
        return resolvedAnnotations.stream()
            .flatMap(ResolvedAnnotations::stream)
            .map(ResolvedAnnotation::synthesis)
            .toArray(Annotation[]::new);
    }

    /**
     * 获取直接存在于此元素上的指定注解
     *
     * @return 注解
     */
    @Nullable
    @Override
    public <T extends Annotation> T getDeclaredAnnotation(@NonNull Class<T> annotationType) {
        return getAnnotation(annotationType);
    }

    /**
     * 获取直接存在于此元素上的指定注解，若注解类型是可重复注解，则一并其容器注解中的注解
     *
     * @param annotationType 注解类型
     * @return 注解
     */
    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(@NonNull Class<T> annotationType) {
        return getAnnotationsByType(annotationType);
    }

    /**
     * 获取直接存在于此元素上的所有注解
     *
     * @param
     * @return 注解
     */
    @SuppressWarnings("all")
    @Override
    public Annotation[] getDeclaredAnnotations() {
        return getAnnotations();
    }
}
