package io.quarkus.arc.processor;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.jboss.jandex.DotName;

final class InterceptionProxyInfo {
    private final DotName targetClass;
    private final DotName bindingsSourceClass;
    private BeanInfo pseudoBean;

    /**
     * Creates an interim instance with an unknown target class.
     * Shall be replaced later, once the target class is known.
     */
    InterceptionProxyInfo(DotName bindingsSourceClass) {
        this(DotName.OBJECT_NAME, bindingsSourceClass);
    }

    InterceptionProxyInfo(DotName targetClass, DotName bindingsSourceClass) {
        this.targetClass = Objects.requireNonNull(targetClass);
        this.bindingsSourceClass = bindingsSourceClass;
    }

    void init(BeanInfo pseudoBean) {
        this.pseudoBean = pseudoBean;
    }

    DotName getTargetClass() {
        return targetClass;
    }

    DotName getBindingsSourceClass() {
        return bindingsSourceClass;
    }

    /**
     * Note that this method only returns non-{@code null} value
     * <em>after</em> {@link BeanDeployment#init(Consumer, List)}.
     */
    BeanInfo getPseudoBean() {
        return pseudoBean;
    }
}
