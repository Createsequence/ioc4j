package io.github.createsequence.core.inject;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.AnnotatedElement;

/**
 * 依赖注入元数据解析器
 *
 * @author huangchengxing
 */
public interface InjectionMetadataParser {

    /**
     * 解析{@code element}的注入元数据
     *
     * @param element 被解析的元素
     * @return 解析后的注入元数据
     */
    @Nullable
    InjectionMetadata parse(AnnotatedElement element);
}
