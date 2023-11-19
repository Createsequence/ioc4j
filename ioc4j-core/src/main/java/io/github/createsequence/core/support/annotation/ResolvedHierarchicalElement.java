package io.github.createsequence.core.support.annotation;

import io.github.createsequence.core.exception.Ioc4jException;
import io.github.createsequence.core.util.AnnotationUtils;
import io.github.createsequence.core.util.ArrayUtils;
import io.github.createsequence.core.util.CollectionUtils;
import io.github.createsequence.core.util.ReflectUtils;
import io.github.createsequence.core.util.Streamable;
import lombok.ToString;
import lombok.experimental.Delegate;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
 *         关于可被重写方法判断标准，参照{@link ReflectUtils#isOverrideableFrom}与{@link OverrideableMethodsDiscoverer}
 *     </li>
 *     <li>若是其他类型，则默认其<i>不具备层级结构</i>，{@link #getParents}返回空集合，{@link #stream}返回仅包含本身的流；</li>
 * </ul>
 *
 * <p><strong>注解增强</strong><br />
 * 通过实例获得的所有注解皆支持{@link ResolvedAnnotation}的增强机制，
 * 比如基于{@link AliasFor}的属性别名和对元注解的属性覆盖。
 *
 * <p><strong>注解查找</strong><br />
 * 你可以通过下述API访问层级结构中的各种元素和注解：
 * <ul>
 *     <li>{@link #hierarchyStream()}/{@link #hierarchies()}可快速遍历包括当前元素在内，其层级结构中所有{@link AnnotatedElement}；</li>
 *     <li>{@link #stream()}可快速遍历包括当前元素在内，其层级结构中所有{@link AnnotatedElement}上直接存在的注解及其元注解；</li>
 *     <li>{@code getXXX}：可用于访问包括当前元素在内，其层级结构中所有的注解及元注解；</li>
 *     <li>{@code getDeclaredXXX}：可用于访问包括当前元素上的注解及元注解；</li>
 * </ul>
 *
 * <p><strong>缓存</strong><br />
 * 基于{@link #from}工厂方法创建的所有类型{@link ResolvedHierarchicalElement}均会被缓存，
 * 缓存的加载是渐进式的，比如若基于{@link Class}创建一个实例，
 * 那么当未访问其父类或父接口时，它们对应的缓存并不会被加载。<br/>
 * 不存在强引用的缓存会在下一次GC时被回收，不过也可以通过{@link #clearCaches}主动清空。
 *
 * @author huangchengxing
 * @see ResolvedAnnotation
 */
@ToString(onlyExplicitlyIncluded = true)
public class ResolvedHierarchicalElement<E extends AnnotatedElement>
    extends AbstractHierarchicalElement<E, ResolvedHierarchicalElement<E>> implements AnnotatedElement, Streamable<ResolvedAnnotations> {

    // TODO 更换为 WeakConcurrentHashMap
    private static final Map<AnnotatedElement, ResolvedHierarchicalElement<AnnotatedElement>> RESOLVED_ELEMENT_CACHES = new ConcurrentHashMap<>();

    /**
     * 在元素上直接存在的注解
     */
    @Delegate(types = Iterable.class)
    private final List<ResolvedAnnotations> resolvedAnnotations;

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
        element = (element instanceof ResolvedHierarchicalElement<?> rhe) ? (E) rhe.getRoot() : element;
        return (ResolvedHierarchicalElement<E>) RESOLVED_ELEMENT_CACHES.computeIfAbsent(element, ele -> switch (ele) {
            case Class<?> type && type.isAnnotation() -> new ResolvedHierarchicalElement<>(type, MetaAnnotationTypesDiscoverer.INSTANCE);
            case Class<?> type -> new ResolvedHierarchicalElement<>(type, SuperclassAndInterfacesDiscoverer.INSTANCE);
            case Method method -> new ResolvedHierarchicalElement<>(method, OverrideableMethodsDiscoverer.INSTANCE);
            default -> new ResolvedHierarchicalElement<>(ele, NoHierarchyElementDiscoverer.INSTANCE);
        });
    }

    /**
     * 创建一个{@link ResolvedHierarchicalElement}实例
     *
     * @param element 待包装的{@link AnnotatedElement}
     * @param parentElementDiscoverer  上级元素查找器
     */
    public static <E extends AnnotatedElement> ResolvedHierarchicalElement<E> create(
        E element, ParentElementDiscoverer<E> parentElementDiscoverer) {
        return new ResolvedHierarchicalElement<>(element, parentElementDiscoverer);
    }

    /**
     * 清空缓存
     */
    public static void clearCaches() {
        RESOLVED_ELEMENT_CACHES.clear();
    }

    /**
     * 创建一个{@link ResolvedHierarchicalElement}实例
     *
     * @param element 待包装的{@link AnnotatedElement}
     * @param parentElementDiscoverer  上级元素查找器
     */
    ResolvedHierarchicalElement(@NonNull E source, @NonNull ParentElementDiscoverer<? super E> parentElementDiscoverer) {
        super(source, parentElementDiscoverer);
        this.resolvedAnnotations = Arrays.stream(source.getDeclaredAnnotations())
            .map(ResolvedAnnotations::from)
            .toList();
    }

    /**
     * 创建一个{@link ResolvedHierarchicalElement}实例
     *
     * @param source 待封装的{@link AnnotatedElement}
     * @return {@link ResolvedHierarchicalElement}实例
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected ResolvedHierarchicalElement<E> createElement(E source) {
        return (ResolvedHierarchicalElement<E>)RESOLVED_ELEMENT_CACHES.computeIfAbsent(
            source, ele -> new ResolvedHierarchicalElement(ele, parentElementDiscoverer)
        );
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
            .map(ele -> ele.getDeclaredAnnotation(annotationType))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    /**
     * 检查该注解是否在层级结构中存在
     *
     * @param annotationType 注解类型
     * @return 是否
     */
    @Override
    public boolean isAnnotationPresent(@NonNull Class<? extends Annotation> annotationType) {
        return hierarchyStream(true)
            .map(ele -> ele.getDeclaredAnnotation(annotationType))
            .anyMatch(Objects::nonNull);
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
            .map(ele -> ele.getDeclaredAnnotationsByType(annotationType))
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
     * 检查该注解是否在层级结构中存在
     *
     * @param annotationType 注解类型
     * @return 是否
     */
    public boolean isDeclaredAnnotationPresent(@NonNull Class<? extends Annotation> annotationType) {
        return Objects.nonNull(getDeclaredAnnotation(annotationType));
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

    /**
     * 不查找任何上级元素，默认返回空集合
     */
    private static class NoHierarchyElementDiscoverer implements ParentElementDiscoverer<AnnotatedElement> {
        static final NoHierarchyElementDiscoverer INSTANCE = new NoHierarchyElementDiscoverer();
        @NonNull
        @Override
        public Collection<? extends AnnotatedElement> get(@NonNull AnnotatedElement element) {
            return Collections.emptyList();
        }
    }

    /**
     * 获取指定注解类上的元注解，忽略jdk的原生注解
     */
    private static class MetaAnnotationTypesDiscoverer implements ParentElementDiscoverer<AnnotatedElement> {
        static final MetaAnnotationTypesDiscoverer INSTANCE = new MetaAnnotationTypesDiscoverer();
        @NonNull
        @Override
        public Collection<Class<? extends Annotation>> get(@NonNull AnnotatedElement element) {
            element = WrappedAnnotatedElement.getRoot(element);
            if (element instanceof Class<?> type && type.isAnnotation()) {
                return Arrays.stream(type.getDeclaredAnnotations())
                    .map(Annotation::annotationType)
                    .filter(AnnotationUtils::isNotJdkMetaAnnotation)
                    .collect(Collectors.toList());
            }
            throw new Ioc4jException("element must be a type of annotation: [{}]", element);
        }
    }

    /**
     * 获得指定方法声明类的父类与父接口中，能够被重写的方法。<br/>
     * 关于可被重写方法判断标准，参照{@link ReflectUtils#isOverrideableFrom}；
     */
    private static class OverrideableMethodsDiscoverer implements ParentElementDiscoverer<AnnotatedElement> {
        static final OverrideableMethodsDiscoverer INSTANCE = new OverrideableMethodsDiscoverer();
        @NonNull
        @Override
        public Collection<Method> get(@NonNull AnnotatedElement element) {
            element = WrappedAnnotatedElement.getRoot(element);
            if (element instanceof Method method) {
                Set<Class<?>> accessed = new HashSet<>();
                Deque<Class<?>> typeQueue = new LinkedList<>(ReflectUtils.getDeclaredSuperClassWithInterface(method.getDeclaringClass()));
                List<Method> recentParents = new ArrayList<>();

                // 由于方法可能重写非直接父类或接口，因此直接的上级节点需要通过递归找到。
                // 比如当存在 interface a -> interface b -> class c 的继承关系时，c 中的方法可能来自 interface a
                // 但是，当存在较为复杂的继承树，且相同的方法在不同的上级类中重复出现时，
                // 就需要对每一个分支进行独立的搜索，直到找到首个匹配的方法为止
                while (!typeQueue.isEmpty()) {
                    Class<?> type = typeQueue.removeFirst();
                    accessed.add(type);

                    // 检查类中是否有可重写的方法，若找到则结束当前分支的搜索
                    List<Method> methods = Stream.of(ReflectUtils.getDeclaredMethods(type))
                        .filter(m -> ReflectUtils.isOverrideableFrom(method, m))
                        .toList();
                    if (CollectionUtils.isNotEmpty(methods)) {
                        recentParents.addAll(methods);
                        continue;
                    }

                    Set<Class<?>> declaredSuperClassWithInterface = ReflectUtils.getDeclaredSuperClassWithInterface(type);
                    declaredSuperClassWithInterface.remove(Object.class);
                    declaredSuperClassWithInterface.removeAll(accessed);
                    CollectionUtils.addAll(typeQueue, declaredSuperClassWithInterface);
                }
                return recentParents;
            }
            throw new Ioc4jException("element must be a method: [{}]", element);
        }
    }

    /**
     * 获取指定类的父类或父接口
     */
    private static class SuperclassAndInterfacesDiscoverer implements ParentElementDiscoverer<AnnotatedElement> {
        static final SuperclassAndInterfacesDiscoverer INSTANCE = new SuperclassAndInterfacesDiscoverer();
        @NonNull
        @Override
        public Collection<Class<?>> get(@NonNull AnnotatedElement element) {
            element = WrappedAnnotatedElement.getRoot(element);
            if (element instanceof Class<?> type) {
                return ReflectUtils.getDeclaredSuperClassWithInterface(type);
            }
            throw new Ioc4jException("element must be a class: [{}]", element);
        }
    }
}
