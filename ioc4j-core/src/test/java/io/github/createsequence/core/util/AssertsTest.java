package io.github.createsequence.core.util;

import io.github.createsequence.core.exception.Ioc4jException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

/**
 * test for {@link Asserts}
 *
 * @author huangchengxing
 */
public class AssertsTest {

    @Test
    public void isNotEquals() {
        Object obj = new Object();
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isNotEquals(obj, obj, () -> new Ioc4jException("test")));
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isNotEquals(obj, obj, "test"));
    }

    @Test
    public void isEquals() {
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isEquals(new Object(), new Object(), () -> new Ioc4jException("test")));
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isEquals(new Object(), new Object(), "test"));
    }

    @Test
    public void isTrue() {
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isTrue(false, () -> new Ioc4jException("test")));
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isTrue(false, "test"));
    }

    @Test
    public void isFalse() {
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isFalse(true, () -> new Ioc4jException("test")));
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isFalse(true, "test"));
    }

    @Test
    public void isNull() {
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isNull(new Object(), () -> new Ioc4jException("test")));
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isNull(new Object(), "test"));
    }

    @Test
    public void notNull() {
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isNotNull(null, () -> new Ioc4jException("test")));
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isNotNull(null, "test"));
    }

    @Test
    public void isEmpty() {
        // object
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isEmpty(new Object(), () -> new Ioc4jException("test")));
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isEmpty(new Object(), "test"));
        // array
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isEmpty(new Object[1], () -> new Ioc4jException("test")));
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isEmpty(new Object[1], "test"));
        // collection
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isEmpty(Collections.singletonList(1), () -> new Ioc4jException("test")));
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isEmpty(Collections.singletonList(1), "test"));
        // map
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isEmpty(Collections.singletonMap(1, 1), () -> new Ioc4jException("test")));
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isEmpty(Collections.singletonMap(1, 1), "test"));
        // string
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isEmpty("test", () -> new Ioc4jException("test")));
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isEmpty("test", "test"));
    }

    @Test
    public void isNotEmpty() {
        // object
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isNotEmpty(null, () -> new Ioc4jException("test")));
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isNotEmpty(null, "test"));
        // array
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isNotEmpty(new Object[0], () -> new Ioc4jException("test")));
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isNotEmpty(new Object[0], "test"));
        // collection
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isNotEmpty(CollectionUtils.newCollection(ArrayList::new), () -> new Ioc4jException("test")));
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isNotEmpty(CollectionUtils.newCollection(ArrayList::new), "test"));
        // map
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isNotEmpty(Collections.emptyMap(), () -> new Ioc4jException("test")));
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isNotEmpty(Collections.emptyMap(), "test"));
        // string
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isNotEmpty("", () -> new Ioc4jException("test")));
        Assert.assertThrows(Ioc4jException.class, () -> Asserts.isNotEmpty("", "test"));
    }
}
