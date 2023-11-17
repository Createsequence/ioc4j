package io.github.createsequence.core.support;

import io.github.createsequence.core.support.annotation.AliasFor;
import io.github.createsequence.core.support.annotation.HierarchicalElement;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * test for {@link HierarchicalElement}
 *
 * @author huangchengxing
 */
public class HierarchicalElementTest {

    @Test
    public void testCache() {
        HierarchicalElement he = HierarchicalElement.fromElement(Foo.class);
        Assert.assertSame(he, HierarchicalElement.fromElement(Foo.class));
        HierarchicalElement.clearCaches();
        Assert.assertNotSame(he, HierarchicalElement.fromElement(Foo.class));

        he = HierarchicalElement.fromResolvedElement(Foo.class);
        Assert.assertSame(he, HierarchicalElement.fromResolvedElement(Foo.class));
        HierarchicalElement.clearCaches();
        Assert.assertNotSame(he, HierarchicalElement.fromResolvedElement(Foo.class));
    }

    @SneakyThrows
    @Test
    public void testResolveHierarchy() {
        // 解析获取类的层级结构
        HierarchicalElement he = HierarchicalElement.fromElement(Foo.class);
        Assert.assertArrayEquals(
            new Object[]{ Foo.class, Super.class, Object.class, Interface.class },
            he.stream().map(HierarchicalElement::getSource).toArray()
        );

        // 解析方法的层级结构
        Method interfaceMethod = Interface.class.getDeclaredMethod("getNum");
        Method SuperMethod = Super.class.getDeclaredMethod("getNum");
        Method FooMethod = Foo.class.getDeclaredMethod("getNum");
        he = HierarchicalElement.fromElement(FooMethod);
        Assert.assertArrayEquals(
            new Object[]{ FooMethod, SuperMethod, interfaceMethod },
            he.stream().map(HierarchicalElement::getSource).toArray()
        );

        // 解析注解的层级结构
        he = HierarchicalElement.fromElement(ChildAnnotation.class);
        Assert.assertArrayEquals(
            new Object[]{ ChildAnnotation.class, ParentAnnotation.class },
            he.stream().map(HierarchicalElement::getSource).toArray()
        );
    }

    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface ParentAnnotation {
        String value() default "";
    }

    @ParentAnnotation
    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface ChildAnnotation {
        @AliasFor("name")
        String value() default "";
        @AliasFor("value")
        String name() default "";
    }

    private interface Interface {
        Number getNum();
    }

    private static abstract class Super implements Interface {
        @Override
        public abstract Integer getNum();
    }

    private static class Foo extends Super {
        @Override
        public Integer getNum() {
            return 1;
        }
    }
}
