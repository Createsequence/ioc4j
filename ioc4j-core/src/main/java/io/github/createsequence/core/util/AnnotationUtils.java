package io.github.createsequence.core.util;

import io.github.createsequence.core.support.annotation.ResolvedAnnotation;
import io.github.createsequence.core.support.annotation.ResolvedAnnotations;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 注解工具类
 *
 * @author huangchengxing
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnnotationUtils {

    private static final Set<Class<? extends Annotation>> JDK_META_ANNOTATIONS = Set.of(
        Target.class, Retention.class, Inherited.class, Documented.class,
        SuppressWarnings.class, Override.class, Deprecated.class
    );
    private static final String VALUE = "value";

    /**
     * 是否是JDK原生元注解
     *
     * @param annotationType 注解类型
     * @return 是否
     * @see #JDK_META_ANNOTATIONS
     */
    public static boolean isJdkMetaAnnotation(Class<? extends Annotation> annotationType) {
        return JDK_META_ANNOTATIONS.contains(annotationType);
    }

    /**
     * 是否不是JDK原生元注解
     *
     * @param annotationType 注解类型
     * @return 是否
     * @see #JDK_META_ANNOTATIONS
     */
    public static boolean isNotJdkMetaAnnotation(Class<? extends Annotation> annotationType) {
        return !isJdkMetaAnnotation(annotationType);
    }

    /**
     * 当前注解是否包含指定类型的元注解
     *
     * @param annotation 注解
     * @param metaAnnotationType 元注解
     * @return 是否
     */
    public static boolean hasMetaAnnotation(Annotation annotation, Class<? extends Annotation> metaAnnotationType) {
        return ResolvedAnnotations.from(annotation, false)
            .get(metaAnnotationType)
            .isPresent();
    }

    /**
     * 将一批注解合成为指定类型的注解
     *
     * @param annotation 注解对象
     * @param annotationType 注解类型
     * @param metaAnnotations 待合成的元注解，每种类型的注解最多只能有一个
     * @return 合成的注解，若输入的注解中没有指定类型的注解，则返回{@code null}
     */
    @Nullable
    public static <A extends Annotation> A getResolvedAnnotation(@NonNull Annotation annotation, @NonNull Class<A> annotationType, Annotation... metaAnnotations) {
        return ResolvedAnnotations.of(annotation, metaAnnotations)
            .synthesis(annotationType)
            .orElse(null);
    }

    /**
     * 将{@code annotation}及其层级结构中的元注解合成为指定类型的注解
     *
     * @param annotation 注解对象
     * @param annotationType 注解类型
     * @return 合成的注解，若层级结构中没有指定类型的注解，则返回{@code null}
     */
    @Nullable
    public static <A extends Annotation> A getResolvedAnnotation(@NonNull Annotation annotation, @NonNull Class<A> annotationType) {
        return ResolvedAnnotations.from(annotation)
            .synthesis(annotationType)
            .orElse(null);
    }

    /**
     * 获取合成注解，合成后的注解支持{@link io.github.createsequence.core.support.annotation.AliasFor}注解
     *
     * @param annotation 注解
     * @return 合成的注解
     */
    public static <A extends Annotation> A getResolvedAnnotation(@NonNull Annotation annotation) {
        return ResolvedAnnotation.create(annotation, true).synthesis();
    }

    /**
     * 将{@code annotation}及其层级结构中的元注解合成为指定类型的注解
     *
     * @param annotation 注解对象
     * @param annotationType 注解类型
     * @return 合成的注解，若层级结构中没有指定类型的注解，则返回{@code null}
     */
    @Nullable
    public static <A extends Annotation> List<A> getRepeatableResolvedAnnotations(Annotation annotation, Class<A> annotationType) {
        List<A> results = new ArrayList<>();
        ResolvedAnnotations ras = ResolvedAnnotations.from(annotation);
        ras.synthesis(annotationType).ifPresent(results::add);
        determineRepeatableContainerType(annotationType)
            .flatMap(ras::synthesis)
            .map(container -> getAnnotationFromRepeatableContainer(annotationType, container))
            .map(Arrays::asList)
            .ifPresent(results::addAll);
        return results;
    }

    /**
     * 基于属性值构建一个注解对象
     *
     * @param annotationType 注解类型
     * @param attributeValues 属性值
     * @return 注解对象
     * @see SynthesizedAnnotationInvocationHandler
     */
    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A synthesis(
        Class<A> annotationType, Map<String, Object> attributeValues) {
        return (A)Proxy.newProxyInstance(
            annotationType.getClassLoader(), new Class[]{ annotationType, Annotation.class},
            new SynthesizedAnnotationInvocationHandler(annotationType, attributeValues)
        );
    }

    /**
     * 构建一个仅包括{@code value}属性的注解
     *
     * @param annotationType 注解类型
     * @param value 属性值
     * @return 注解对象
     * @see SynthesizedAnnotationInvocationHandler
     */
    public static <A extends Annotation> A synthesis(Class<A> annotationType, Object value) {
        Map<String, Object> attributes = Collections.singletonMap(VALUE, value);
        return synthesis(annotationType, attributes);
    }

    /**
     * 将一个注解转为属性值集合
     *
     * @param annotation 注解对象
     * @return 属性值集合
     */
    public static Map<String, Object> getAttributeValues(Annotation annotation) {
        return Stream.of(getAnnotationAttributes(annotation.annotationType()))
            .collect(Collectors.toMap(
                Method::getName, m -> ReflectUtils.invokeRaw(annotation, m)
            ));
    }

    /**
     * 获取注解属性方法
     *
     * @param annotationType 注解类型
     * @return 属性方法
     */
    public static Method[] getAnnotationAttributes(Class<? extends Annotation> annotationType) {
        return Stream.of(annotationType.getDeclaredMethods())
            .filter(m -> !ClassUtils.isObjectOrVoid(m.getReturnType()))
            .filter(m -> m.getParameterCount() == 0)
            .filter(m -> !m.isSynthetic())
            .toArray(len -> ArrayUtils.newInstance(Method.class, len));
    }

    /**
     * 从当前元素上查找指定类型的注解，若注解是可重复注解，则一并获取间接存在的注解
     *
     * @param element 注解元素
     * @param annotationType 注解类型
     * @return 注解对象
     */
    public static <A extends Annotation> List<A> getDeclaredRepeatableAnnotations(
        AnnotatedElement element, Class<A> annotationType) {
        List<A> results = new ArrayList<>();
        Optional.ofNullable(element)
            .map(e -> e.getDeclaredAnnotation(annotationType))
            .ifPresent(results::add);
        Optional.ofNullable(element)
            .map(e -> getRepeatableAnnotationFromContainer(e, annotationType))
            .filter(ArrayUtils::isNotEmpty)
            .ifPresent(annotations -> Collections.addAll(results, annotations));
        return results;
    }

    /**
     * 若获取可重复注解
     *
     * @param element 注解元素
     * @param annotationType 注解类型
     * @return 可重复注解
     */
    @SneakyThrows
    private static <A> A[] getRepeatableAnnotationFromContainer(AnnotatedElement element, Class<A> annotationType) {
        Annotation container = determineRepeatableContainerType(annotationType)
            .map(element::getDeclaredAnnotation)
            .orElse(null);
        if (Objects.isNull(container)) {
            return ArrayUtils.newInstance(annotationType, 0);
        }
        return getAnnotationFromRepeatableContainer(annotationType, container);
    }

    /**
     * 获取可重复注解的容器类型
     *
     * @param annotationType 注解类型
     * @return 容器类型
     */
    public static <A> Optional<Class<? extends Annotation>> determineRepeatableContainerType(Class<A> annotationType) {
        return Optional.ofNullable(annotationType)
            .map(t -> t.getDeclaredAnnotation(Repeatable.class))
            .map(Repeatable::value);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static <A> A[] getAnnotationFromRepeatableContainer(Class<A> annotationType, Annotation container) {
        if (SynthesizedAnnotationInvocationHandler.isSynthesized(container)) {
            return (A[])((SynthesizedAnnotationInvocationHandler)Proxy.getInvocationHandler(container)).getMemberValues().get(VALUE);
        }
        Method valueMethod = ReflectUtils.getMethod(container.annotationType(), VALUE);
        Asserts.isNotNull(
            valueMethod, "The repeatable container annotation [{}] of [{}] must have a 'value' method!",
            annotationType, container.annotationType()
        );
        if (Proxy.isProxyClass(container.annotationType())) {
            return (A[]) Proxy.getInvocationHandler(container).invoke(container, valueMethod, null);
        }
        return ReflectUtils.invokeRaw(container, valueMethod);
    }
}
