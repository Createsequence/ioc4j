package io.github.createsequence.core.support.annotation;

import io.github.createsequence.core.exception.Ioc4jException;
import io.github.createsequence.core.util.AnnotationUtils;
import io.github.createsequence.core.util.ArrayUtils;
import io.github.createsequence.core.util.CollectionUtils;
import io.github.createsequence.core.util.ReflectUtils;
import io.github.createsequence.core.util.Streamable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Delegate;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * <p>{@link HierarchicalAnnotatedElement}的通用实现，
 * 表示一个具备层级结构（即具备上级元素，比如类的父类或接口）的{@link AnnotatedElement}。
 *
 * <p><strong>层级结构</strong><br />
 * <p>使用{@link #from}从{@link AnnotatedElement}创建一个实例，
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
 * {@link ResolvedHierarchicalElement}中的所有注解都被封装为{@link ResolvedAnnotations}，
 * 因此通过实例获得的所有注解皆支持{@link ResolvedAnnotation}的增强机制，
 * 比如基于{@link AliasFor}的属性别名和对元注解的属性覆盖。
 *
 * <p><strong>注解查找</strong><br />
 * 你可以通过下述API访问层级结构中的各种元素和注解：
 * <ul>
 *     <li>{@link #hierarchyStream()}/{@link #hierarchies()}可快速遍历包括当前元素在内，其层级结构中所有{@link AnnotatedElement}；</li>
 *     <li>{@link #stream()}可快速遍历包括当前元素在内，其层级结构中所有{@link AnnotatedElement}上直接存在的注解及其元注解；</li>
 *     <li>{@code getXXX}：可用于访问包括当前元素在内，其层级结构中所有的注解及元注解；</li>
 *     <li>{@code getDeclaredXXX}：可用于访问包括当前元素上的注解及元注解；</li>
 * <ul>
 *
 * @author huangchengxing
 * @see ResolvedAnnotation
 */
@ToString(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ResolvedHierarchicalElement<E extends AnnotatedElement> implements AnnotatedElement, Streamable<ResolvedAnnotations> {

    private static final Map<AnnotatedElement, ResolvedHierarchicalElement<? extends AnnotatedElement>> RESOLVED_ELEMENT_CACHES = new ConcurrentHashMap<>();
    private static final NoHierarchyElementDiscoverer NO_HIERARCHY_ELEMENT = new NoHierarchyElementDiscoverer();
    private static final OverrideableMethodsDiscoverer OVERRIDEABLE_METHODS = new OverrideableMethodsDiscoverer();
    private static final SuperclassAndInterfacesDiscoverer SUPERCLASS_AND_INTERFACES = new SuperclassAndInterfacesDiscoverer();
    private static final MetaAnnotationTypesDiscoverer META_ANNOTATION_TYPES = new MetaAnnotationTypesDiscoverer();

    /**
     * 被包装的{@link AnnotatedElement}
     */
    @ToString.Include
    @Getter
    @NonNull
    private final E source;

    /**
     * 上级元素查找器
     */
    private final ParentElementDiscoverer parentElementDiscoverer;

    /**
     * 在元素上直接存在的注解
     */
    @Delegate(types = Iterable.class)
    private final List<ResolvedAnnotations> resolvedAnnotations;

    /**
     * <p>上级元素缓存，在调用{@link #getParents}时触发加载。<br />
     * 令每一个结点都仅缓存其直接上级节点，从而实现整个层级结构中的所有节点的懒加载，
     * 这在基于{@link ResolvedAnnotation}实现构建树结构时，能避免过多的查找和解析过程。
     */
    private volatile Collection<ResolvedHierarchicalElement<E>> parents;

    /**
     * 从元素中构建一个{@link ResolvedHierarchicalElement}实例。
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
     * @return {@link ResolvedHierarchicalElement}实例
     */
    @SuppressWarnings("unchecked")
    public static <E extends AnnotatedElement> ResolvedHierarchicalElement<E> from(E element) {
        element = (element instanceof ResolvedHierarchicalElement<?> rhe) ? (E) rhe.getSource() : element;
        return (ResolvedHierarchicalElement<E>) RESOLVED_ELEMENT_CACHES.computeIfAbsent(element, ele -> switch (ele) {
            case Class<?> type && type.isAnnotation() -> new ResolvedHierarchicalElement<>(type, META_ANNOTATION_TYPES);
            case Class<?> type -> new ResolvedHierarchicalElement<>(type, SUPERCLASS_AND_INTERFACES);
            case Method method -> new ResolvedHierarchicalElement<>(method, OVERRIDEABLE_METHODS);
            default -> new ResolvedHierarchicalElement<>(ele, NO_HIERARCHY_ELEMENT);
        });
    }

    /**
     * 创建一个{@link ResolvedHierarchicalElement}实例
     *
     * @param element 待包装的{@link AnnotatedElement}
     * @param parentElementDiscoverer  上级元素查找器
     */
    public static <E extends AnnotatedElement> ResolvedHierarchicalElement<E> create(E element, ParentElementDiscoverer parentElementDiscoverer) {
        return new ResolvedHierarchicalElement<>(element, parentElementDiscoverer);
    }

    /**
     * 清空缓存
     */
    public static void clearCaches() {
        RESOLVED_ELEMENT_CACHES.clear();
    }


    ResolvedHierarchicalElement(@NonNull E source, @NonNull ParentElementDiscoverer parentElementDiscoverer) {
        this.source = source;
        this.parentElementDiscoverer = parentElementDiscoverer;
        this.resolvedAnnotations = Arrays.stream(source.getDeclaredAnnotations())
            .map(ResolvedAnnotations::from)
            .toList();
    }

    // region ===== 查找注解 =====

    /**
     * 从层级结构中的所有的元素中查找首个匹配的注解
     *
     * @param annotationType 注解类型
     * @param <A> 注解类型
     * @return 注解
     */
    @Override
    @Nullable
    public <A extends Annotation> A getAnnotation(@NonNull Class<A> annotationType) {
        return hierarchyStream()
            .map(parent -> parent.getDeclaredAnnotation(annotationType))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    /**
     * 获取层级结构中的所有的元素上的全部注解
     *
     * @return 注解列表
     */
    @Override
    public Annotation[] getAnnotations() {
        return hierarchyStream(true)
            .map(ResolvedHierarchicalElement::getDeclaredAnnotations)
            .flatMap(Stream::of)
            .toArray(Annotation[]::new);
    }

    /**
     * 从层级结构中的所有的元素上获取所有指定类型的注解，包括被{@link Repeatable}标记的可重复注解
     *
     * @param annotationType 注解类型
     * @return 注解
     */
    @Override
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
        return hierarchyStream(true)
            .map(parent -> parent.getDeclaredAnnotationsByType(annotationType))
            .filter(ArrayUtils::isNotEmpty)
            .flatMap(Arrays::stream)
            .toArray(len -> ArrayUtils.newInstance(annotationType, len));
    }

    /**
     * 从当前元素上查找首个匹配的注解
     *
     * @param annotationType 注解类型
     * @param <A> 注解类型
     * @return 注解
     */
    @Override
    @Nullable
    public <A extends Annotation> A getDeclaredAnnotation(@NonNull Class<A> annotationType) {
        return resolvedAnnotations.stream()
            .map(ras -> ras.synthesis(annotationType))
            .mapMulti(Optional::ifPresent)
            .findFirst()
            .map(annotationType::cast)
            .orElse(null);
    }

    /**
     * 从当前元素上获取所有注解
     *
     * @return 注解对象
     */
    @Override
    public Annotation[] getDeclaredAnnotations() {
        return resolvedAnnotations.stream()
            .flatMap(ResolvedAnnotations::stream)
            .map(ResolvedAnnotation::<Annotation>synthesis)
            .toArray(Annotation[]::new);
    }

    /**
     * 从当前元素上获取指定所有类型的注解，包括被{@link Repeatable}标记的可重复注解
     *
     * @param annotationType 注解类型
     * @return 注解对象
     */
    @Override
    public <A extends Annotation> A[] getDeclaredAnnotationsByType(Class<A> annotationType) {
        List<A> annotations = resolvedAnnotations.stream()
            .map(ras -> ras.synthesis(annotationType))
            .<A>mapMulti(Optional::ifPresent)
            .collect(Collectors.toList());
        // 若注解类型是可重复注解，则还需要收集其容器注解中的注解
        AnnotationUtils.determineRepeatableContainerType(annotationType).ifPresent(containerType ->
            resolvedAnnotations.stream()
                .map(ras -> ras.synthesis(containerType))
                .mapMulti(Optional::ifPresent)
                .map(container -> AnnotationUtils.getAnnotationFromRepeatableContainer(annotationType, (Annotation) container))
                .flatMap(Arrays::stream)
                .forEach(annotations::add)
        );
        return annotations.toArray(len -> ArrayUtils.newInstance(annotationType, len));
    }

    // endregion

    // region ===== 访问层级结构 =====

    /**
     * 创建一个{@link ResolvedHierarchicalElement}实例
     *
     * @param source 待封装的{@link AnnotatedElement}
     * @param parentElementDiscoverer 上级节点查找器
     * @return {@link ResolvedHierarchicalElement}实例
     */
    @SuppressWarnings("unchecked")
    protected ResolvedHierarchicalElement<E> createElement(AnnotatedElement source, ParentElementDiscoverer parentElementDiscoverer) {
        return (ResolvedHierarchicalElement<E>) RESOLVED_ELEMENT_CACHES.computeIfAbsent(
            source, ele -> new ResolvedHierarchicalElement<>(ele, parentElementDiscoverer)
        );
    }

    /**
     * 获取父级别元素
     *
     * @return 父级别元素
     */
    @NonNull
    public Collection<ResolvedHierarchicalElement<E>> getParents() {
        if (parents == null) {
            synchronized (this) {
                if (parents == null) {
                    var ps = parentElementDiscoverer.resolve(source).stream()
                        .map(e -> createElement(e, parentElementDiscoverer))
                        .toList();
                    parents = CollectionUtils.isNotEmpty(ps) ? ps : Collections.emptyList();
                }
            }
        }
        return parents;
    }


    /**
     * 获取包括当前元素在内，层级结构中的所有元素组成的流
     *
     * @param parallel 是否并行流，全量搜索注解时可使用并行流加速
     * @return 流
     */
    public Stream<ResolvedHierarchicalElement<E>> hierarchyStream(boolean parallel) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(hierarchyIterator(), 0), false);
    }

    /**
     * 获取包括当前元素在内，层级结构中的所有元素组成的串行流
     *
     * @return 流
     */
    public Stream<ResolvedHierarchicalElement<E>> hierarchyStream() {
        return hierarchyStream(false);
    }

    /**
     * 获取包括当前元素在内，层级结构中的所有元素
     *
     * @return 流
     */
    public List<ResolvedHierarchicalElement<E>> hierarchies() {
        return hierarchyStream(false).toList();
    }

    /**
     * 获取层级结构迭代器，用于按广度优先迭代包括当前元素在内，层级结构中所有元素
     *
     * @return 迭代器实例
     * @see HierarchicalAnnotatedElement.HierarchicalAnnotatedElementIterator
     */
    private Iterator<ResolvedHierarchicalElement<E>> hierarchyIterator() {
        return new IteratorImpl<>(this);
    }

    /**
     * 内部迭代器，用于按广度优先迭代包括当前元素在内，层级结构中的所有元素
     *
     * @author huangchengxing
     */
    static class IteratorImpl<E extends AnnotatedElement> implements Iterator<ResolvedHierarchicalElement<E>> {
        private final Set<E> accessed = new HashSet<>();
        private final Deque<ResolvedHierarchicalElement<E>> queue = new LinkedList<>();
        IteratorImpl(ResolvedHierarchicalElement<E> root) {
            queue.add(root);
        }
        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }
        @Override
        public ResolvedHierarchicalElement<E> next() {
            ResolvedHierarchicalElement<E> m = queue.removeFirst();
            accessed.add(m.getSource());
            for (ResolvedHierarchicalElement<E> rhe : m.getParents()) {
                if (!accessed.contains(rhe.getSource())) {
                    queue.add(rhe);
                }
            }
            return m;
        }
    }

    // endregion

    // region ===== 查找上级元素 =====

    /**
     * 上级节点查找器
     *
     * @author huangchengxing
     */
    public interface ParentElementDiscoverer {

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
     * 不查找任何上级元素，默认返回空集合
     */
    static class NoHierarchyElementDiscoverer implements ParentElementDiscoverer {
        @Override
        public @NonNull Collection<? extends AnnotatedElement> resolve(AnnotatedElement element) {
            return Collections.emptyList();
        }
    }

    /**
     * 获取指定注解类上的元注解，忽略jdk的原生注解
     */
    static class MetaAnnotationTypesDiscoverer implements ParentElementDiscoverer {
        @Override
        public @NonNull Collection<? extends AnnotatedElement> resolve(AnnotatedElement element) {
            element = WrappedAnnotatedElement.getRoot(element);
            if (element instanceof Class<?> type && type.isAnnotation()) {
                return Arrays.stream(type.getDeclaredAnnotations())
                    .map(Annotation::annotationType)
                    .filter(AnnotationUtils::isNotJdkMetaAnnotation)
                    .toList();
            }
            throw new Ioc4jException("element must be a type of annotation: [{}]", element);
        }
    }

    /**
     * 获得指定方法声明类的父类与父接口中，能够被重写的方法。<br/>
     * 关于可被重写方法判断标准，参照{@link ReflectUtils#isOverrideableFrom}；
     */
    static class OverrideableMethodsDiscoverer implements ParentElementDiscoverer {
        @Override
        public @NonNull Collection<? extends AnnotatedElement> resolve(AnnotatedElement element) {
            element = WrappedAnnotatedElement.getRoot(element);
            if (element instanceof Method method) {
                return ReflectUtils.getDeclaredSuperClassWithInterface(method.getDeclaringClass()).stream()
                    .map(ReflectUtils::getDeclaredMethods)
                    .flatMap(Arrays::stream)
                    .filter(m -> ReflectUtils.isOverrideableFrom(method, m))
                    .toList();
            }
            throw new Ioc4jException("element must be a method: [{}]", element);
        }
    }

    /**
     * 获取指定类的父类或父接口
     */
    static class SuperclassAndInterfacesDiscoverer implements ParentElementDiscoverer {
        @Override
        public @NonNull Collection<? extends AnnotatedElement> resolve(AnnotatedElement element) {
            element = WrappedAnnotatedElement.getRoot(element);
            if (element instanceof Class<?> type) {
                return ReflectUtils.getDeclaredSuperClassWithInterface(type);
            }
            throw new Ioc4jException("element must be a class: [{}]", element);
        }
    }

    // regend
}
