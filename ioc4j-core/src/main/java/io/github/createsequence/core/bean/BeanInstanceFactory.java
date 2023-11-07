package io.github.createsequence.core.bean;

/**
 * Bean实例工厂
 *
 * @author huangchengxing
 */
public interface BeanInstanceFactory {

    /**
     * 根据指定名称与依赖项，创建一个Bean实例
     *
     * @param beanName bean名称
     * @param dependencies 创建bean所必须的依赖性
     * @return bean实例
     */
    <T> T create(String beanName, Object... dependencies);
}
