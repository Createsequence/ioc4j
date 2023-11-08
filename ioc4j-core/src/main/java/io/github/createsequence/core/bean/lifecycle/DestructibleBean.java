package io.github.createsequence.core.bean.lifecycle;

/**
 * @author huangchengxing
 */
public interface DestructibleBean extends BeanLifecycle {
    
    /**
     * 进行销毁处理
     */
    void destroy();
}
