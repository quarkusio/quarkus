package io.quarkus.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers a combination of interfaces to allow a {@link java.lang.reflect.Proxy} to be created
 * for this combination when using native image.
 *
 * Order is important, so the order of the classes must match the order that is passed in when creating the
 * proxy.
 *
 * This annotation is repeatable so multiple proxy definitions can be placed on the same class
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(RegisterProxy.RegisterProxyList.class)
public @interface RegisterProxy {

    /**
     * The interfaces to be registered as part of this proxy definition
     */
    Class[] value();

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface RegisterProxyList {
        RegisterProxy[] value();
    }

}
