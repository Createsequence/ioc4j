package io.github.createsequence.core.bean;

import io.github.createsequence.core.inject.Dependency;
import io.github.createsequence.core.inject.InjectionPoint;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Bean定义，表示一个其本身或子类可实例化的java类，
 * 通过该java类创建的bean会被容器管理，并在后续进行或用于依赖注入。
 *
 * @author huangchengxing
 */
@Accessors(chain = true)
@Setter
@Getter
public class BeanDefinition {

    /**
     * bean名称
     */
    private String beanName;

    /**
     * bean类型
     */
    private Class<?> beanType;

    /**
     * bean类型的全限定名，当无法直接指定类对象时使用
     */
    private String beanTypeName;

    /**
     * bean作用域
     */
    private String scope;

    /**
     * 是否优先
     */
    private boolean primary = false;

    /**
     * 用于通过工厂方法或构造器对Bean实例化时所需的依赖项
     */
    private List<Dependency> instantiationDependencies;

    /**
     * 依赖注入点
     */
    private List<InjectionPoint> injectionPoints = new ArrayList<>();

    /**
     * 父级bean定义名称
     */
    @Nullable
    private String parentDefinitionName;
}
