package io.github.createsequence.core.container;

import io.github.createsequence.core.exception.Ioc4jException;

/**
 * 单例Bean注册表
 *
 * @author huangchengxing
 */
public interface SingletonBeanRegister extends BeanRegister {

    /**
     * 注册单例Bean
     *
     * @param beanName bean名称
     * @param bean 单例Bean
     * @throws Ioc4jException 如果beanName已存在，则抛出异常
     */
    void registerSingleton(String beanName, Object bean) throws Ioc4jException;
}
