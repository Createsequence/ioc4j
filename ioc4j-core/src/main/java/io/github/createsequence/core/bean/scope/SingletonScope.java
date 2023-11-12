package io.github.createsequence.core.bean.scope;

import io.github.createsequence.core.context.SingletonRegister;
import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

/**
 * 单例，每次获取bean时都会返回同一个实例。
 *
 * @author huangchengxing
 * @see SingletonRegister
 */
@RequiredArgsConstructor
public class SingletonScope implements Scope {

    private final SingletonRegister singletonRegister;

    /**
     * 获取Bean的名称
     *
     * @param name bean的名称
     * @param supplier bean的名称
     * @return bean实例
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String name, Supplier<T> supplier) {
        Object singleton = singletonRegister.getSingleton(name);
        if (singleton == null) {
            singleton = supplier.get();
            singletonRegister.registerSingleton(name, singleton);
        }
        return (T) singleton;
    }
}
