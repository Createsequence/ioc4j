package io.github.createsequence.core.bean.lifecycle;

/**
 * 初始化时回调
 *
 * @author huangchengxing
 */
public interface InitializableBean extends BeanLifecycle {

    /**
     * 进行初始化处理
     */
    void initialize();
}
