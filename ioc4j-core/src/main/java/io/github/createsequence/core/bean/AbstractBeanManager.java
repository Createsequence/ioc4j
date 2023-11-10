package io.github.createsequence.core.bean;

import io.github.createsequence.core.bean.processor.LifecycleProcessor;
import io.github.createsequence.core.bean.scope.Scope;
import io.github.createsequence.core.util.Asserts;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author huangchengxing
 */
@RequiredArgsConstructor
public abstract class AbstractBeanManager implements ConfigurableBeanManager {

    private final BeanDefinitionRegister beanDefinitionRegister;
    private final List<LifecycleProcessor> lifecycleProcessors = new ArrayList<>();
    private final Map<String, Scope> scopes = new HashMap<>(4);

    @Override
    public void addLifecycleProcessor(LifecycleProcessor lifecycleProcessor) {
        Asserts.isNotNull(lifecycleProcessor, "lifecycleProcessor must not null!");
        lifecycleProcessors.add(lifecycleProcessor);
    }

    @Override
    public void addScope(Scope scope) {
        scopes.put(scope.getName(), scope);
    }

    protected void init() {

    }
}
