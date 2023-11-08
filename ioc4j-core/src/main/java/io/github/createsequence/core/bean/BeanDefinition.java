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
 * Bean定义，与一个java类对应，表示一个被管理的、其子类或本身可实例化，并且可被依赖注入或用于依赖注入的对象
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
