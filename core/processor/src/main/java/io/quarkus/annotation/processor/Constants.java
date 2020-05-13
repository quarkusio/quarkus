package io.quarkus.annotation.processor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.annotation.processor.generate_doc.ConfigDocItem;

final public class Constants {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static TypeReference<List<ConfigDocItem>> LIST_OF_CONFIG_ITEMS_TYPE_REF = new TypeReference<List<ConfigDocItem>>() {
    };

    public static final char DOT = '.';
    public static final String EMPTY = "";
    public static final String DASH = "-";
    public static final String ADOC_EXTENSION = ".adoc";
    public static final String DIGIT_OR_LOWERCASE = "^[a-z0-9]+$";

    public static final String PARENT = "<<parent>>";
    public static final String NO_DEFAULT = "<<no default>>";
    public static final String HYPHENATED_ELEMENT_NAME = "<<hyphenated element name>>";

    public static final String COMMON = "common";
    public static final String RUNTIME = "runtime";
    public static final String DEPLOYMENT = "deployment";

    public static final Pattern CLASS_NAME_PATTERN = Pattern.compile("^.+[\\.$](\\w+)$");
    public static final Pattern PKG_PATTERN = Pattern.compile("^io\\.quarkus\\.(\\w+)\\.?(\\w+)?\\.?(\\w+)?");

    public static final String INSTANCE_SYM = "__instance";
    public static final String QUARKUS = "quarkus";

    public static final String ANNOTATION_TEMPLATE = "io.quarkus.runtime.annotations.Template";
    public static final String ANNOTATION_RECORDER = "io.quarkus.runtime.annotations.Recorder";

    public static final String MEMORY_SIZE_TYPE = "io.quarkus.runtime.configuration.MemorySize";
    public static final String ANNOTATION_CONFIG_ITEM = "io.quarkus.runtime.annotations.ConfigItem";
    public static final String ANNOTATION_BUILD_STEP = "io.quarkus.deployment.annotations.BuildStep";
    public static final String ANNOTATION_CONFIG_ROOT = "io.quarkus.runtime.annotations.ConfigRoot";
    public static final String ANNOTATION_DEFAULT_CONVERTER = "io.quarkus.runtime.annotations.DefaultConverter";
    public static final String ANNOTATION_CONVERT_WITH = "io.quarkus.runtime.annotations.ConvertWith";
    public static final String ANNOTATION_CONFIG_GROUP = "io.quarkus.runtime.annotations.ConfigGroup";
    public static final String ANNOTATION_CONFIG_DOC_MAP_KEY = "io.quarkus.runtime.annotations.ConfigDocMapKey";
    public static final String ANNOTATION_CONFIG_DOC_SECTION = "io.quarkus.runtime.annotations.ConfigDocSection";

    public static final Set<String> SUPPORTED_ANNOTATIONS_TYPES = new HashSet<>();
    public static final Map<String, String> ALIASED_TYPES = new HashMap<>();
    private static final Properties SYSTEM_PROPERTIES = System.getProperties();

    private static final String DOCS_SRC_MAIN_ASCIIDOC_GENERATED = "/target/asciidoc/generated/config/";
    private static final String DOCS_OUT_DIR = System.getProperty("quarkus.docsOutputDir",
            SYSTEM_PROPERTIES.getProperty("maven.multiModuleProjectDirectory", "."));
    public static final Path GENERATED_DOCS_PATH = Paths.get(DOCS_OUT_DIR + DOCS_SRC_MAIN_ASCIIDOC_GENERATED).toAbsolutePath();
    public static final File GENERATED_DOCS_DIR = GENERATED_DOCS_PATH.toFile();
    public static final Boolean SKIP_DOCS_GENERATION = Boolean.valueOf(SYSTEM_PROPERTIES.getProperty("skipDocs", "false"));

    /**
     * Holds the list of configuration items of each configuration roots.
     */
    public static final File ALL_CR_GENERATED_DOC = GENERATED_DOCS_PATH
            .resolve("all-configuration-roots-generated-doc.properties").toFile();

