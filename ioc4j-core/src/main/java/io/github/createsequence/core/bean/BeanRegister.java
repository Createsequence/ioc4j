package io.github.createsequence.core.bean;

import java.util.List;

/**
 * Bean注册表
 *
 * @author huangchengxing
 */
public interface BeanRegister {

    /**
     * 获取Bean
     *
     * @param beanName bean名称
     * @return Bean
     */
    Object getBean(String beanName);
    
    /**
     * 获取Bean
     *
     * @param beanType bean类型
     * @return Bean
     */
    <T> T getBean(Class<T> beanType);

    /**
     * 获取Bean
     *
     * @param beanType bean类型
     * @param name bean类型
     * @return Bean
     */
    <T> T getBean(Class<T> beanType, String name);

    /**
     * 获取Bean
     *
     * @param beanType bean类型
     * @return Bean
     */
    <T> List<T> getBeansOfType(Class<T> beanType);
}
