module ioc4j.core {
    exports io.github.createsequence.core.support;
    exports io.github.createsequence.core.support.annotation;
    exports io.github.createsequence.core.bean;
    exports io.github.createsequence.core.bean.scope;
    exports io.github.createsequence.core.bean.metadata;
    exports io.github.createsequence.core.util;
    exports io.github.createsequence.core.exception;

    requires lombok;
    requires org.checkerframework.checker.qual;
    requires jakarta.annotation;
    requires javax.inject;
}