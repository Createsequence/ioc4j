package io.github.createsequence.core.support.annotation;

import io.github.createsequence.core.util.CollectionUtils;
import io.github.createsequence.core.util.ReflectUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * {@link HierarchicalAnnotatedElement}的通用实现
 *
 * @param <E> {@link AnnotatedElement}类型
 * @author huangchengxing
 */
@RequiredArgsConstructor
public class GeneralHierarchicalAnnotatedElement<E extends AnnotatedElement> implements HierarchicalAnnotatedElement<E, GeneralHierarchicalAnnotatedElement<E>> {

    // TODO 更换为 WeakConcurrentHashMap，并提供主动清缓存的API
    private static final Map<AnnotatedElement, GeneralHierarchicalAnnotatedElement<? extends AnnotatedElement>> ELEMENT_CACHES = new ConcurrentHashMap<>();
    private static final Map<AnnotatedElement, GeneralHierarchicalAnnotatedElement<? extends AnnotatedElement>> RESOLVED_ELEMENT_CACHES = new ConcurrentHashMap<>();

    /**
     * 被包装的{@link AnnotatedElement}
     */
    @Getter
    private final E source;
    /**
     * 获取{@link AnnotatedElement}上级元素的方法
     */
    private final Function<E, ? extends Collection<E>> parentResolver;
    /**
     * 当前元素的上级元素，在{@link #getParents()}时才加载
     */
    private volatile Collection<GeneralHierarchicalAnnotatedElement<E>> parents = null;

    /**
     * 清空缓存
     */
    public static void clearCaches() {
        // TODO 由于加载阶段可能会扫描并缓存大量的数据，因此在应用完全启动后需要至少调用一次以主动释放内存
        ELEMENT_CACHES.clear();
        RESOLVED_ELEMENT_CACHES.clear();
    }

    /**
     * 从元素中构建一个{@link GeneralHierarchicalAnnotatedElement}实例。
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
     * @return {@link GeneralHierarchicalAnnotatedElement}实例
     */
    public static GeneralHierarchicalAnnotatedElement<? extends AnnotatedElement> from(AnnotatedElement element) {
        return ELEMENT_CACHES.computeIfAbsent(element, ele -> switch (ele) {
            case Class<?> type -> createElement(
                type, ReflectUtils::getDeclaredSuperClassWithInterface
            );
            case Method method -> createElement(method, m ->
                ReflectUtils.getDeclaredSuperClassWithInterface(m.getDeclaringClass()).stream()
                    .map(ReflectUtils::getDeclaredMethods)
                    .flatMap(Arrays::stream)
                    .filter(sm -> ReflectUtils.isOverrideableFrom(m, sm))
                    .toList()
            );
            default -> createElement(
                element, e -> Collections.emptyList()
            );
        });
    }

    /**
     * 从元素中构建一个{@link GeneralHierarchicalAnnotatedElement}实例
     *
     * @param element 元素
     * @return {@link GeneralHierarchicalAnnotatedElement}实例
     * @see ResolvedAnnotatedElement
     */
    public static GeneralHierarchicalAnnotatedElement<? extends AnnotatedElement> fromElement(AnnotatedElement element) {
        return RESOLVED_ELEMENT_CACHES.computeIfAbsent(element, ele -> switch (ele) {
            case Class<?> type -> createResolvedElement(
                type, ReflectUtils::getDeclaredSuperClassWithInterface
            );
            case Method method -> createResolvedElement(method, m ->
                ReflectUtils.getDeclaredSuperClassWithInterface(m.getDeclaringClass()).stream()
                    .map(ReflectUtils::getDeclaredMethods)
                    .flatMap(Arrays::stream)
                    .filter(sm -> ReflectUtils.isOverrideableFrom(m, sm))
                    .toList()
            );
            default -> createResolvedElement(
                element, e -> Collections.emptyList()
            );
        });
    }

    private static <E extends AnnotatedElement> GeneralHierarchicalAnnotatedElement<E> createElement(
        E element, Function<E, Collection<E>> parentResolver) {
        return new GeneralHierarchicalAnnotatedElement<>(element, parentResolver);
    }

    private static <E extends AnnotatedElement> GeneralHierarchicalAnnotatedElement<ResolvedAnnotatedElement<E>> createResolvedElement(
        E element, Function<E, Collection<E>> parentResolver) {
        ResolvedAnnotatedElement<E> re = ResolvedAnnotatedElement.of(element);
        Function<ResolvedAnnotatedElement<E>, Collection<ResolvedAnnotatedElement<E>>> pr = e ->
            parentResolver.apply(e.getSource()).stream().map(ResolvedAnnotatedElement::of).toList();
        return new GeneralHierarchicalAnnotatedElement<>(re, pr);
    }

    /**
     * 获取父级别元素
     *
     * @return 父级别元素
     */
    @NonNull
    @Override
    public Collection<GeneralHierarchicalAnnotatedElement<E>> getParents() {
        // 获取上级元素可能是一个开销比较大的操作，尤其是涉及ResolvedAnnotatedElement时，
        // 因此每个节点在获取上级元素时针都需要缓存，从而逐级实现整个层级结构中所有元素的懒加载
        if (parents == null) {
            synchronized (this) {
                if (parents == null) {
                    Collection<E> ps = parentResolver.apply(source);
                    parents = CollectionUtils.isEmpty(ps) ? Collections.emptyList() : ps.stream()
                        .map(p -> new GeneralHierarchicalAnnotatedElement<>(p, parentResolver))
                        .toList();
                }
            }
        }
        return parents;
    }
}
