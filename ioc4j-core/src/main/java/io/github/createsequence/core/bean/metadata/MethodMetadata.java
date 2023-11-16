package io.github.createsequence.core.bean.metadata;

import io.github.createsequence.core.support.annotation.HierarchicalAnnotatedElement;
import io.github.createsequence.core.util.ReflectUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * 方法元数据，表示一个java方法
 *
 * @author huangchengxing
 */
public interface MethodMetadata extends HierarchicalAnnotatedElement<Method, MethodMetadata> {

    /**
     * 方法是否为公开方法
     *
     * @return 是否
     */
    default boolean isPublic() {
        return Modifier.isPublic(getSource().getModifiers());
    }

    /**
     * 方法是否为静态方法
     *
     * @return 是否
     */
    default boolean isStatic() {
        return Modifier.isStatic(getSource().getModifiers());
    }

    /**
     * 获取方法的声明类的元数据对象
     *
     * @return 方法的声明类的元数据对象
     */
    ClassMetadata getDeclaringClassMetadata();

    /**
     * 该方法是否重写自父类
     *
     * @return 是否
     */
    default boolean isOverride() {
        return getOverwrittenMethods()
            .findFirst()
            .isPresent();
    }

    /**
     * 获取被该方法重写的所有父类方法
     *
     * @return 父级元数据，返回的列表不可变
     */
    @Override
    @NonNull
    default Collection<MethodMetadata> getParents() {
        return getOverwrittenMethods().toList();
    }

    /**
     * 当前方法是否重写自指定方法
     *
     * @param parentMethod 父类中的方法
     * @return boolean
     */
    default boolean isOverrideFrom(@NonNull Method parentMethod) {
        return ReflectUtils.isOverrideableFrom(getSource(), parentMethod);
    }

    /**
     * 获取方法声明类的父类或父接口中，被当前方法重写的原方法
     *
     * @return 被重写方法组成的流
     * @see #isOverrideFrom
     */
    private Stream<MethodMetadata> getOverwrittenMethods() {
        return getDeclaringClassMetadata()
            .stream()
            .map(ClassMetadata::getDeclaredMethods)
            .flatMap(Collection::stream)
            .filter(mm -> mm != this && isOverrideFrom(mm.getSource()));
    }
}
