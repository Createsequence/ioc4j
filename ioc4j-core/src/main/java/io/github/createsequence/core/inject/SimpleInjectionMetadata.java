package io.github.createsequence.core.inject;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 简单的注入元数据
 *
 * @author huangchengxing
 */
@Getter
@RequiredArgsConstructor
public class SimpleInjectionMetadata implements InjectionMetadata {
    private final Object source;
    private final String name;
    private final Class<?> type;
}
