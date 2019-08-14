package io.quarkus.annotation.processor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.regex.Pattern;

public class Constants {
    public static final String PARENT = "<<parent>>";
    public static final String NO_DEFAULT = "<<no default>>";
    public static final String HYPHENATED_ELEMENT_NAME = "<<hyphenated element name>>";

    public static final Pattern JAVA_DOC_CODE_PATTERN = Pattern.compile("\\{@code (.*?)\\}");
    public static final Pattern JAVA_DOC_LINK_PATTERN = Pattern.compile("\\{@link #(.*?)\\}");
    public static final Pattern CONFIG_ROOT_PATTERN = Pattern.compile("^(\\w+)Config(uration)?");
    public static final Pattern PKG_PATTERN = Pattern.compile("^io\\.quarkus\\.(\\w+)\\.?(\\w+)?\\.?(\\w+)?");

    public static final String MEMORY_SIZE_TYPE = "io.quarkus.runtime.configuration.MemorySize";
    public static final String ANNOTATION_BUILD_STEP = "io.quarkus.deployment.annotations.BuildStep";
    public static final String ANNOTATION_CONFIG_GROUP = "io.quarkus.runtime.annotations.ConfigGroup";
    public static final String ANNOTATION_CONFIG_ITEM = "io.quarkus.runtime.annotations.ConfigItem";
    public static final String ANNOTATION_CONFIG_ROOT = "io.quarkus.runtime.annotations.ConfigRoot";
    public static final String ANNOTATION_TEMPLATE = "io.quarkus.runtime.annotations.Template";
    public static final String ANNOTATION_RECORDER = "io.quarkus.runtime.annotations.Recorder";
    public static final String INSTANCE_SYM = "__instance";
    public static final String QUARKUS = "quarkus.";

    public static final Set<String> SUPPOERTED_ANNOTATIONS_TYPES = new HashSet<>();
    public static final Map<String, String> OPTIONAL_NUMBER_TYPES = new HashMap<>();
    public static final String DOCS_SRC_MAIN_ASCIIDOC_GENERATED = "/docs/src/main/asciidoc/generated/";
    public static final String MAVEN_MULTI_MODULE_PROJECT_DIRECTORY = "maven.multiModuleProjectDirectory";
    public static final String SEE_DURATION_NOTE_BELOW = ". _See duration note below_";
    public static final String SEE_MEMORY_SIZE_NOTE_BELOW = ". _See memory size note below_";

    public static final String DURATION_FORMAT_NOTE = "\n[NOTE]\n" +
            "====\n" +
            "The format for durations uses the standard `java.time.Duration` format.\n" +
            "You can learn more about it in the link:https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-[Duration#parse() javadoc].\n"
            +
            "\n" +
            "You can also provide duration values starting with a number.\n" +
            "In this case, if the value consists only of a number, the converter treats the value as seconds.\n" +
            "Otherwise, `PT` is implicitly appended to the value to obtain a standard `java.time.Duration` format.\n" +
            "====\n";

    public static final String MEMORY_SIZE_FORMAT_NOTE = "\n[NOTE]\n" +
            "====\n" +
            "A size configuration option recognises string in this format (shown as a regular expression): `[0-9]+[KkMmGgTtPpEeZzYy]?`.\n"
            +
            "If no suffix is given, assume bytes.\n" +
            "====\n";

    static {
        OPTIONAL_NUMBER_TYPES.put(OptionalLong.class.getName(), Long.class.getName());
        OPTIONAL_NUMBER_TYPES.put(OptionalInt.class.getName(), Integer.class.getName());
        OPTIONAL_NUMBER_TYPES.put(OptionalDouble.class.getName(), Double.class.getName());
        SUPPOERTED_ANNOTATIONS_TYPES.add(ANNOTATION_BUILD_STEP);
        SUPPOERTED_ANNOTATIONS_TYPES.add(ANNOTATION_CONFIG_GROUP);
        SUPPOERTED_ANNOTATIONS_TYPES.add(ANNOTATION_CONFIG_ROOT);
        SUPPOERTED_ANNOTATIONS_TYPES.add(ANNOTATION_TEMPLATE);
        SUPPOERTED_ANNOTATIONS_TYPES.add(ANNOTATION_RECORDER);
    }

}
