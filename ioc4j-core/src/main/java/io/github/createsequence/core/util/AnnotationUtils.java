package io.github.createsequence.core.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 注解工具类
 *
 * @author huangchengxing
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnnotationUtils {

    private static final String VALUE = "value";

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
        Annotation container = Optional.ofNullable(annotationType)
            .map(t -> t.getDeclaredAnnotation(Repeatable.class))
            .map(Repeatable::value)
            .map(element::getDeclaredAnnotation)
            .orElse(null);
        if (Objects.isNull(container)) {
            return ArrayUtils.newInstance(annotationType, 0);
        }
        return getAnnotationFromRepeatableContainer(annotationType, container);
    }

    @SuppressWarnings("unchecked")
    private static <A> A[] getAnnotationFromRepeatableContainer(Class<A> annotationType, Annotation container) throws Throwable {
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
