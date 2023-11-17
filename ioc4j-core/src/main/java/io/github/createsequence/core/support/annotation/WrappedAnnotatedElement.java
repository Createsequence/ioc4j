package io.github.createsequence.core.support.annotation;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.reflect.AnnotatedElement;

/**
 * 被包装的{@link AnnotatedElement}.
 *
 * @author huangchengxing
 */
public interface WrappedAnnotatedElement<E extends AnnotatedElement> extends AnnotatedElement {

    static AnnotatedElement getRoot(AnnotatedElement element) {
        return (element instanceof WrappedAnnotatedElement<?> wae) ? wae.getRoot() : element;
    }

    /**
     * 获取被包装的元素
     *
     * @return 元素
     */
    @NonNull
    E getSource();

    /**
     * 获取最初被包装的元素
     *
     * @return 最初被包装的元素
     */
    default AnnotatedElement getRoot() {
        AnnotatedElement ele = this;
        while (ele instanceof WrappedAnnotatedElement<?> wae) {
            ele = wae.getSource();
        }
        return ele;
    }
}
