package io.github.createsequence.core.bean;

import io.github.createsequence.core.bean.processor.LifecycleProcessor;
import io.github.createsequence.core.bean.scope.Scope;

/**
 * @author huangchengxing
 */
public interface ConfigurableBeanManager extends BeanManager {

    void addLifecycleProcessor(LifecycleProcessor lifecycleProcessor);

    void addScope(Scope scope);
}
