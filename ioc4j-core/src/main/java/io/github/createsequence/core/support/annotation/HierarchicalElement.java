package io.github.createsequence.core.support.annotation;

import io.github.createsequence.core.exception.Ioc4jException;
import io.github.createsequence.core.util.AnnotationUtils;
import io.github.createsequence.core.util.CollectionUtils;
import io.github.createsequence.core.util.ReflectUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * <p>{@link HierarchicalAnnotatedElement}的通用实现，
 * 表示一个具备层级结构（具备上级元素）的{@link AnnotatedElement}，比如类或方法。
 *
 * <p><strong>层级结构</strong><br />
 * <p>使用{@link #fromElement}或{@link #fromResolvedElement}从{@link AnnotatedElement}创建一个实例，
 * 若元素具备层级结构，则通过{@link #getParents}或{@link #stream}可以访问其层级结构中的上级元素。<br />
 * 针对<i>层级结构</i>的定义如下：
 * <ul>
 *     <li>
 *         如果元素是{@link Class}，则{@link #getParents}将获得当前类的<i>父类与父接口</i>，
 *         而{@link #stream}则返回其包含其本身在内层级结构中所有的父类和父接口；
 *     </li>
 *     <li>
 *         如果元素是{@link Method}，则{@link #getParents}将获得当前类的<i>父类与父接口中能够被重写的方法</i>，
 *         而{@link #stream}则返回包含其本身在内层级结构中所有可被重写的方法。<br />
 *         关于可被重写方法判断标准，参照{@link ReflectUtils#isOverrideableFrom}；
 *     </li>
 *     <li>若是其他类型，则默认其<i>不具备层级结构</i>，{@link #getParents}返回空集合，{@link #stream}返回仅包含本身的流；</li>
 * </ul>
 *
 * <p><strong>注解增强</strong><br />
 * 通过{@link #resolved()}与{@link #unresolved()}可以让元素在
 * 原始的{@link AnnotatedElement}与增强的{@link ResolvedAnnotatedElement}之间转换。<br />
 * 当元素转换为{@link ResolvedAnnotatedElement}后，
 * 获得的所有注解皆支持{@link ResolvedAnnotation}的增强机制，
 * 比如基于{@link AliasFor}的属性别名和对元注解的属性覆盖。
 *
 * <p><strong>注解查找</strong><br />
 * 通过{@link #stream}可快速遍历当前元素及其层级结构中所有上级元素，
 * 当调用{@link #resolved()}后，
 * 所有{@link AnnotatedElement}中的API均支持查找元注解。
 *
 * @author huangchengxing
 * @see ResolvedAnnotatedElement
 * @see ResolvedAnnotation
 */
@ToString(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class HierarchicalElement implements
    HierarchicalAnnotatedElement<AnnotatedElement, HierarchicalElement> {

    // TODO 更换为 WeakConcurrentHashMap，并提供主动清缓存的API
    private static final Map<AnnotatedElement, HierarchicalElement> ELEMENT_CACHES = new ConcurrentHashMap<>();
    private static final Map<AnnotatedElement, HierarchicalElement> RESOLVED_ELEMENT_CACHES = new ConcurrentHashMap<>();

    /**
     * 从元素中构建一个{@link HierarchicalElement}实例。
     * <ul>
     *     <li>如果是{@link Class}，则{@link #getParents}将获得当前类的父类与父接口；</li>
     *     <li>
     *         如果是{@link Method}，则{@link #getParents}将获得当前类的父类与父接口中，能够被重写的方法。<br/>
     *         关于可被重写方法判断标准，参照{@link ReflectUtils#isOverrideableFrom}；
     *     </li>
     *     <li>若是其他类型，则{@link #getParents}将默认返回一个空列表；</li>
     * <ul>
     *
     * @param element 元素
     * @return {@link HierarchicalElement}实例
     */
    public static HierarchicalElement fromElement(AnnotatedElement element) {
        element = WrappedAnnotatedElement.getRoot(element);
        return ELEMENT_CACHES.computeIfAbsent(element, ele -> switch (ele) {
            case Class<?> type && type.isAnnotation() -> new HierarchicalElement(
                type, ParentElementDiscoverer.META_ANNOTATION_TYPES, ElementCreator.CACHED_ELEMENT
            );
            case Class<?> type -> new HierarchicalElement(
                type, ParentElementDiscoverer.SUPERCLASS_AND_INTERFACES, ElementCreator.CACHED_ELEMENT
            );
            case Method method -> new HierarchicalElement(
                method, ParentElementDiscoverer.OVERRIDEABLE_METHODS, ElementCreator.CACHED_ELEMENT
            );
            default -> new HierarchicalElement(
                ele, ParentElementDiscoverer.NO_HIERARCHY_ELEMENT, ElementCreator.CACHED_ELEMENT
            );
        });
    }

    /**
     * 从元素中构建一个{@link HierarchicalElement}实例。
     * <ul>
     *     <li>如果是{@link Class}，则{@link #getParents}将获得当前类的父类与父接口；</li>
     *     <li>
     *         如果是{@link Method}，则{@link #getParents}将获得当前类的父类与父接口中，能够被重写的方法。<br/>
     *         关于可被重写方法判断标准，参照{@link ReflectUtils#isOverrideableFrom}；
     *     </li>
     *     <li>若是其他类型，则{@link #getParents}将默认返回一个空列表；</li>
     * <ul>
     *
     * @param element 元素
     * @return {@link HierarchicalElement}实例
     */
    public static HierarchicalElement fromResolvedElement(AnnotatedElement element) {
        element = WrappedAnnotatedElement.getRoot(element);
        return RESOLVED_ELEMENT_CACHES.computeIfAbsent(element, ele -> switch (ele) {
            case Class<?> type && type.isAnnotation() -> new HierarchicalElement(
                ResolvedAnnotatedElement.of(type), ParentElementDiscoverer.META_ANNOTATION_TYPES, ElementCreator.CACHED_RESOLVED_ELEMENT
            );
            case Class<?> type -> new HierarchicalElement(
                ResolvedAnnotatedElement.of(type), ParentElementDiscoverer.SUPERCLASS_AND_INTERFACES, ElementCreator.CACHED_RESOLVED_ELEMENT
            );
            case Method method -> new HierarchicalElement(
                ResolvedAnnotatedElement.of(method), ParentElementDiscoverer.OVERRIDEABLE_METHODS, ElementCreator.CACHED_RESOLVED_ELEMENT
            );
            default -> new HierarchicalElement(
                ResolvedAnnotatedElement.of(ele), ParentElementDiscoverer.NO_HIERARCHY_ELEMENT, ElementCreator.CACHED_RESOLVED_ELEMENT
            );
        });
    }

    /**
     * 清除缓存
     */
    public static void clearCaches() {
        ELEMENT_CACHES.clear();
        RESOLVED_ELEMENT_CACHES.clear();
    }

    /**
     * 被包装的{@link AnnotatedElement}
     */
    @ToString.Include
    @Getter
    private final AnnotatedElement source;
    
    /**
     * 上级元素查找器
     */
    private final ParentElementDiscoverer parentElementDiscoverer;

    /**
     * {@link HierarchicalElement}创建器
     */
    private final ElementCreator elementCreator;

    /**
     * <p>上级元素缓存，在调用{@link #getParents}时触发加载。<br />
     * 我们令每一个结点都仅缓存其直接上级节点，从而实现整个层级结构中的所有节点的懒加载，
     * 这在基于{@link ResolvedAnnotation}实现构建树结构时，能避免过多的查找和解析过程。
     */
    private volatile Collection<HierarchicalElement> parents;

    /**
     * 将元素转为{@link ResolvedAnnotatedElement}
     *
     * @return {@link ResolvedAnnotatedElement}
     */
    public HierarchicalElement resolved() {
        return fromResolvedElement(getSource());
    }

    /**
     * 将元素转为普通的{@link AnnotatedElement}
     *
     * @return {@link AnnotatedElement}
     */
    public HierarchicalElement unresolved() {
        return fromElement(getSource());
    }

    /**
     * 获取被包装的元素
     *
     * @param elementType 目标类型
     * @return 元素
     */
    public <E extends AnnotatedElement> E getSource(Class<E> elementType) throws ClassCastException {
        return elementType.cast(getSource());
    }

    /**
     * 获取指定元素的流
     *
     * @param elementType 元素类型
     * @return 元素流
     */
    public <E extends AnnotatedElement> Stream<E> stream(Class<E> elementType) throws ClassCastException {
        return stream()
            .map(HierarchicalElement::resolved)
            .map(e -> elementType.cast(getSource()));
    }

    /**
     * 获取父级别元素
     *
     * @return 父级别元素
     */
    @NonNull
    @Override
    public Collection<HierarchicalElement> getParents() {
        if (parents == null) {
            synchronized (this) {
                if (parents == null) {
                    var ps = parentElementDiscoverer.resolve(source).stream()
                        .map(e -> elementCreator.create(e, parentElementDiscoverer, elementCreator))
                        .toList();
                    parents = CollectionUtils.isNotEmpty(ps) ? ps : Collections.emptyList();
                }
            }
        }
        return parents;
    }

    /**
     * 上级节点查找器
     *
     * @author huangchengxing
     * @see #NO_HIERARCHY_ELEMENT
     * @see #OVERRIDEABLE_METHODS
     * @see #SUPERCLASS_AND_INTERFACES
     */
    private interface ParentElementDiscoverer {

        /**
         * 默认返回空集合
         */
        ParentElementDiscoverer NO_HIERARCHY_ELEMENT = e -> Collections.emptyList();

        /**
         * 获取指定注解类上的元注解，忽略jdk的原生注解
         */
        ParentElementDiscoverer META_ANNOTATION_TYPES = ele -> {
            ele = WrappedAnnotatedElement.getRoot(ele);
            if (ele instanceof Class<?> type && type.isAnnotation()) {
                return Arrays.stream(type.getDeclaredAnnotations())
                    .map(Annotation::annotationType)
                    .filter(AnnotationUtils::isNotJdkMetaAnnotation)
                    .toList();
            }
            throw new Ioc4jException("element must be a type of annotation: [{}]", ele);
        };

        /**
         * 获得指定方法声明类的父类与父接口中，能够被重写的方法。<br/>
         * 关于可被重写方法判断标准，参照{@link ReflectUtils#isOverrideableFrom}；
         */
        ParentElementDiscoverer OVERRIDEABLE_METHODS = ele -> {
            ele = WrappedAnnotatedElement.getRoot(ele);
            if (ele instanceof Method method) {
                return ReflectUtils.getDeclaredSuperClassWithInterface(method.getDeclaringClass()).stream()
                    .map(ReflectUtils::getDeclaredMethods)
                    .flatMap(Arrays::stream)
                    .filter(m -> ReflectUtils.isOverrideableFrom(method, m))
                    .toList();
            }
            throw new Ioc4jException("element must be a method: [{}]", ele);
        };

        /**
         * 获取指定类的父类或父接口
         */
        ParentElementDiscoverer SUPERCLASS_AND_INTERFACES = ele -> {
            ele = WrappedAnnotatedElement.getRoot(ele);
            if (ele instanceof Class<?> type) {
                return ReflectUtils.getDeclaredSuperClassWithInterface(type);
            }
            throw new Ioc4jException("element must be a class: [{}]", ele);
        };

        /**
         * 获取上级元素
         *
         * @param element 元素
         * @return 上级元素
         */
        @NonNull
        Collection<? extends AnnotatedElement> resolve(AnnotatedElement element);
    }

    /**
     * 用于将{@link AnnotatedElement}转为{@link HierarchicalElement}
     *
     * @author huangchengxing
     * @see #CACHED_ELEMENT
     * @see #CACHED_RESOLVED_ELEMENT
     */
    private interface ElementCreator {

        /**
         * 优先从{@link #ELEMENT_CACHES}缓存中查找，不存在再新建
         */
        ElementCreator CACHED_ELEMENT = (e, r, c) -> ELEMENT_CACHES.computeIfAbsent(
            e, ele -> new HierarchicalElement(ele, r, c)
        );

        /**
         * 优先从{@link #RESOLVED_ELEMENT_CACHES}缓存中查找，不存在再基于{@link ResolvedAnnotatedElement}新建
         */
        ElementCreator CACHED_RESOLVED_ELEMENT = (e, r, c) -> RESOLVED_ELEMENT_CACHES.computeIfAbsent(e, ele ->
            new HierarchicalElement(ResolvedAnnotatedElement.of(ele), r, c)
        );

        /**
         * 将{@link AnnotatedElement}转为{@link HierarchicalElement}
         *
         * @param element 元素
         * @param parentElementDiscoverer 上级元素解析器
         * @param elementCreator 元素构建器
         * @return {@link HierarchicalElement}
         */
        @NonNull HierarchicalElement create(
            AnnotatedElement element, ParentElementDiscoverer parentElementDiscoverer, ElementCreator elementCreator);
    }
}
