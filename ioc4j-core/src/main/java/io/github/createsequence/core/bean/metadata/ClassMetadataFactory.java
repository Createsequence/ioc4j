package io.github.createsequence.core.bean.metadata;

/**
 * 类型元数据工厂
 *
 * @author huangchengxing
 * @see GeneralClassMetadataFactory
 */
public interface ClassMetadataFactory {

    /**
     * 获取类型对应的元数据
     *
     * @param type 类型
     * @return 元数据
     */
    ClassMetadata resolve(Class<?> type);
}
