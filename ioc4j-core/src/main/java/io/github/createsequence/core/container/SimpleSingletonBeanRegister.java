package io.github.createsequence.core.container;

import io.github.createsequence.core.util.Asserts;

import java.util.HashMap;
import java.util.Map;

/**
 * 默认单例Bean注册表
 *
 * @author huangchengxing
 */
public class SimpleSingletonBeanRegister implements SingletonBeanRegister {

    private final Map<String, Object> singletonObjects = new HashMap<>();

    /**
     * 获取单例Bean
     *
     * @param beanName bean名称
     * @return 单例Bean
     */
    @Override
    public Object getBean(String beanName) {
        Object bean = singletonObjects.get(beanName);
        Asserts.isNotNull(bean, "No such bean '{}'", beanName);
        return bean;
    }

    /**
     * 注册单例Bean
     *
     * @param beanName bean名称
     * @param bean 单例Bean
     */
    @Override
    public void registerSingleton(String beanName, Object bean) {
        Object singleton = singletonObjects.get(beanName);
        Asserts.isNull(singleton, "Bean name '{}' already exists");
        singletonObjects.put(beanName, bean);
    }
}
