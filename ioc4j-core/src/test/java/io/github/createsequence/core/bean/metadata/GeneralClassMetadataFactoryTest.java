package io.github.createsequence.core.bean.metadata;

import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * test for {@link GeneralClassMetadataFactory}
 *
 * @author huangchengxing
 */
public class GeneralClassMetadataFactoryTest {

    @Test
    public void test() {
        ClassMetadataFactory factory = new GeneralClassMetadataFactory();
        ClassMetadata metadata = factory.resolve(Foo.class);
        Assert.assertNotNull(metadata);
        metadata.getDeclaredMethods().get(1).getParents();
    }

    @Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAnnotation {
        String value() default "";
    }

    @TestAnnotation("FooInterface.class")
    private interface FooInterface {

        @TestAnnotation("FooInterface.class#method2")
        Collection<?> method2(Collection<Integer> ids);
    }

    private static class FooSupper implements FooInterface {

        @TestAnnotation("FooSupper.class#method2")
        @Override
        public Collection<?> method2(Collection<Integer> ids) {
            return null;
        }
    }


    @TestAnnotation("Foo.class")
    private static class Foo extends FooSupper {

        @TestAnnotation("Foo.class#field1")
        private String field1;
        private String field2;

        @TestAnnotation("Foo.class#method1")
        public void method1() {}

        @TestAnnotation("Foo.class#method2")
        @Override
        public List<?> method2(Collection<Integer> ids) {
            return Collections.emptyList();
        }
    }
}
