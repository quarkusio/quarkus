package io.quarkus.qute.runtime;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

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
     * <li>{@code *.age} - exclude the property/method {@code age} on any class</li>
     * </ul>
     */
    @ConfigItem
    public Optional<List<String>> typeCheckExcludes;

    /**
     * This regular expression is used to exclude template files from the {@code templates} directory. Excluded templates are
     * neither parsed nor validated during build and are not available at runtime.
     * <p>
     * The matched input is the file path relative from the {@code templates} directory and the
     * {@code /} is used as a path separator.
     * <p>
     * By default, the hidden files are excluded. The name of a hidden file starts with a dot.
     */
    @ConfigItem(defaultValue = "^\\..*|.*\\/\\..*$")
    public Pattern templatePathExclude;

    /**
     * The prefix is used to access the iteration metadata inside a loop section.
     * <p>
     * A valid prefix consists of alphanumeric characters and underscores.
     * Three special constants can be used:
     * <ul>
     * <li>{@code <alias_>} - the alias of an iterated element suffixed with an underscore is used, e.g. {@code item_hasNext}
     * and {@code it_count}</li>
     * <li>{@code <alias?>} - the alias of an iterated element suffixed with a question mark is used, e.g. {@code item?hasNext}
     * and {@code it?count}</li>
     * <li>{@code <none>} - no prefix is used, e.g. {@code hasNext} and {@code count}</li>
     * </ul>
     * By default, the {@code <alias_>} constant is set.
     */
    @ConfigItem(defaultValue = "<alias_>")
    public String iterationMetadataPrefix;

    /**
     * The list of content types for which the {@code '}, {@code "}, {@code <}, {@code >} and {@code &} characters are escaped
     * if a template variant is set.
     */
    @ConfigItem(defaultValue = "text/html,text/xml,application/xml,application/xhtml+xml")
    public List<String> escapeContentTypes;

    /**
     * The default charset of the templates files.
     */
    @ConfigItem(defaultValue = "UTF-8")
    public Charset defaultCharset;

}
