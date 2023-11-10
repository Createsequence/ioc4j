package io.github.createsequence.core.bean.processor;

import io.github.createsequence.core.bean.BeanDefinition;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Bean初始化阶段处理器
 *
 * @author huangchengxing
 */
public interface BeanInitializationProcessor extends LifecycleProcessor {

    /**
     * Bean实例化前回调
     *
     * @param beanName bean名称
     * @param beanDefinition bean定义
     * @param bean bean
     * @return bean对象，若不为{@code null}则直接跳过实例化，而直接使用返回的实例
     */
    @Nullable
    default Object beforeBeanInitialization(String beanName, BeanDefinition beanDefinition, Object bean) {
        return null;
    }

    /**
     * Bean实例化后回调
     *
     * @param beanName bean名称
     * @param beanDefinition bean定义
     * @param bean bean
     * @return bean对象，若不为{@code null}则直接跳过实例化，而直接使用返回的实例
     */
    @Nullable
    default Object afterBeanInitialization(String beanName, BeanDefinition beanDefinition, Object bean) {
        return bean;
    }
}
