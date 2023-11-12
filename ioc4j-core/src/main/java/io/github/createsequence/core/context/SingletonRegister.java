package io.github.createsequence.core.context;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 单例注册表
 *
 * @author huangchengxing
 */
@NonNull
public interface SingletonRegister {

    /**
     * 注册单例bean
     *
     * @param name bean的名称
     * @param bean bean实例
     */
    void registerSingleton(String name, @NonNull Object bean);

    /**
     * 获取单例bean
     *
     * @param name bean的名称
     * @return bean实例
     */
    @Nullable
    Object getSingleton(String name);
}
