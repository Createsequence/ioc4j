package io.github.createsequence.core.inject;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 用于描述注入对象的元数据
 *
 * @author huangchengxing
 */
public interface InjectionMetadata {

    /**
     * 获取元数据的源
     *
     * @return 源
     */
    Object getSource();

    /**
     * 获取注入对象的类型
     *
     * @return 注入对象的类型
     */
    @Nullable
    Class<?> getType();

    /**
     * 获取注入对象的名称
     *
     * @return 注入对象的名称
     */
    @Nullable
    String getName();
}
