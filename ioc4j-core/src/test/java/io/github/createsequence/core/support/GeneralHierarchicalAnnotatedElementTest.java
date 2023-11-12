package io.github.createsequence.core.support;

import io.github.createsequence.core.util.ClassUtils;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * test for {@link GeneralHierarchicalAnnotatedElement}
 *
 * @author huangchengxing
 */
public class GeneralHierarchicalAnnotatedElementTest {

    @Test
    public void test() {
        GeneralHierarchicalAnnotatedElement<Class<?>> gh = new GeneralHierarchicalAnnotatedElement<>(
            Annotation4.class, t -> Stream.of(t.getAnnotations()).map(Annotation::annotationType).collect(Collectors.toList())
        );
        List<Annotation> annotations = gh.stream()
            .map(HierarchicalAnnotatedElement::getDeclaredAnnotations)
            .flatMap(Stream::of)
            .filter(a -> !ClassUtils.isJdkClass(a.annotationType()))
            .toList();
        System.out.println(annotations);
    }

    @Annotation2("anno1")
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface Annotation1 {
        String value() default "";
    }
    @Annotation1("anno2")
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface Annotation2 {
        String value() default "";
    }
    @Annotation1("anno3")
    @Annotation2("anno3")
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface Annotation3 {
        String value() default "";
    }
    @Annotation1("anno4")
    @Annotation2("anno4")
    @Annotation3("anno4")
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface Annotation4 {}
}
