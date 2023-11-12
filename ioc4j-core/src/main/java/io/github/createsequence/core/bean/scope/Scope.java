package io.github.createsequence.core.bean.scope;

import java.util.function.Supplier;

/**
 * Bean的作用域
 *
 * @author huangchengxing
 */
public interface Scope {

    /**
     * 单例
     */
    String SINGLETON = "singleton";

    /**
     * 多例
     */
    String PROTOTYPE = "prototype";

    /**
     * 获取Bean的名称
     *
     * @param name bean的名称
     * @param supplier bean的名称
     * @param <T> bean的类型
     * @return bean实例
     */
    <T> T get(String name, Supplier<T> supplier);
}
