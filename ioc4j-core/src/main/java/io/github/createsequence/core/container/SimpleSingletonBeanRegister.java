package io.github.createsequence.core.container;

import io.github.createsequence.core.util.Asserts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认单例Bean注册表
 *
 * @author huangchengxing
 */
public class SimpleSingletonBeanRegister implements SingletonBeanRegister {

    private final Map<String, Object> singletonBeans = new HashMap<>();

    /**
     * 获取单例Bean
     *
     * @param beanName bean名称
     * @return 单例Bean
     */
    @Override
    public Object getBean(String beanName) {
        Object bean = singletonBeans.get(beanName);
        Asserts.isNotNull(bean, "No such bean '{}'", beanName);
        return bean;
    }

    /**
     * 获取Bean名称
     *
     * @param beanType bean类型
     * @return Bean名称
     */
    @Override
    public List<String> getBeanNamesOfType(Class<?> beanType) {
        return singletonBeans.entrySet().stream()
            .filter(e -> beanType.isAssignableFrom(e.getValue().getClass()))
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * 获取Bean
     *
     * @param beanType bean类型
     * @return Bean
     */
    @Override
    public <T> T getBean(Class<T> beanType) {
        return null;
    }

    /**
     * 获取Bean
     *
     * @param beanType bean类型
     * @return Bean
     */
    @Override
    public <T> List<T> getBeansOfType(Class<T> beanType) {
        return null;
    }

    /**
     * 获取Bean
     *
     * @param beanType bean类型
     * @param name     bean类型
     * @return Bean
     */
    @Override
    public <T> T getBean(Class<T> beanType, String name) {
        return null;
    }

    /**
     * 注册单例Bean
     *
     * @param beanName bean名称
     * @param bean 单例Bean
     */
    @Override
    public void registerSingleton(String beanName, Object bean) {
        Object singleton = singletonBeans.get(beanName);
        Asserts.isNull(singleton, "Bean name '{}' already exists");
        singletonBeans.put(beanName, bean);
    }
}
