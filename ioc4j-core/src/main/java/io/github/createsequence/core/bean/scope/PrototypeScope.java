package io.github.createsequence.core.bean.scope;

import java.util.function.Supplier;

/**
 * 多例，每次获取bean时都会创建一个新的实例。
 *
 * @author huangchengxing
 */
public class PrototypeScope implements Scope {

    /**
     * 获取Bean的名称
     *
     * @param name     bean的名称
     * @param supplier bean的名称
     * @return bean实例
     */
    @Override
    public <T> T get(String name, Supplier<T> supplier) {
        return supplier.get();
    }
}
