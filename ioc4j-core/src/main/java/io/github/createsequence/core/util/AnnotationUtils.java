package io.github.createsequence.core.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Objects;

/**
 * Annotation utils.
 *
 * @author huangchengxing
 */
public class AnnotationUtils {

    public static <A extends Annotation> A getAnnotation(AnnotatedElement element, Class<A> annotationType) {
        return Objects.isNull(element) ? null : element.getAnnotation(annotationType);
    }

    public static <A extends Annotation> A getDeclaredAnnotation(AnnotatedElement element, Class<A> annotationType) {
        return Objects.isNull(element) ? null : element.getDeclaredAnnotation(annotationType);
    }
}
