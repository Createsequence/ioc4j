package io.github.createsequence.core.bean;

import java.util.Collection;
import java.util.List;

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
     * 获取所有指定类型的Bean定义
     *
     * @param type bean类型
     * @return bean定义
     */
    Collection<BeanDefinition> getBeanDefinitionsOfType(Class<?> type);

    /**
     * 获取所有指定类型的Bean定义名称
     *
     * @param type 类型
     * @return bean名称
     */
    default List<String> getBeanNamesOfType(Class<?> type) {
        return getBeanDefinitionsOfType(type).stream()
            .map(BeanDefinition::getName)
            .toList();
    }
}
