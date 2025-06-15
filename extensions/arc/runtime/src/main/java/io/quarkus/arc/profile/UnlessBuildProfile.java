package io.quarkus.arc.profile;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When applied to a bean class or producer method (or field), the bean will only be enabled if the Quarkus build time
 * profile does <b>not</b> match the rules of the annotation values. <blockquote>
 *
 * <pre>
 *    Enabled when "dev" profile is <b>not</b> active:
 *
 *    &#064;ApplicationScoped
 *    &#064;UnlessBuildProfile("dev")
 *    public class NotDevBean {
 *    }
 *
 *    Enabled when both "build" and "dev" profiles are <b>not</b> active:
 *
 *    &#064;ApplicationScoped
 *    &#064;UnlessBuildProfile(allOf = {"build", "dev"})
 *    public class NotBuildDevBean {
 *    }
 *
 *    Enabled if either "build" or "dev" profile is <b>not</b> active:
 *
 *    &#064;ApplicationScoped
 *    &#064;UnlessBuildProfile(anyOf = {"build", "dev"})
 *    public class NotBuildDevBean {
 *    }
 *
 *    Enabled when both "build" and "dev" profiles are <b>not</b> active and either "test" or "prod" profile is
 *    <b>not</b> active:
 *
 *    &#064;ApplicationScoped
 *    &#064;UnlessBuildProfile(allOf = {"build", "dev"}, anyOf = {"test", "prod"})
 *    public class NotBuildDevBean {
 *    }
 * </pre>
 *
 * </blockquote>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE, ElementType.FIELD })
public @interface UnlessBuildProfile {
    /**
     * A single profile name to enable a bean if a profile with the same name is <b>not</b> active in Quarkus build time
     * config.
     */
    String value() default "";

    /**
     * Multiple profiles names to enable a bean if all the profile names are <b>not</b> active in Quarkus build time
     * config.
     */
    String[] allOf() default {};

    /**
     * Multiple profiles names to enable a bean if any the profile names is <b>not</b> active in Quarkus build time
     * config.
     */
    String[] anyOf() default {};
}
