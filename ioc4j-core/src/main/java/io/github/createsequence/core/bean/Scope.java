package io.github.createsequence.core.bean;

import java.util.function.Supplier;

/**
 * Bean的作用域
 *
 * @author huangchengxing
 */
public interface Scope {

    String PROTOTYPE = "prototype";
    String SINGLETON = "singleton";

    /**
     * 获取作用域名称
     *
     * @return 名称
     */
    String getName();

    /**
     * 获取Bean
     *
     * @param beanName bean名称
     * @param supplier bean提供者
     * @param <T> bean类型
     * @return Bean实例
     */
    <T> T getBean(String beanName, Supplier<T> supplier);
}
