package io.github.createsequence.core.support.annotation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 简单的实现类，用于构建{@link HierarchicalAnnotatedElement}实例
 *
 * @param <E> 元素类型
 * @author huangchengxing
 */
@SuppressWarnings("all")
@Getter
@RequiredArgsConstructor
public class GeneralHierarchicalAnnotatedElement<E extends AnnotatedElement>
    implements HierarchicalAnnotatedElement<E, GeneralHierarchicalAnnotatedElement<E>> {
    
    private final E source;
    private final Function<E, List<E>> parentResolver;

    /**
     * 获取父级别元素
     *
     * @return 父级别元素
     */
    @NonNull
    @Override
    public Collection<GeneralHierarchicalAnnotatedElement<E>> getParents() {
        List<E> ps = parentResolver.apply(getSource());
        return Objects.isNull(ps) ? Collections.emptyList() : ps.stream()
            .map(p -> new GeneralHierarchicalAnnotatedElement<>(p, parentResolver))
            .toList();
    }
}
