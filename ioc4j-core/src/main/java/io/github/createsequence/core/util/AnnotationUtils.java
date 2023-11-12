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
import java.util.Objects;
import java.util.Optional;

/**
 * 注解工具类
 *
 * @author huangchengxing
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnnotationUtils {

    private static final String VALUE = "value";

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
