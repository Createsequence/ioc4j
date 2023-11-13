package io.github.createsequence.core.bean.metadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 可变的{@link AnnotatedElement}
 *
 * @author huangchengxing
 */
public interface MutableAnnotatedElement extends AnnotatedElement {

    /**
     * 添加一个注解，同一类型的注解只能有一个，且会覆盖原有注解
     *
     * @param annotation 注解
     */
    void addDeclaredAnnotation(Annotation annotation);

    /**
     * 根据属性值构建并添加一个注解，同一类型的注解只能有一个，且会覆盖原有注解
     *
     * @param annotationType 注解类型
     * @param attributeValues 注解属性值
     */
    void addDeclaredAnnotation(Class<?> annotationType, Map<String, Object> attributeValues);

    /**
     * 修改某个注解的属性值，同一类型的注解只能有一个，且会覆盖原有注解
     *
     * @param annotationType 注解类型
     * @param modifiedAttributeValues 要修改的属性值
     * @throws NoSuchElementException 当指定注解不存在时抛出
     */
    void modifyDeclaredAnnotationAttributes(Class<?> annotationType, Map<String, Object> modifiedAttributeValues) throws NoSuchElementException;

    /**
     * 修改注解的某个属性值
     *
     * @param annotationType 注解类型
     * @param attributeName 属性名称
     * @param attributeValue 属性值
     * @throws NoSuchElementException 当指定注解不存在时抛出
     */
    void modifyDeclaredAnnotationAttribute(Class<?> annotationType, String attributeName, Object attributeValue) throws NoSuchElementException;
}
