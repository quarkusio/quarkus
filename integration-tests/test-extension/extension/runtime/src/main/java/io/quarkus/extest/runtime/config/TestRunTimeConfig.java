package io.quarkus.extest.runtime.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.annotations.DefaultConverter;

@ConfigRoot(name = "rt", phase = ConfigPhase.RUN_TIME)
public class TestRunTimeConfig {
    /** A map of properties */
    @ConfigItem
    public Map<String, String> stringMap;
    /** A map of property lists */
    @ConfigItem
    public Map<String, List<String>> stringListMap;
    /** A run time object */
    @ConfigItem
    public String rtStringOpt;
    /** A run time object with default value */
    @ConfigItem(defaultValue = "rtStringOptWithDefaultValue")
    public String rtStringOptWithDefault;
    /** A config group with all supported value types */
    @ConfigItem
    public AllValuesConfig allValues;

    /** A map of properties */
    @ConfigItem
    public Map<String, Map<String, String>> leafMap;
    /** A map of property lists */
    @ConfigItem
    public Map<String, Map<String, NestedConfig>> configGroupMap;

    /**
     * Enum object
     */
    @ConfigItem
    public MyEnum myEnum;
    /**
     * Enum list of objects
     */
    @ConfigItem
    public List<MyEnum> myEnums;
    /**
     * Enum optional value
     */
    @ConfigItem
    public Optional<MyEnum> myOptionalEnums;

    /**
     * No hyphenation
     */
    @DefaultConverter
    @ConfigItem
    public Optional<MyEnum> noHyphenateFirstEnum;

    /**
     * No hyphenation
     */
    @DefaultConverter
    @ConfigItem
    public Optional<MyEnum> noHyphenateSecondEnum;

    /**
     * Primitive boolean conversion with {@link ConvertWith}
     */
    @ConfigItem(defaultValue = "NO")
    @ConvertWith(YesNoConverter.class)
    public boolean primitiveBoolean;

    /**
     * Boolean conversion with {@link ConvertWith}
     */
    @ConfigItem(defaultValue = "NO")
    @ConvertWith(YesNoConverter.class)
    public Boolean objectBoolean;

    /**
     * Primitive int conversion with {@link ConvertWith}
     */
    @ConfigItem(defaultValue = "zero")
    @ConvertWith(WholeNumberConverter.class)
    public int primitiveInteger;

    /**
     * Integer conversion with {@link ConvertWith}
     */
    @ConfigItem(defaultValue = "zero")
    @ConvertWith(WholeNumberConverter.class)
    public Integer objectInteger;

    /**
     * List of Integer conversion with {@link ConvertWith}
     */
    @ConfigItem(defaultValue = "one")
    @ConvertWith(WholeNumberConverter.class)
    public List<Integer> oneToNine;

    /**
     * Map of Integer conversion with {@link ConvertWith}
     */
    @ConfigItem
    @ConvertWith(WholeNumberConverter.class)
    public Map<String, Integer> mapOfNumbers;

    public Map<String, Map<String, String>> mapMap;

    @Override
    public String toString() {
        return "TestRunTimeConfig{" +
                "stringMap=" + stringMap +
                ", stringListMap=" + stringListMap +
                ", rtStringOpt='" + rtStringOpt + '\'' +
                ", rtStringOptWithDefault='" + rtStringOptWithDefault + '\'' +
                ", allValues=" + allValues +
                ", myEnum=" + myEnum +
                ", myEnums=" + myEnums +
                ", myOptionalEnums=" + myOptionalEnums +
                ", noHyphenateFirstEnum=" + noHyphenateFirstEnum +
                ", noHyphenateSecondEnum=" + noHyphenateSecondEnum +
                ", primitiveBoolean=" + primitiveBoolean +
                ", objectBoolean=" + objectBoolean +
                ", primitiveInteger=" + primitiveInteger +
                ", objectInteger=" + objectInteger +
                ", oneToNine=" + oneToNine +
                ", mapOfNumbers=" + mapOfNumbers +
                '}';
    }
}
