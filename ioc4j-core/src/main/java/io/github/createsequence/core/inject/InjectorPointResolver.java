package io.github.createsequence.core.inject;

import java.util.List;

/**
 * @author huangchengxing
 */
public interface InjectorPointResolver {

    /**
     * 从指定类中解析得到注入点
     *
     * @param targetType 目标类型
     * @return 注入点
     */
    List<InjectionPoint> resolve(Class<?> targetType);
}
