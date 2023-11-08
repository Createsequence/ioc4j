package io.github.createsequence.core.bean.scope;

import java.util.function.Supplier;

/**
 * 多例作用域，在该作用域中，总是创建一个新的Bean
 *
 * @author huangchengxing
 */
public class PrototypeScope implements Scope {

    /**
     * 获取作用域名称
     *
     * @return 名称
     */
    @Override
    public String getName() {
        return PROTOTYPE;
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
        return supplier.get();
    }
}
