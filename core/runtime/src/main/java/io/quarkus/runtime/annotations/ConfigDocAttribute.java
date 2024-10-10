package io.quarkus.runtime.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A way to set <a href="https://docs.asciidoctor.org/asciidoc/latest/attributes/attribute-entries/" asciidoc attributes}
 * (variables) at the start of generated documentation.
 * <p>
 * Only taken into account on config roots.
 * <p>
 * Each attribute will be rendered as a single line: {@code :name: value}.
 * <p>
 * Especially useful for injecting content (e.g. link roots) into documentation
 * of config groups reused from another extension.
 * For example a declaration such as {@code @ConfigDocAttribute(name = "myname", value = "myvalue")}
 * can be referenced in Javadoc that uses {@code @asciidoclet}
 * with the attribute reference {@code {myname}}.
 */
@Retention(RUNTIME)
@Target(TYPE)
@Documented
public @interface ConfigDocAttribute {

    /**
     * @return The name of the attribute in generated documentation.
     */
    String name();

    /**
     * @return The value of the attribute in generated documentation.
     */
    String value();

}
