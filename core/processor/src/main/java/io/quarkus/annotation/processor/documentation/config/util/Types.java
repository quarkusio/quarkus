package io.quarkus.annotation.processor.documentation.config.util;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

public final class Types {

    private Types() {
    }

    public static final String ANNOTATION_RECORDER = "io.quarkus.runtime.annotations.Recorder";
    public static final String ANNOTATION_RECORD = "io.quarkus.deployment.annotations.Record";

    public static final String MEMORY_SIZE_TYPE = "io.quarkus.runtime.configuration.MemorySize";
    public static final String ANNOTATION_CONFIG_ITEM = "io.quarkus.runtime.annotations.ConfigItem";
    public static final String ANNOTATION_BUILD_STEP = "io.quarkus.deployment.annotations.BuildStep";
    public static final String ANNOTATION_CONFIG_ROOT = "io.quarkus.runtime.annotations.ConfigRoot";
    public static final String ANNOTATION_CONFIG_MAPPING = "io.smallrye.config.ConfigMapping";
    public static final String ANNOTATION_DEFAULT_CONVERTER = "io.quarkus.runtime.annotations.DefaultConverter";
    public static final String ANNOTATION_CONVERT_WITH = "io.quarkus.runtime.annotations.ConvertWith";
    public static final String ANNOTATION_CONFIG_GROUP = "io.quarkus.runtime.annotations.ConfigGroup";
    public static final String ANNOTATION_CONFIG_DOC_IGNORE = "io.quarkus.runtime.annotations.ConfigDocIgnore";
    public static final String ANNOTATION_CONFIG_DOC_MAP_KEY = "io.quarkus.runtime.annotations.ConfigDocMapKey";
    public static final String ANNOTATION_CONFIG_DOC_SECTION = "io.quarkus.runtime.annotations.ConfigDocSection";
    public static final String ANNOTATION_CONFIG_DOC_ENUM_VALUE = "io.quarkus.runtime.annotations.ConfigDocEnumValue";
    public static final String ANNOTATION_CONFIG_DOC_DEFAULT = "io.quarkus.runtime.annotations.ConfigDocDefault";
    public static final String ANNOTATION_CONFIG_DOC_FILE_NAME = "io.quarkus.runtime.annotations.ConfigDocFilename";
    public static final String ANNOTATION_CONFIG_DOC_PREFIX = "io.quarkus.runtime.annotations.ConfigDocPrefix";
    public static final String ANNOTATION_CONFIG_DOC_ENUM = "io.quarkus.runtime.annotations.ConfigDocEnum";

    public static final String ANNOTATION_CONFIG_WITH_CONVERTER = "io.smallrye.config.WithConverter";
    public static final String ANNOTATION_CONFIG_WITH_NAME = "io.smallrye.config.WithName";
    public static final String ANNOTATION_CONFIG_WITH_PARENT_NAME = "io.smallrye.config.WithParentName";
    public static final String ANNOTATION_CONFIG_WITH_DEFAULT = "io.smallrye.config.WithDefault";
    public static final String ANNOTATION_CONFIG_WITH_UNNAMED_KEY = "io.smallrye.config.WithUnnamedKey";

    public static final Set<String> SUPPORTED_ANNOTATIONS_TYPES = Set.of(ANNOTATION_BUILD_STEP, ANNOTATION_CONFIG_GROUP,
            ANNOTATION_CONFIG_ROOT, ANNOTATION_RECORDER, ANNOTATION_CONFIG_MAPPING);

    static final Map<String, String> ALIASED_TYPES = Map.of(
            OptionalLong.class.getName(), long.class.getName(),
            OptionalInt.class.getName(), int.class.getName(),
            OptionalDouble.class.getName(), double.class.getName(),
            "java.lang.Class<?>", "class name",
            "java.net.InetSocketAddress", "host:port",
            Path.class.getName(), "path",
            String.class.getName(), "string");

    static final Map<String, String> PRIMITIVE_DEFAULT_VALUES = new HashMap<>();

    static final Map<String, String> PRIMITIVE_WRAPPERS = new HashMap<>();

    static {
        PRIMITIVE_DEFAULT_VALUES.put("int", "0");
        PRIMITIVE_DEFAULT_VALUES.put("byte", "0");
        PRIMITIVE_DEFAULT_VALUES.put("char", "");
        PRIMITIVE_DEFAULT_VALUES.put("short", "0");
        PRIMITIVE_DEFAULT_VALUES.put("long", "0l");
        PRIMITIVE_DEFAULT_VALUES.put("float", "0f");
        PRIMITIVE_DEFAULT_VALUES.put("double", "0d");
        PRIMITIVE_DEFAULT_VALUES.put("boolean", "false");

        PRIMITIVE_WRAPPERS.put("java.lang.Character", "char");
        PRIMITIVE_WRAPPERS.put("java.lang.Boolean", "boolean");
        PRIMITIVE_WRAPPERS.put("java.lang.Byte", "byte");
        PRIMITIVE_WRAPPERS.put("java.lang.Short", "short");
        PRIMITIVE_WRAPPERS.put("java.lang.Integer", "int");
        PRIMITIVE_WRAPPERS.put("java.lang.Long", "long");
        PRIMITIVE_WRAPPERS.put("java.lang.Float", "float");
        PRIMITIVE_WRAPPERS.put("java.lang.Double", "double");
    }
}
