package io.quarkus.quickcli;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import io.quarkus.quickcli.model.BuiltCommandModel;
import io.quarkus.quickcli.model.BuiltCommandModel.FieldKind;
import io.quarkus.quickcli.model.BuiltCommandModel.OptionBinding;
import io.quarkus.quickcli.model.BuiltCommandModel.ParameterBinding;
import io.quarkus.quickcli.model.CommandModelRegistry;
import io.quarkus.quickcli.model.FieldAccessor;

/**
 * Test utility for building and registering command models without reflection
 * or annotation processing. Uses BuiltCommandModel.Builder with lambda field
 * accessors.
 */
public final class TestModelHelper {

    private TestModelHelper() {
    }

    public static BuiltCommandModel.Builder builder(Class<?> cls, Supplier<Object> creator) {
        return BuiltCommandModel.builder(cls, creator);
    }

    public static void register(BuiltCommandModel model) {
        CommandModelRegistry.register(model);
    }

    public static OptionBinding option(String[] names, String description, Class<?> type,
            FieldAccessor accessor) {
        return new OptionBinding(names, description, type, false, "", "", false, "",
                names[names.length - 1], false, false, false, -1,
                FieldKind.SINGLE, null, null, "", "", accessor);
    }

    public static OptionBinding requiredOption(String[] names, String description, Class<?> type,
            FieldAccessor accessor) {
        return new OptionBinding(names, description, type, true, "", "", false, "",
                names[names.length - 1], false, false, false, -1,
                FieldKind.SINGLE, null, null, "", "", accessor);
    }

    public static OptionBinding booleanOption(String[] names, String description,
            FieldAccessor accessor) {
        return new OptionBinding(names, description, boolean.class, false, "", "", false, "",
                names[names.length - 1], false, false, false, -1,
                FieldKind.SINGLE, null, null, "", "", accessor);
    }

    public static OptionBinding negatableOption(String[] names, String description,
            String defaultValue, FieldAccessor accessor) {
        return new OptionBinding(names, description, boolean.class, false, defaultValue, "", false, "",
                names[names.length - 1], false, false, true, -1,
                FieldKind.SINGLE, null, null, "", "", accessor);
    }

    public static OptionBinding listOption(String[] names, String description,
            Class<?> componentType, FieldAccessor accessor) {
        return new OptionBinding(names, description, List.class, false, "", "", false, "",
                names[names.length - 1], false, false, false, -1,
                FieldKind.LIST, componentType, null, "", "", accessor);
    }

    public static OptionBinding setOption(String[] names, String description,
            Class<?> componentType, FieldAccessor accessor) {
        return new OptionBinding(names, description, Set.class, false, "", "", false, "",
                names[names.length - 1], false, false, false, -1,
                FieldKind.SET, componentType, null, "", "", accessor);
    }

    public static OptionBinding arrayOption(String[] names, String description,
            Class<?> componentType, FieldAccessor accessor) {
        return new OptionBinding(names, description, componentType.arrayType(), false, "", "", false, "",
                names[names.length - 1], false, false, false, -1,
                FieldKind.ARRAY, componentType, null, "", "", accessor);
    }

    public static OptionBinding mapOption(String[] names, String description,
            String mapFallbackValue, FieldAccessor accessor) {
        return new OptionBinding(names, description, Map.class, false, "", "", false, "",
                names[names.length - 1], false, false, false, -1,
                FieldKind.MAP, null, null, "", mapFallbackValue, accessor);
    }

    public static OptionBinding optionalOption(String[] names, String description,
            Class<?> innerType, FieldAccessor accessor) {
        return new OptionBinding(names, description, Optional.class, false, "", "", false, "",
                names[names.length - 1], false, false, false, -1,
                FieldKind.OPTIONAL, null, innerType, "", "", accessor);
    }

    public static OptionBinding splitOption(String[] names, String description,
            Class<?> componentType, String split, FieldAccessor accessor) {
        return new OptionBinding(names, description, List.class, false, "", "", false, "",
                names[names.length - 1], false, false, false, -1,
                FieldKind.LIST, componentType, null, split, "", accessor);
    }

    public static OptionBinding hiddenOption(String[] names, String description, Class<?> type,
            FieldAccessor accessor) {
        return new OptionBinding(names, description, type, false, "", "", true, "",
                names[names.length - 1], false, false, false, -1,
                FieldKind.SINGLE, null, null, "", "", accessor);
    }

    public static OptionBinding defaultOption(String[] names, String description, Class<?> type,
            String defaultValue, FieldAccessor accessor) {
        return new OptionBinding(names, description, type, false, defaultValue, "", false, "",
                names[names.length - 1], false, false, false, -1,
                FieldKind.SINGLE, null, null, "", "", accessor);
    }

    public static ParameterBinding parameter(int index, String description, Class<?> type,
            FieldAccessor accessor) {
        return new ParameterBinding(index, description, type, "", "", false, "",
                "param" + index, false, FieldKind.SINGLE, null, accessor, "");
    }

    public static ParameterBinding multiValueParameter(int index, String description,
            Class<?> componentType, FieldAccessor accessor) {
        return new ParameterBinding(index, description, List.class, "", "", false, "",
                "params", true, FieldKind.LIST, componentType, accessor, "");
    }
}
