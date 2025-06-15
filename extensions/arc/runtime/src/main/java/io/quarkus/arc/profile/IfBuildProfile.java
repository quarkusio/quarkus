package io.quarkus.arc.profile;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When applied to a bean class or producer method (or field), the bean will only be enabled if the Quarkus build time
 * profile matches the rules of the annotation values. <blockquote>
 *
 * <pre>
 *    Enabled when "dev" profile is active:
 *
 *    &#064;ApplicationScoped
 *    &#064;IfBuildProfile("dev")
 *    public class DevBean {
 *    }
 *
 *    Enabled when both "build" and "dev" profiles are active:
 *
 *    &#064;ApplicationScoped
 *    &#064;IfBuildProfile(allOf = {"build", "dev"})
 *    public class BuildDevBean {
 *    }
 *
 *    Enabled if either "build" or "dev" profile is active:
 *
 *    &#064;ApplicationScoped
 *    &#064;IfBuildProfile(anyOf = {"build", "dev"})
 *    public class BuildDevBean {
 *    }
 *
 *    Enabled when both "build" and "dev" profiles are active and either "test" or "prod" profile is active:
 *
 *    &#064;ApplicationScoped
 *    &#064;IfBuildProfile(allOf = {"build", "dev"}, anyOf = {"test", "prod"})
 *    public class BuildDevBean {
 *    }
 * </pre>
 *
 * </blockquote>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE, ElementType.FIELD })
public @interface IfBuildProfile {
    /**
     * A single profile name to enable a bean if a profile with the same name is active in Quarkus build time config.
     */
    String value() default "";

    /**
     * Multiple profiles names to enable a bean if all the profile names are active in Quarkus build time config.
     */
    String[] allOf() default {};

    /**
     * Multiple profiles names to enable a bean if any the profile names is active in Quarkus build time config.
     */
    String[] anyOf() default {};
}
