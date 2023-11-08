package io.github.createsequence.core.bean.scope;

import io.github.createsequence.core.bean.SingletonBeanRegister;
import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

/**
 * 单例作用域，固定关联一个{@link SingletonBeanRegister}，
 * 当创建Bean后将会注册到该注册表中，此后再次获取时将总是获得已注册的Bean实例。
 *
 * @author huangchengxing
 */
@RequiredArgsConstructor
public class SingletonScope implements Scope {

    private final SingletonBeanRegister singletonBeanRegister;

    /**
     * 获取作用域名称
     *
     * @return 名称
     */
    @Override
    public String getName() {
        return SINGLETON;
    }

    /**
     * 获取Bean
     *
     * @param beanName bean名称
     * @param supplier bean提供者
     * @return Bean实例
     */
    @Override
    public <T> T getBean(String beanName, Supplier<T> supplier) {
        T bean = null;
        if (!singletonBeanRegister.containSingleton(beanName)) {
            bean = supplier.get();
            singletonBeanRegister.registerSingleton(beanName, bean);
        }
        return bean;
    }
}