    /**
     * Holds the list of configuration items of each configuration groups.
     * The items in this list may not have complete information as some depend on the configuration root.
     */
    public static final File ALL_CG_GENERATED_DOC = GENERATED_DOCS_PATH
            .resolve("all-configuration-groups-generated-doc.properties").toFile();

    /**
     * Holds the list of computed file names and the list of configuration roots of this extension
     */
    public static final File EXTENSION_CONFIGURATION_ROOT_LIST = GENERATED_DOCS_PATH
            .resolve("extensions-configuration-roots-list.properties").toFile();

    public static final String DURATION_NOTE_ANCHOR = "duration-note-anchor";
    public static final String MEMORY_SIZE_NOTE_ANCHOR = "memory-size-note-anchor";
    public static final String MORE_INFO_ABOUT_TYPE_FORMAT =
            " link:#%s[icon:question-circle[], title=More information about the %s format]";
    public static final String DURATION_INFORMATION = String.format(Constants.MORE_INFO_ABOUT_TYPE_FORMAT,
            Constants.DURATION_NOTE_ANCHOR, Duration.class.getSimpleName());
    public static final String MEMORY_SIZE_INFORMATION = String.format(Constants.MORE_INFO_ABOUT_TYPE_FORMAT,
            Constants.MEMORY_SIZE_NOTE_ANCHOR, "MemorySize");

    public static final String CONFIG_PHASE_BUILD_TIME_ILLUSTRATION = "icon:lock[title=Fixed at build time]";
    public static final String CONFIG_PHASE_LEGEND = String.format(
            "%n%s Configuration property fixed at build time - All other configuration properties are overridable at runtime",
            CONFIG_PHASE_BUILD_TIME_ILLUSTRATION);

    public static final String DURATION_FORMAT_NOTE = "\n[NOTE]" +
            "\n[[" + DURATION_NOTE_ANCHOR + "]]\n" +
            ".About the Duration format\n" +
            "====\n" +
            "The format for durations uses the standard `java.time.Duration` format.\n" +
            "You can learn more about it in the link:https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-[Duration#parse() javadoc].\n"
            +
            "\n" +
            "You can also provide duration values starting with a number.\n" +
            "In this case, if the value consists only of a number, the converter treats the value as seconds.\n" +
            "Otherwise, `PT` is implicitly prepended to the value to obtain a standard `java.time.Duration` format.\n" +
            "====\n";

    public static final String MEMORY_SIZE_FORMAT_NOTE = "\n[NOTE]" +
            "\n[[" + MEMORY_SIZE_NOTE_ANCHOR + "]]\n" +
            ".About the MemorySize format\n" +
            "====\n" +
            "A size configuration option recognises string in this format (shown as a regular expression): `[0-9]+[KkMmGgTtPpEeZzYy]?`.\n"
            +
            "If no suffix is given, assume bytes.\n" +
            "====\n";

    static {
        ALIASED_TYPES.put(OptionalLong.class.getName(), Long.class.getName());
        ALIASED_TYPES.put(OptionalInt.class.getName(), Integer.class.getName());
        ALIASED_TYPES.put(OptionalDouble.class.getName(), Double.class.getName());
        ALIASED_TYPES.put("java.lang.Class<?>", "class name");
        ALIASED_TYPES.put("java.net.InetSocketAddress", "host:port");
        ALIASED_TYPES.put(Path.class.getName(), "path");
        ALIASED_TYPES.put(String.class.getName(), "string");
        SUPPORTED_ANNOTATIONS_TYPES.add(ANNOTATION_BUILD_STEP);
        SUPPORTED_ANNOTATIONS_TYPES.add(ANNOTATION_CONFIG_GROUP);
        SUPPORTED_ANNOTATIONS_TYPES.add(ANNOTATION_CONFIG_ROOT);
        SUPPORTED_ANNOTATIONS_TYPES.add(ANNOTATION_TEMPLATE);
        SUPPORTED_ANNOTATIONS_TYPES.add(ANNOTATION_RECORDER);
    }

}
