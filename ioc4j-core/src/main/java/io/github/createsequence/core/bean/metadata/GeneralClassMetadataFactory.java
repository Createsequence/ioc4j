package io.github.createsequence.core.bean.metadata;

import io.github.createsequence.core.util.ArrayUtils;
import io.github.createsequence.core.util.CollectionUtils;
import io.github.createsequence.core.util.ReflectUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * {@link ClassMetadataFactory}的通用实现
 *
 * @author huangchengxing
 */
public class GeneralClassMetadataFactory implements ClassMetadataFactory {

    private final Map<Class<?>, ClassMetadataImpl> metadataCaches = new ConcurrentHashMap<>();

    /**
     * 获取类型对应的元数据
     *
     * @param type 类型
     * @return 元数据
     */
    @Override
    public ClassMetadata resolve(Class<?> type) {
        return metadataCaches.computeIfAbsent(type, this::doResolve);
    }

    private ClassMetadataImpl doResolve(Class<?> type) {
        ClassMetadataImpl metadata = createClassMetadata(type);

        // 搜集类中所有的属性
        Field[] declaredFields = type.getDeclaredFields();
        if (ArrayUtils.isNotEmpty(declaredFields)) {
            List<FieldMetadata> fieldMetadataList = Stream.of(declaredFields)
                .map(field -> createFieldMetadata(metadata, field))
                .toList();
            metadata.setDeclaredFields(fieldMetadataList);
        }

        // 收集类中所有的方法
        Method[] declaredMethods = type.getDeclaredMethods();
        if (ArrayUtils.isNotEmpty(declaredMethods)) {
            List<MethodMetadata> methodMetadataList = Stream.of(declaredMethods)
                .filter(method -> !method.isSynthetic())
                .map(method -> createMethodMetadata(metadata, method))
                .toList();
            metadata.setDeclaredMethods(methodMetadataList);
        }

        // 搜集父类或父接口
        Set<Class<?>> superAndInterfaces = ReflectUtils.getDeclaredSuperClassWithInterface(type);
        superAndInterfaces.remove(Object.class);
        if (CollectionUtils.isNotEmpty(superAndInterfaces)) {
            List<Class<?>> parents = List.copyOf(superAndInterfaces);
            metadata.setSuperclasses(parents);
        }

        return metadata;
    }

    /**
     * 创建类型元数据
     *
     * @param type 类型
     * @return 类型元数据
     */
    protected ClassMetadataImpl createClassMetadata(Class<?> type) {
        return new ClassMetadataImpl(type);
    }

    /**
     * 创建属性元数据
     *
     * @param metadata 类型元数据
     * @param field 属性
     * @return 属性元数据
     */
    protected FieldMetadata createFieldMetadata(ClassMetadataImpl metadata, Field field) {
        return new FieldMetadataImpl(field, metadata);
    }

    /**
     * 创建方法元数据
     *
     * @param metadata 类型元数据
     * @param method 方法
     * @return 方法元数据
     */
    protected MethodMetadata createMethodMetadata(ClassMetadataImpl metadata, Method method) {
        return new MethodMetadataImpl(method, metadata);
    }

    /**
     * 类型元数据实现
     *
     * @author huangchengxing
     */
    @Setter
    @Getter
    @RequiredArgsConstructor
    protected class ClassMetadataImpl implements ClassMetadata {

        private final Class<?> source;
        private List<MethodMetadata> declaredMethods = Collections.emptyList();
        private List<FieldMetadata> declaredFields = Collections.emptyList();
        private List<Class<?>> superclasses = Collections.emptyList();

        /**
         * 若元数据存在父级节点，则返回父级节点的元数据。<br/>
         * 返回的列表中的元素顺序遵循就近原则，比如若是类，则其父类的元数据应当比祖父类的元数据优先。
         *
         * @return 父级元数据，返回的列表不可变
         */
        @NonNull
        @Override
        public Collection<ClassMetadata> getParents() {
            return superclasses.stream()
                .map(GeneralClassMetadataFactory.this::resolve)
                .toList();
        }
    }

    /**
     * 方法元数据实现
     *
     * @author huangchengxing
     */
    @Getter
    @RequiredArgsConstructor
    protected class MethodMetadataImpl implements MethodMetadata {

        private final Method source;
        private final ClassMetadata declaringClassMetadata;
        private volatile Collection<MethodMetadata> parents;

        /**
         * 获取被该方法重写的所有父类方法
         *
         * @return 父级元数据，返回的列表不可变
         */
        @NonNull
        @Override
        public Collection<MethodMetadata> getParents() {
            if (Objects.isNull(parents)) {
                synchronized (this) {
                    if (Objects.isNull(parents)) {
                        parents = MethodMetadata.super.getParents();
                    }
                }
            }
            return parents;
        }
    }

    /**
     * 属性元数据实现
     *
     * @author huangchengxing
     */
    @Getter
    @RequiredArgsConstructor
    protected static class FieldMetadataImpl implements FieldMetadata {
        private final Field source;
        private final ClassMetadata declaringClassMetadata;
    }
}
