package io.github.createsequence.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 按条件对指定对象进行依赖注入。
 *
 * @author huangchengxing
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.FIELD})
public @interface Autoinject {

    /**
     * 要注入对象的类型
     * <ul>
     *     <li>当注解在属性上时，默认为属性类型；</li>
     *     <li>当注解在参数上时，默认为参数类型；</li>
     *     <li>当注解在方法上时无任何效果；</li>
     * </ul>
     *
     * @return 类型
     */
    Class<?> type() default Object.class;

    /**
     * 要注入的对象的BeanName：
     * <ul>
     *     <li>当注解在属性上时，默认为属性名；</li>
     *     <li>当注解在参数上时，默认为参数名；</li>
     *     <li>当注解在方法上时无任何效果；</li>
     * </ul>
     *
     * @return BeanName
     */
    String name() default "";

    /**
     * 是否非空，若为{@code true}，则若注入过程中无法找到可注入的对象会直接报错
     *
     * @return 是否
     */
    boolean required() default true;
}
