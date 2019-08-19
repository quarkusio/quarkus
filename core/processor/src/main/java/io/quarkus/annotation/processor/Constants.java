package io.quarkus.annotation.processor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.regex.Pattern;

final public class Constants {
    public static final char DOT = '.';
    public static final String EMPTY = "";
    public static final String DASH = "-";
    public static final String CORE = "core-";
    public static final String ADOC_EXTENSION = ".adoc";
    public static final String DIGIT_OR_LOWERCASE = "^[a-z0-9]+$";

    public static final String PARENT = "<<parent>>";
    public static final String NO_DEFAULT = "<<no default>>";
    public static final String HYPHENATED_ELEMENT_NAME = "<<hyphenated element name>>";

    public static final String COMMON = "common";
    public static final String RUNTIME = "runtime";
    public static final String DEPLOYMENT = "deployment";

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
    public static final String QUARKUS = "quarkus";

    public static final Set<String> SUPPORTED_ANNOTATIONS_TYPES = new HashSet<>();
    public static final Map<String, String> OPTIONAL_NUMBER_TYPES = new HashMap<>();
    public static final String DOCS_SRC_MAIN_ASCIIDOC_GENERATED = "/docs/src/main/asciidoc/generated/";
    public static final Path GENERATED_DOCS_PATH = Paths
            .get(System.getProperties().getProperty("maven.multiModuleProjectDirectory")
                    + Constants.DOCS_SRC_MAIN_ASCIIDOC_GENERATED);
    public static final File GENERATED_DOCS_DIR = GENERATED_DOCS_PATH.toFile();
    public static final File ALL_CR_GENERATED_DOC = GENERATED_DOCS_PATH
            .resolve("all-configuration-roots-generated-doc.properties").toFile();

    public static final String SEE_NOTE_BELOW = " - _see note below_";

    public static final String CONFIG_PHASE_RUNTIME_ILLUSTRATION = "⚙️";
    public static final String CONFIG_PHASE_BUILD_TIME_ILLUSTRATION = "\uD83D\uDCE6";
    public static final String CONFIG_PHASE_LEGEND = String.format(
            "\n %s Configuration property fixed at build time - %s️ Configuration property overridable at runtime \n\n",
            CONFIG_PHASE_BUILD_TIME_ILLUSTRATION, CONFIG_PHASE_RUNTIME_ILLUSTRATION);

    public static final String DURATION_FORMAT_NOTE = "\n[NOTE]\n" +
            ".About the Duration format\n" +
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
            ".About the MemorySize format\n" +
            "====\n" +
            "A size configuration option recognises string in this format (shown as a regular expression): `[0-9]+[KkMmGgTtPpEeZzYy]?`.\n"
            +
            "If no suffix is given, assume bytes.\n" +
            "====\n";

    static {
        OPTIONAL_NUMBER_TYPES.put(OptionalLong.class.getName(), Long.class.getName());
        OPTIONAL_NUMBER_TYPES.put(OptionalInt.class.getName(), Integer.class.getName());
        OPTIONAL_NUMBER_TYPES.put(OptionalDouble.class.getName(), Double.class.getName());
        SUPPORTED_ANNOTATIONS_TYPES.add(ANNOTATION_BUILD_STEP);
        SUPPORTED_ANNOTATIONS_TYPES.add(ANNOTATION_CONFIG_GROUP);
        SUPPORTED_ANNOTATIONS_TYPES.add(ANNOTATION_CONFIG_ROOT);
        SUPPORTED_ANNOTATIONS_TYPES.add(ANNOTATION_TEMPLATE);
        SUPPORTED_ANNOTATIONS_TYPES.add(ANNOTATION_RECORDER);
    }

}
