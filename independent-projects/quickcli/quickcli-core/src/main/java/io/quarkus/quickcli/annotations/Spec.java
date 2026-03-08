package io.quarkus.quickcli.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code CommandSpec} field for injection. The field will be populated with
 * the command's specification before the command is executed. This allows commands
 * to access their own metadata, print usage, and retrieve exit codes.
 *
 * <p>The annotation processor generates code to inject the spec at build time,
 * so no runtime reflection is needed.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Spec {

    /** The target of the spec injection. */
    Spec.Target value() default Spec.Target.SELF;

    /** Specifies which command spec to inject. */
    enum Target {
        /** Inject the spec of the command that declares this field. */
        SELF,
        /** Inject the spec of the command that uses this mixin (the "mixee"). */
        MIXEE
    }
}
