package io.github.createsequence.core.inject;

/**
 * 依赖注入器
 *
 * @author huangchengxing
 */
public interface DependencyInjector {

    /**
     * 对{@code target}进行依赖注入
     *
     * @param target   目标对象
     */
    void inject(Object target);
}
