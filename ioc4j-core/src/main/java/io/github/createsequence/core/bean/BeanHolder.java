package io.github.createsequence.core.bean;

import io.github.createsequence.core.bean.metadata.ClassMetadata;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Bean占位符，表示一个被容器管理的Bean实例。
 *
 * @author huangchengxing
 */
public interface BeanHolder {
    
    /**
     * 获取Bean的名称
     *
     * @return bean的名称
     */
    String getName();
    
    /**
     * 获取Bean的类型
     *
     * @return bean的类型
     */
    Class<?> getType();

    /**
     * 获取Bean实例
     *
     * @return bean实例
     */
    @Nullable
    Object getBean();

    /**
     * bean是否已经初始化完毕，若为{@code true}，则说明该Bean实例已经初始化完毕，
     * 各种回调方法已经执行完毕，并且完成所有依赖注入。
     *
     * @return bean是否已经初始化完毕
     */
    boolean isInitialized();

    /**
     * 获取Bean的作用域
     *
     * @return bean的作用域
     */
    String getScope();

    /**
     * 获取Bean的元数据
     *
     * @return bean的元数据
     */
    ClassMetadata getClassMetadata();
}
