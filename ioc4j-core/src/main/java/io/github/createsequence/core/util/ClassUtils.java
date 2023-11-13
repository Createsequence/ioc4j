package io.github.createsequence.core.util;

import io.github.createsequence.core.exception.Ioc4jException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>{@link Class} utils.
 *
 * @author huangchengxing
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClassUtils {

    private static Map<Class<?>, Class<?>> PRIMITIVE_TYPE_TO_BOXED_TYPE;

    static {
        PRIMITIVE_TYPE_TO_BOXED_TYPE = new HashMap<>(8);
        PRIMITIVE_TYPE_TO_BOXED_TYPE.put(boolean.class, Boolean.class);
        PRIMITIVE_TYPE_TO_BOXED_TYPE.put(byte.class, Byte.class);
        PRIMITIVE_TYPE_TO_BOXED_TYPE.put(char.class, Character.class);
        PRIMITIVE_TYPE_TO_BOXED_TYPE.put(double.class, Double.class);
        PRIMITIVE_TYPE_TO_BOXED_TYPE.put(float.class, Float.class);
        PRIMITIVE_TYPE_TO_BOXED_TYPE.put(int.class, Integer.class);
        PRIMITIVE_TYPE_TO_BOXED_TYPE.put(long.class, Long.class);
        PRIMITIVE_TYPE_TO_BOXED_TYPE.put(short.class, Short.class);
        PRIMITIVE_TYPE_TO_BOXED_TYPE = Map.copyOf(PRIMITIVE_TYPE_TO_BOXED_TYPE);
    }

    /**
     * 检查{@code target}是否可以从{@code sourceType}转换得到
     *
     * @param targetType 目标类型，可以是源类型的父类
     * @param sourceType 源类型
     * @return 是否
     */
    public static boolean isAssignable(Class<?> targetType, Class<?> sourceType) {
        targetType = targetType.isPrimitive() ? PRIMITIVE_TYPE_TO_BOXED_TYPE.get(targetType) : targetType;
        sourceType = sourceType.isPrimitive() ? PRIMITIVE_TYPE_TO_BOXED_TYPE.get(sourceType) : sourceType;
        return targetType.isAssignableFrom(sourceType);
    }

    /**
     * Whether the given class is {@code Object} or {@code Void}.
     *
     * @param clazz clazz
     * @return boolean
     */
    public static boolean isObjectOrVoid(Class<?> clazz) {
        return Objects.equals(Object.class, clazz)
            || Objects.equals(Void.TYPE, clazz);
    }

    /**
     * <p>Whether the given class is from packages
     * which package name is started with "java." or "javax.".
     *
     * @param clazz class
     * @return is jdk class
     */
    public static boolean isJdkClass(Class<?> clazz) {
        Objects.requireNonNull(clazz, "class name must not null");
        final Package objectPackage = clazz.getPackage();
        // unable to determine the package in which it is located, maybe is a proxy class？
        if (Objects.isNull(objectPackage)) {
            return false;
        }
        final String objectPackageName = objectPackage.getName();
        return objectPackageName.startsWith("java.")
            || objectPackageName.startsWith("javax.")
            || clazz.getClassLoader() == null;
    }

    /**
     * <p>Get class by class name.
     *
     * @param className class name
     * @return class instance
     * @throws Ioc4jException if class not found
     */
    public static Class<?> forName(String className) throws Ioc4jException {
        Objects.requireNonNull(className, "class name must not null");
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new Ioc4jException(e);
        }
    }

    /**
     * <p>Get class by class name, if class not found, return default class.
     *
     * @param className class name, it may be null or empty
     * @param defaultClass default class
     * @return class instance or default class
     * @throws Ioc4jException if class which specified by className not found
     */
    public static Class<?> forName(@Nullable String className, Class<?> defaultClass) {
        if (StringUtils.isNotEmpty(className)) {
            return forName(className);
        }
        return defaultClass;
    }

    /**
     * <p>Create new instance of given type.
     *
     * @param type type
     * @param <T> type
     * @return new instance
     * @throws Ioc4jException if create instance failed
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(@NonNull Class<?> type) {
        Objects.requireNonNull(type, "type must not null");
        try {
            return (T) type.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new Ioc4jException(e);
        }
    }

    /**
     * <p>Convert package path to resource path.<br />
     * eg: {@code cn.crane4j.core.util.ClassUtils -> cn/crane4j/core/util/ClassUtils}
     *
     * @param packagePath class path
     * @return resource path
     */
    public static String packageToPath(String packagePath) {
        Objects.requireNonNull(packagePath, "packagePath must not null");
        return packagePath.replace(".", "/");
    }
}
