package io.quarkus.runtime.annotations;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_TIME;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A marker used in conjunction with {@link io.smallrye.config.ConfigMapping} by Quarkus Extensions to set the
 * Quarkus {@link io.quarkus.runtime.annotations.ConfigPhase} of the mapping. The
 * {@link io.smallrye.config.ConfigMapping#prefix()} must state the full path of the configuration namespace.
 * <p>
 * A Configuration Root is strictly bound by the configuration phase, and attempting to access a Configuration Root
 * from outside its corresponding phase will result in an error. They dictate when its contained keys are read from
 * the configuration, and when they are available to applications.
 * <p>
 * A Configuration Root works exactly as a {@link io.smallrye.config.ConfigMapping}. It can be retrieved
 * programmatically via {@link io.smallrye.config.SmallRyeConfig#getConfigMapping(Class)} and injected via CDI.
 * Additionally, Quarkus will automatically inject a Configuration Root in the following cases:
 * <ul>
 * <li>Methods annotated with <code>@BuildStep</code> and phase {@link io.quarkus.runtime.annotations.ConfigPhase#BUILD_TIME} or
 * {@link io.quarkus.runtime.annotations.ConfigPhase#BUILD_AND_RUN_TIME_FIXED}</li>
 * <li>Recorder constructors and phase {@link io.quarkus.runtime.annotations.ConfigPhase#BUILD_AND_RUN_TIME_FIXED}</li>
 * <li>Recorder constructors and phase {@link io.quarkus.runtime.annotations.ConfigPhase#RUN_TIME} if wrapped in a
 * {@link io.quarkus.runtime.RuntimeValue}</li>
 * </ul>
 * <p>
 * All members of a Configuration root must be documented with a Javadoc.
 * <p>
 * This annotation can only be used on interfaces.
 */
@Retention(RUNTIME)
@Target(TYPE)
@Documented
public @interface ConfigRoot {
    /**
     * Determine the phase of this configuration root.
     *
     * @return the phase
     */
    ConfigPhase phase() default BUILD_TIME;
}
