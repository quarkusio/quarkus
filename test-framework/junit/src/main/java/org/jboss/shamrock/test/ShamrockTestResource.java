package org.jboss.shamrock.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jboss.shamrock.test.ShamrockTestResource.List;

/**
 * Used to define a test resource.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(List.class)
public @interface ShamrockTestResource {

    /**
     * @return The class managing the lifecycle of the test resource.
     */
    Class<? extends ShamrockTestResourceLifecycleManager> value();

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        ShamrockTestResource[] value();
    }
}
