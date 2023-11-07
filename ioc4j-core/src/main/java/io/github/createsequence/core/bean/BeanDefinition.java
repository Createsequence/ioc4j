package io.github.createsequence.core.bean;

import io.github.createsequence.core.inject.InjectionPoint;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

/**
 * Bean定义，与一个java类对应，表示一个受容器管理的、其本身或子类可实例化、可被依赖注入并且也可用于依赖注入的对象。
 *
 * @author huangchengxing
 */
public interface BeanDefinition {

    /**
     * 获取Bean名称
     *
     * @return Bean名称
     */
    String getName();

    /**
     * 获取Bean类型
     *
     * @return Bean类型
     */
    Class<?> getType();

    /**
     * 获取Bean的作用域名称
     *
     * @return 作用域名称
     * @see Scope#getName()
     */
    String getScope();

    /**
     * 该定义是否代表一个可实例化的bean，如果不是则可能为接口或抽象类
     *
     * @return 是否
     */
    boolean isAbstract();

    /**
     * 是否为优先Bean，若为{@code true}，
     * 则若存在多个相同类型的bean，则优先返回当前bean
     *
     * @return 是否
     */
    boolean isPrimary();

    /**
     * <p>获取实例创建工厂。<br/>
     * 若不为空，且{@link #isAbstract()}为{@code true}，
     * 则在使用构造器之前，优先尝试通过该工厂创建
     *
     * @return 工厂方法
     */
    @Nullable
    BeanInstanceFactory getBeanInstanceFactory();

    /**
     * 获取实例的依赖注入点
     *
     * @return 依赖注入点
     */
    @NonNull
    List<InjectionPoint> getInjectionPoints();

    /**
     * 若当前类有父类或父接口，则获取父类或父接口对应的bean定义名称
     *
     * @return 上级bean定义名称
     */
    @Nullable
    String getParentDefinitionName();
}
