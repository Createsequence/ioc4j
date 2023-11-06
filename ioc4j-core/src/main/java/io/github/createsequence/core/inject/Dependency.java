package io.github.createsequence.core.inject;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author huangchengxing
 */
public interface Dependency {

    /**
     * 获取注入对象的BeanName
     *
     * @return BeanName
     */
    String getName();

    /**
     * 获取注入对象的类型
     *
     * @return 类型
     */
    Class<?> getType();

    /**
     * 当前依赖项对应注入点的第几个参数
     *
     * @return 参数下标
     */
    int getArgIndex();

    @Getter
    @AllArgsConstructor
    @RequiredArgsConstructor
    class Impl implements Dependency {
        private final String name;
        private final Class<?> type;
        private int argIndex = 0;
    }
}
