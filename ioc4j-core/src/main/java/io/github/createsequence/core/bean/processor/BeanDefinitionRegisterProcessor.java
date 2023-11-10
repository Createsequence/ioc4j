package io.github.createsequence.core.bean.processor;

import io.github.createsequence.core.bean.BeanDefinitionRegister;

/**
 * <p>Bean定义注册表处理器<br />
 * 该回调接口允许在初始化动态修改或添加Bean定义
 *
 * @author huangchengxing
 */
public interface BeanDefinitionRegisterProcessor extends LifecycleProcessor {

    /**
     * 处理Bean定义注册表
     *
     * @param beanDefinitionRegister 注册表
     */
    void process(BeanDefinitionRegister beanDefinitionRegister);
}
