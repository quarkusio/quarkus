package io.quarkus.arc.lookup;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.inject.Instance;

/**
 * Indicates that a bean should only be obtained by programmatic lookup if the property does not match the provided value.
 * <p>
 * This annotation is repeatable. A bean will be included if all of the conditions defined by the {@link LookupUnlessProperty}
 * and {@link LookupIfProperty} annotations are satisifed.
 * 
 * <pre>
 * <code>
 *  interface Service {
 *     String name();
 *  }
 *  
 *  {@literal @LookupUnlessProperty(name = "service.foo.disabled", stringValue = "true")}
 *  {@literal @ApplicationScoped}
 *  class ServiceFoo implements Service {
 *  
 *     public String name() {
 *        return "foo";
 *     }
 *  }
 *  
 *  {@literal @ApplicationScoped}
 *  class ServiceBar {
 *  
 *     public String name() {
 *        return "bar";
 *     }
 *  }
 *  
 *  {@literal @ApplicationScoped}
 *  class Client {
 *  
 *     {@literal @Inject}
 *     Instance&lt;Service&gt; service;
 *     
 *     void printServiceName() {
 *        // This would print "bar" if the property of name "service.foo.disabled" is set to true
 *        // Note that service.get() would normally result in AmbiguousResolutionException
 *        System.out.println(service.get().name());
 *     }
 *  }
 *  </code>
 * </pre>
 * 
 * @see Instance
 */
@Repeatable(LookupUnlessProperty.List.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE, ElementType.FIELD })
public @interface LookupUnlessProperty {

    /**
     * Name of the runtime time property to check
     */
    String name();

    /**
     * Expected {@code String} value of the runtime time property (specified by {@code name}) if the bean should be skipped at
     * runtime.
     */
    String stringValue();

    /**
     * Determines if the bean should be suppressed when the property name specified by {@code name} has not been specified at
     * all
     */
    boolean lookupIfMissing() default false;

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.TYPE, ElementType.FIELD })
    @interface List {

        LookupUnlessProperty[] value();

    }
}
