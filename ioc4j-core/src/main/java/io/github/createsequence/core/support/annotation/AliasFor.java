package io.github.createsequence.core.support.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 令多个属性互相关联，当对其中任意属性赋值时，会将属性值一并同步到所有关联的属性中
 *
 * @author huangchengxing
 * @see AttributeResolvableAnnotation
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
public @interface AliasFor {

    /**
     * 别名值，即使用此注解要替换成的别名名称
     *
     * @return 别名值
     */
    String value();
}
