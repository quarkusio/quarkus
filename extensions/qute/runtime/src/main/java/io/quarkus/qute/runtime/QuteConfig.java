package io.quarkus.qute.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class QuteConfig {

    /**
     * The list of suffixes used when attempting to locate a template file.
     * 
     * By default, `engine.getTemplate("foo")` would result in several lookups: `foo`, `foo.html`, `foo.txt`, etc.
     * 
     * @asciidoclet
     */
    @ConfigItem(defaultValue = "qute.html,qute.txt,html,txt")
    public List<String> suffixes;

    /**
     * The additional map of suffixes to content types. This map is used when working with template variants. By default, the
     * {@link java.net.URLConnection#getFileNameMap()} is used to determine the content type of a template file.
     */
    @ConfigItem
    public Map<String, String> contentTypes;

    /**
     * The list of exclude rules used to intentionally ignore some parts of an expression when performing type-safe validation.
     * <p>
     * An element value must have at least two parts separated by dot. The last part is used to match the property/method name.
     * The prepended parts are used to match the class name. The value {@code *} can be used to match any name.
     * <p>
     * Examples:
     * <ul>
     * <li>{@code org.acme.Foo.name} - exclude the property/method {@code name} on the {@code org.acme.Foo} class</li>
     * <li>{@code org.acme.Foo.*} - exclude any property/method on the {@code org.acme.Foo} class</li>
     * <li>{@code *.age} - exlude the property/method {@code age} on any class</li>
     * </ul>
     */
    @ConfigItem
    public Optional<List<String>> typeCheckExcludes;

}
