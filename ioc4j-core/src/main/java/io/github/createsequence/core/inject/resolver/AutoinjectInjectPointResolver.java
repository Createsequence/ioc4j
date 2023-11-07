package io.github.createsequence.core.inject.resolver;

import io.github.createsequence.core.annotation.Autoinject;
import io.github.createsequence.core.inject.InjectionPoint;
import io.github.createsequence.core.inject.InjectorPointResolver;
import io.github.createsequence.core.util.ClassUtils;
import io.github.createsequence.core.util.ReflectUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * @author huangchengxing
 * @see Autoinject
 */
public class AutoinjectInjectPointResolver implements InjectorPointResolver {

    /**
     * 从指定类中解析得到注入点
     *
     * @param targetType 目标类型
     * @return 注入点
     */
    @Override
    public List<InjectionPoint> resolve(Class<?> targetType) {
        Set<Class<?>> classes = ReflectUtils.getDeclaredSuperClassWithInterface(targetType);
        for (Class<?> clz : classes) {
            if (ClassUtils.isJdkClass(clz)) {
                continue;
            }

        }
        return null;
    }

    @Nullable
    protected InjectionPoint resolveField(Class<?> targetType, Field field) {

    }

    @Nullable
    protected InjectionPoint resolveMethod(Class<?> targetType, Method method) {

    }

    @Nullable
    protected InjectionPoint resolveConstructor(Class<?> targetType, Constructor<?> constructor) {

    }
}
