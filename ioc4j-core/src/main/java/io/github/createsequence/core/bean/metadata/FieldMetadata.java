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
     * 获取方法的声明类
     *
     * @return 方法的声明类
     */
    default Class<?> getDeclaringClass() {
        return getSource().getDeclaringClass();
    }
}
