package io.quarkus.runtime.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.inject.Qualifier;

/**
 * A qualifier that can be used to inject the command line arguments.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandLineArguments {

}
