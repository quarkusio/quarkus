package io.quarkus.qute;

import java.util.function.Function;

/**
 *
 * @see ParserHook
 */
public interface ParserHelper {

    /**
     *
     * @return the template id
     */
    String getTemplateId();

    /**
     * Adds an <em>implicit</em> parameter declaration. This is an alternative approach to <em>explicit</em> parameter
     * declarations used directly in the templates, e.g. <code>{@org.acme.Foo foo}</code>.
     * <p>
     * The type is a fully qualified class name. The package name is optional for JDK types from the {@code java.lang}
     * package. Parameterized types are supported, however wildcards are always ignored - only the upper/lower bound is taken
     * into account. For example, the type info {@code java.util.List<? extends org.acme.Foo>} is recognized as
     * {@code java.util.List<org.acme.Foo> list}. Type variables are not handled in a special way and should never be used.
     *
     * @param name
     * @param type
     */
    void addParameter(String name, String type);

    /**
     * The filter is used before the template contents is parsed.
     *
     * @param filter
     */
    void addContentFilter(Function<String, String> filter);
}
