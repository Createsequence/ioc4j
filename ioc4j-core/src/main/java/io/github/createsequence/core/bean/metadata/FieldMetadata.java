package io.github.createsequence.core.bean.metadata;

import io.github.createsequence.core.support.HierarchicalAnnotatedElement;

import java.lang.reflect.Field;

/**
 * 属性元数据，表示一个java属性
 *
 * @author huangchengxing
 */
public interface FieldMetadata extends HierarchicalAnnotatedElement<Field, FieldMetadata> {

    /**
     * 获取属性的声明类
     *
     * @return 方法的声明类
     */
    ClassMetadata getDeclaringClassMetadata();

    /**
     * 获取属性名
     *
     * @return 属性名
     */
    default String getName() {
        return getSource().getName();
    }

    /**
     * 获取方法的全限定名
     *
     * @return 方法的全限定名
     */
    default String getFullName() {
        return getSource().toString();
    }

    /**
     * 获取属性类型
     *
     * @return 属性类型
     */
    default Class<?> getType() {
        return getSource().getType();
    }
}
