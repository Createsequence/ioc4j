package io.github.createsequence.core.bean;

import io.github.createsequence.core.exception.Ioc4jException;

/**
 * 单例Bean注册表
 *
 * @author huangchengxing
 */
public interface SingletonBeanRegister {

    /**
     * 注册单例Bean
     *
     * @param beanName bean名称
     * @param bean 单例Bean
     * @throws Ioc4jException 如果beanName已存在，则抛出异常
     */
    void registerSingleton(String beanName, Object bean) throws Ioc4jException;

    /**
     * 是否包含指定单例Bean
     *
     * @param beanName bean名称
     * @return 是否
     */
    boolean containSingleton(String beanName);
}
