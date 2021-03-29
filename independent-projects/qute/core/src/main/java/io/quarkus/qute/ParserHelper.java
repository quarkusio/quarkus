package io.quarkus.qute;

import java.util.function.Function;

public interface ParserHelper {

    /**
     * 
     * @return the template id
     */
    String getTemplateId();

    /**
     * Adds an <em>implicit</em> parameter declaration. This an alternative approach to <em>explicit</em> parameter declarations
     * used directly in the templates, e.g. <code>{@org.acme.Foo foo}</code>.
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
