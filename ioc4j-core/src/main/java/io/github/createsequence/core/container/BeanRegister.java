package io.github.createsequence.core.container;

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
}
