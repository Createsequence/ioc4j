package io.github.createsequence.core.inject;

import io.github.createsequence.core.container.BeanRegister;
import lombok.RequiredArgsConstructor;

/**
 * @author huangchengxing
 */
@RequiredArgsConstructor
public class DefaultInjector implements Injector {

    private final BeanRegister beanRegister;

    /**
     * 对目标对象进行依赖注入
     *
     * @param target 待依赖注入的对象
     */
    @Override
    public void inject(Object target) {

    }
}
