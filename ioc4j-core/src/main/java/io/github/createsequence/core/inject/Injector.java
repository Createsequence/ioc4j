package io.github.createsequence.core.inject;

/**
 * @author huangchengxing
 */
public interface Injector {

    /**
     * 对目标对象进行依赖注入
     *
     * @param target 待依赖注入的对象
     */
    void inject(Object target);
}
