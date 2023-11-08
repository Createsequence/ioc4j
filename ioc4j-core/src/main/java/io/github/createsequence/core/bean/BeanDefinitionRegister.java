package io.github.createsequence.core.bean;

import java.util.Collection;

/**
 * bean定义注册表
 *
 * @author huangchengxing
 */
public interface BeanDefinitionRegister {

    /**
     * 注册bean定义
     *
     * @param beanDefinition bean定义
     */
    void registerBeanDefinition(BeanDefinition beanDefinition);

    /**
     * bean定义是否存在
     *
     * @param beanName bean名称
     * @return 是否
     */
    boolean containsBeanDefinition(String beanName);

    /**
     * 获取bean定义
     *
     * @param beanName bean名称
     * @return bean定义
     */
    BeanDefinition getBeanDefinition(String beanName);

    /**
     * 获取所有bean定义
     *
     * @return bean定义
     */
    Collection<BeanDefinition> getBeanDefinitions();
}
