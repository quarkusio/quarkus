package io.quarkus.aesh.runtime.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

/**
 * Qualifier annotation for commands that should be available in console (interactive shell) mode.
 * <p>
 * When the Aesh extension runs in console mode (using {@link org.aesh.AeshConsoleRunner}),
 * all commands marked with this annotation will be registered in the interactive shell.
 * <p>
 * In auto mode, if multiple commands are annotated with @CliCommand (or @CommandDefinition
 * without @TopCommand/@GroupCommandDefinition), the extension will automatically start
 * in console mode.
 * <p>
 * Example usage:
 *
 * <pre>
 * &#64;CommandDefinition(name = "greet", description = "Greet someone")
 * &#64;CliCommand
 * public class GreetCommand implements Command&lt;CommandInvocation&gt; {
 *     // ...
 * }
 *
 * &#64;CommandDefinition(name = "calc", description = "Calculate")
 * &#64;CliCommand
 * public class CalcCommand implements Command&lt;CommandInvocation&gt; {
 *     // ...
 * }
 * </pre>
 * <p>
 * With multiple @CliCommand classes, the application will start an interactive shell:
 *
 * <pre>
 * [quarkus]$ greet --name John
 * Hello, John!
 * [quarkus]$ calc --a 5 --b 3
 * Result: 8
 * [quarkus]$ exit
 * </pre>
 *
 * @see TopCommand
 * @see io.quarkus.aesh.runtime.AeshMode
 */
@Target({ TYPE, FIELD, PARAMETER, METHOD })
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface CliCommand {
}
