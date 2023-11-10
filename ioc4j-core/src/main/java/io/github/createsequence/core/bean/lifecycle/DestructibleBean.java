package io.github.createsequence.core.bean.lifecycle;

/**
 * 销毁时回调
 *
 * @author huangchengxing
 */
public interface DestructibleBean extends BeanLifecycle {
    
    /**
     * 进行销毁处理
     */
    void destroy();
}
