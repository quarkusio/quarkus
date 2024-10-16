package io.quarkus.qute;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides quick and convenient access to the engine instance stored in a static variable. If a specific engine instance is not
 * set via the {@link #setEngine(Engine)} method a default engine is created lazily.
 * <p>
 * Moreover, the convenient {@code fmt()} methods that can be used to format messages easily.
 *
 * <pre>
 * Qute.fmt("Hello {}!", "Quarkus");
 * // => Hello Quarkus!
 *
 * Qute.fmt("Hello {name} {surname ?: 'Default'}!", Map.of("name", "Martin"));
 * // => Hello Martin Default!
 *
 * Qute.fmt("&lt;html&gt;{header}&lt;/html&gt;").contentType("text/html").data("header", "&lt;h1&gt;Header&lt;/h1&gt;").render();
 * // &lt;html&gt;{@literal &lt;}h1{@literal &gt;}Header{@literal &lt;}/h1{@literal &gt;}&lt;/html&gt;
 * // Note that for a "text/html" template the special chars are replaced with html entities by default.
 * </pre>
 *
 * @see #fmt(String)
 * @see #fmt(String, Map)
 * @see #fmt(String, Object...)
 */
public final class Qute {

    /**
     * If needed, a default engine is created lazily.
     * <p>
     * The default engine has in addition to {@link EngineBuilder#addDefaults()}:
     * <ul>
     * <li>{@link ReflectionValueResolver}</li>
     * <li>{@link IndexedArgumentsParserHook}</li>
     * <li>{@link HtmlEscaper} registered for {@code text/html} and {@code text/xml} content types</li>
     * </ul>
     *
     * @return the engine
     * @see #setEngine(Engine)
     */
    public static Engine engine() {
        Engine engine = Qute.engine;
        if (engine == null) {
            synchronized (Qute.class) {
                if (engine == null) {
                    engine = newDefaultEngine();
                    Qute.engine = engine;
                }
            }
        }
        return engine;
    }

    /**
     * Set a specific engine instance.
     * <p>
     * Note that the engine should have a {@link IndexedArgumentsParserHook} registered so that the
     * {@link #fmt(String, Object...)} method works correcly.
     * <p>
     * The cache is always cleared when a new engine is set.
     *
     * @param engine
     * @see #engine()
     */
    public static void setEngine(Engine engine) {
        clearCache();
        Qute.engine = engine;
    }

    /**
     *
     * @param template
     * @param data
     * @return the rendered template
     */
    public static String fmt(String template, Map<String, Object> data) {
        return fmt(template).dataMap(data).render();
    }

    /**
     * The data array is accessibe via the {@code data} key, e.g. {data[0]} is resolved to the first argument.
     * <p>
     * An empty expression <code>{}</code> is a placeholder that is replaced with an index-based array accessor
     * <code>{data[n]}</code> where {@code n} is the index of the placeholder. The first placeholder is replace with
     * <code>{data[0]}</code>, the second with <code>{data[1]}</code>, and so on. For example, <code>"Hello {}!"</code> becomes
     * <code>Hello {data[0]}!</code>.
     *
     * @param template
     * @param data
     * @return the rendered template
     */
    public static String fmt(String template, Object... data) {
        return fmt(template).dataArray(data).render();
    }

    /**
     *
     * @param template
     * @return a new format object
     */
    public static Fmt fmt(String template) {
        return new Fmt(template);
    }

    /**
     * The template cache will be used by default.
     *
     * @see Fmt#cache()
     * @see #clearCache()
     */
    public static void enableCache() {
        cacheByDefault = true;
    }

    /**
     * The template cache will not be used by default.
     *
     * @see Fmt#noCache()
     * @see #clearCache()
     */
    public static void disableCache() {
        cacheByDefault = false;
    }

    /**
     * Clears the template cache.
     */
    public static void clearCache() {
        CACHE.clear();
    }

    /**
     * This construct is not thread-safe.
     */
    public final static class Fmt {

        private final String template;
        private Map<String, Object> attributes;
        private Map<String, Object> dataMap;
        private boolean cache;
        private Variant variant;

        Fmt(String template) {
            this.template = template;
            this.cache = cacheByDefault;
            this.variant = PLAIN_TEXT;
            this.dataMap = new HashMap<>();
        }

        /**
         * Use the template cache, i.e. first attempt to find the parsed template in the cache and if not found then parse the
         * template and store the instance in the cache.
         * <p>
         * Note that caching greatly improves the performance of formatting, albeit increases the memory usage.
         *
         * @return self
         * @see Qute#clearCache()
         */
        public Fmt cache() {
            this.cache = true;
            return this;
        }

        /**
         * Do not use the cache, i.e. always parse the template.
         *
         * @return self
         */
        public Fmt noCache() {
            this.cache = false;
            return this;
        }

        /**
         * Set the template content type.
         *
         * @return self
         * @see Variant
         */
        public Fmt contentType(String contentType) {
            this.variant = Variant.forContentType(contentType);
            return this;
        }

        /**
         * Set the template variant.
         *
         * @return self
         * @see Variant
         */
        public Fmt variant(Variant variant) {
            this.variant = variant;
            return this;
        }

        /**
         * Set the template instance attribute.
         *
         * @return self
         * @see TemplateInstance#setAttribute(String, Object)
         */
        public Fmt attribute(String key, Object value) {
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            this.attributes.put(key, value);
            return this;
        }

        /**
         * The data array is accessibe via the {@code data} key, e.g. {data[0]} is resolved to the first argument.
         * <p>
         * An empty expression <code>{}</code> is a placeholder that is replaced with an index-based array accessor
         * <code>{data[n]}</code> where {@code n} is the index of the placeholder. The first placeholder is replace with
         * <code>{data[0]}</code>, the second with <code>{data[1]}</code>, and so on. For example, <code>"Hello {}!"</code>
         * becomes <code>Hello {data[0]}!</code>.
         *
         * @param data
         * @return self
         */
        public Fmt dataArray(Object... data) {
            dataMap.put("data", data);
            return this;
        }

        public Fmt dataMap(Map<String, Object> data) {
            dataMap.putAll(data);
            return this;
        }

        public Fmt data(String key, Object data) {
            dataMap.put(key, data);
            return this;
        }

        /**
         *
         * @return the rendered template
         */
        public String render() {
            return instance().render();
        }

        /**
         *
         * @return a new template instance
         */
        public TemplateInstance instance() {
            Engine engine = engine();
            Template parsed;
            if (cache) {
                parsed = CACHE.computeIfAbsent(hashKey(template), key -> {
                    String id = newId();
                    return engine.parse(template, variant, id);
                });
            } else {
                parsed = engine.parse(template, variant, newId());
            }
            TemplateInstance instance = parsed.instance();
            if (attributes != null) {
                attributes.forEach(instance::setAttribute);
            }
            dataMap.forEach(instance::data);
            return instance;
        }

        @Override
        public String toString() {
            return render();
        }

    }

    public static class IndexedArgumentsParserHook implements ParserHook {

        private final Pattern p = Pattern.compile("\\{\\}");
        private final String prefix;

        public IndexedArgumentsParserHook() {
            this(TEMPLATE_PREFIX);
        }

        public IndexedArgumentsParserHook(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void beforeParsing(ParserHelper parserHelper) {
            if (prefix != null && !parserHelper.getTemplateId().startsWith(prefix)) {
                return;
            }
            parserHelper.addContentFilter(new Function<String, String>() {

                @Override
                public String apply(String input) {
                    if (!input.contains("{}")) {
                        return input;
                    }
                    // Find all empty expressions and turn them into index-based expressions
                    // e.g. "Hello {} and {}!" turns into "Hello {data.0} and {data.1}!"
                    StringBuilder builder = new StringBuilder();
                    Matcher m = p.matcher(input);
                    int idx = 0;
                    while (m.find()) {
                        m.appendReplacement(builder, "{data." + idx + "}");
                        idx++;
                    }
                    m.appendTail(builder);
                    return builder.toString();
                }
            });
        }

    }

    private static volatile Engine engine;
    private static volatile boolean cacheByDefault;
    private static final Map<Hash, Template> CACHE = new ConcurrentHashMap<>();
    private static final Variant PLAIN_TEXT = Variant.forContentType(Variant.TEXT_PLAIN);
    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);

    /* Internals */

    private static final String TEMPLATE_PREFIX = "Qute$$";

    private static String newId() {
        return TEMPLATE_PREFIX + ID_GENERATOR.incrementAndGet();
    }

    private static Hash hashKey(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return new Hash(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class Hash {

        private final byte[] hash;
        private final int hashCode;

        Hash(byte[] hash) {
            this.hash = hash;
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(hash);
            this.hashCode = result;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            Hash other = (Hash) obj;
            return Arrays.equals(hash, other.hash);
        }

    }

    private static Engine newDefaultEngine() {
        return Engine.builder()
                .addDefaults()
                .addValueResolver(new ReflectionValueResolver())
                .addParserHook(new IndexedArgumentsParserHook())
                .addResultMapper(new HtmlEscaper(ImmutableList.of("text/html", "text/xml")))
                .build();
    }

}
