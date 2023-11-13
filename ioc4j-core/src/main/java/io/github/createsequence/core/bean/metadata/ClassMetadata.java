package io.github.createsequence.core.bean.metadata;

import io.github.createsequence.core.support.annotation.HierarchicalAnnotatedElement;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 类元数据，表示一个java类，其父节点为该类层级结构中的父类和父接口
 *
 * @author huangchengxing
 */
public interface ClassMetadata extends HierarchicalAnnotatedElement<Class<?>, ClassMetadata> {

    /**
     * 类是否是无法实例化的，比如抽象类、接口、原始类型
     *
     * @return 是否
     */
    default boolean isAbstract() {
        int mod = getSource().getModifiers();
        return Modifier.isAbstract(mod) || Modifier.isInterface(mod) || getSource().isPrimitive();
    }

    /**
     * 获取方法列表
     *
     * @return 方法列表
     */
    default List<MethodMetadata> getDeclaredMethods() {
        return Collections.emptyList();
    }

    /**
     * 获取属性列表
     *
     * @return 属性列表
     */
    default Collection<FieldMetadata> getDeclaredFields() {
        return Collections.emptyList();
    }
}
