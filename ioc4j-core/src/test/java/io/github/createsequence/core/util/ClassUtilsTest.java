package io.github.createsequence.core.util;

import io.github.createsequence.core.exception.Ioc4jException;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Assert;
import org.junit.Test;

/**
 * test for {@link ClassUtils}
 *
 * @author huangchengxing
 */
public class ClassUtilsTest {

    @Test
    public void testIsObjectOrVoid() {
        Assert.assertFalse(ClassUtils.isObjectOrVoid(String.class));
        Assert.assertTrue(ClassUtils.isObjectOrVoid(Object.class));
        Assert.assertTrue(ClassUtils.isObjectOrVoid(Void.TYPE));
        Assert.assertTrue(ClassUtils.isObjectOrVoid(void.class));
    }

    @Test
    public void isJdkClass() {
        Assert.assertTrue(ClassUtils.isJdkClass(String.class));
        Assert.assertFalse(ClassUtils.isJdkClass(Nullable.class));
        Assert.assertFalse(ClassUtils.isJdkClass(ClassUtilsTest.class));
        Assert.assertThrows(NullPointerException.class, () -> ClassUtils.isJdkClass(null));
    }

    @Test
    public void forName() {
        Assert.assertEquals(String.class, ClassUtils.forName("java.lang.String"));
        Assert.assertEquals(ClassUtilsTest.class, ClassUtils.forName("io.github.createsequence.core.util.ClassUtilsTest"));
        Assert.assertThrows(Ioc4jException.class, () -> ClassUtils.forName("not.found.class"));
        Assert.assertThrows(NullPointerException.class, () -> ClassUtils.forName(null));

        Assert.assertEquals(ClassUtilsTest.class, ClassUtils.forName("", ClassUtilsTest.class));
        Assert.assertEquals(String.class, ClassUtils.forName("java.lang.String", ClassUtilsTest.class));
        Assert.assertThrows(Ioc4jException.class, () -> ClassUtils.forName("not.found.class", ClassUtilsTest.class));
    }

    @Test
    public void newInstance() {
        Object object = ClassUtils.newInstance(String.class);
        Assert.assertNotNull(object);
        // if class has no default constructor, it will throw exception
        Assert.assertThrows(Ioc4jException.class, () -> ClassUtils.newInstance(Foo.class));
    }

    @Test
    public void packageToPath() {
        Assert.assertEquals("cn/crane4j/core/util/ClassUtils", ClassUtils.packageToPath("cn.crane4j.core.util.ClassUtils"));
        Assert.assertThrows(NullPointerException.class, () -> ClassUtils.packageToPath(null));
    }

    @SuppressWarnings("unused")
    @RequiredArgsConstructor
    private static class Foo {
        private final String name;
    }
}
