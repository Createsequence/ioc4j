package io.github.createsequence.core.bean;

import java.util.Map;

/**
 * Bean管理器
 *
 * @author huangchengxing
 */
public interface BeanManager {

    /**
     * 获取Bean
     *
     * @param beanName bean名称
     * @param <T> bean类型
     * @return Bean
     */
    <T> T getBean(String beanName);
    
    /**
     * 获取Bean
     *
     * @param beanType bean类型
     * @param <T> bean类型
     * @return Bean
     */
    <T> T getBean(Class<T> beanType);

    /**
     * 获取Bean
     *
     * @param beanType bean类型
     * @param name bean类型
     * @param <T> bean类型
     * @return Bean
     */
    <T> T getBean(Class<T> beanType, String name);

    /**
     * 获取Bean
     *
     * @param beanType bean类型
     * @param <T> bean类型
     * @return Bean
     */
    <T> Map<String, T> getBeansOfType(Class<T> beanType);

    /**
     * 销毁Bean
     *
     * @param beanName bean名称
     */
    void destroyBean(String beanName);
}
