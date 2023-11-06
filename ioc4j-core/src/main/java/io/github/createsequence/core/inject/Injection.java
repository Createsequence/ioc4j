package io.github.createsequence.core.inject;

import java.util.Collections;
import java.util.List;

/**
 * 注入点
 *
 * @author huangchengxing
 */
public interface Injection {

    /**
     * 完成依赖注入
     *
     * @param target 待注入对象
     * @param dependencies 依赖项
     */
    void inject(Object target, Object... dependencies);

    /**
     * 获取依赖项
     *
     * @return 依赖项
     */
    default List<Dependency> getDependencies() {
        return Collections.emptyList();
    }
}
