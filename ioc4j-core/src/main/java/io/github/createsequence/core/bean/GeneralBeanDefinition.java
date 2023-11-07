package io.github.createsequence.core.bean;

import io.github.createsequence.core.inject.InjectionPoint;
import io.github.createsequence.core.util.Asserts;
import io.github.createsequence.core.util.ClassUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * {@link BeanDefinition}的通用实现
 *
 * @author huangchengxing
 */
@Accessors(chain = true)
@Setter
@Getter
public class GeneralBeanDefinition implements BeanDefinition {

    /**
     * bean名称
     */
    private String name;

    /**
     * bean类型
     */
    @Nullable
    private Class<?> type;

    /**
     * bean类型的全限定名，当无法直接指定类对象时使用
     */
    @Nullable
    private String typeName;

    /**
     * bean作用域
     */
    private String scope;

    /**
     * 是否优先
     */
    private boolean primary = false;

    /**
     * bean实例工厂
     */
    private BeanInstanceFactory beanInstanceFactory;

    /**
     * 依赖注入点
     */
    private List<InjectionPoint> injectionPoints = new ArrayList<>();

    /**
     * 父级bean定义，当需要直接指定父级bean定义时使用，优先于{@link #parentDefinitionName}属性
     */
    @Nullable
    private BeanDefinition parentBeanDefinition;

    /**
     * 父级bean定义名称
     */
    @Nullable
    private String parentDefinitionName;

    /**
     * 添加注入点
     *
     * @param injectionPoint 注入点
     * @return 当前实例
     */
    public GeneralBeanDefinition addInjectionPoint(InjectionPoint injectionPoint) {
        this.injectionPoints.add(injectionPoint);
        return this;
    }

    /**
     * 该定义是否代表一个可实例化的bean，如果不是则可能为接口或抽象类
     *
     * @return 是否
     */
    @Override
    public boolean isAbstract() {
        int mod = getType().getModifiers();
        return Modifier.isAbstract(mod) || Modifier.isInterface(mod);
    }

    /**
     * 获取bean类型
     *
     * @return bean类型
     */
    @Override
    public Class<?> getType() {
        if (Objects.nonNull(type)) {
            return type;
        }
        Asserts.isNotNull(typeName, "The bean definition has not specified a type or typeName yet!");
        type = ClassUtils.forName(typeName);
        return type;
    }

    /**
     * 获取bean类型的全限定名
     *
     * @return bean类型全限定名
     */
    public String getTypeName() {
        if (Objects.nonNull(typeName)) {
            return typeName;
        }
        Asserts.isNotNull(type, "The bean definition has not specified a type or typeName yet!");
        typeName = type.getName();
        return typeName;
    }

    /**
     * 若当前类有父类或父接口，则获取父类或父接口对应的bean定义名称
     *
     * @return 上级bean定义名称
     */
    @Override
    public String getParentDefinitionName() {
        return Objects.isNull(parentBeanDefinition) ? parentDefinitionName : parentBeanDefinition.getName();
    }
}
