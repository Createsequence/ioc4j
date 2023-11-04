package io.github.createsequence.core.inject;

import io.github.createsequence.core.container.BeanRegister;
import io.github.createsequence.core.util.AnnotationUtils;
import io.github.createsequence.core.util.ReflectUtils;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.reflect.Field;

/**
 * 基于属性的依赖解析器
 *
 * @author huangchengxing
 */
@RequiredArgsConstructor
public class AnnotationAwareDependencyInjector implements DependencyInjector {

    private final BeanRegister beanRegister;
    private final InjectionMetadataParser injectionMetadataParser;

    /**
     * 对{@code target}进行依赖注入
     *
     * @param target 目标对象
     */
    @Override
    public void inject(@NonNull Object target) {
        Class<?> targetType = target.getClass();
        Field[] fields = ReflectUtils.getFields(targetType);
        for (Field field : fields) {
            AnnotationUtils.getAnnotation(field, Resource.class);
            AnnotationUtils.getAnnotation(field, Named.class);
            AnnotationUtils.getAnnotation(field, Inject.class);
        }
    }
}
