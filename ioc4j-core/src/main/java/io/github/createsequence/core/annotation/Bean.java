package io.github.createsequence.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标明当前类或方法的返回值可作为一个Bean注册到容器，
 * 该Bean将会在后续用于注入其他对象中
 *
 * @author huangchengxing
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.TYPE})
public @interface Bean {

    /**
     * BeanName
     *
     * @return bean名称
     */
    String name();

    /**
     * Bean在容器中的类型。
     * <ul>
     *     <li>当注解在类上时，默认为类本身；</li>
     *     <li>当注解在方法上时，默认为返回值类型；</li>
     * </ul>
     *
     * @return 类型
     */
    Class<?> type() default Object.class;
}
