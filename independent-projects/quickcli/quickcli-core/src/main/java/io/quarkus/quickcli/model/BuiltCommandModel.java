package io.quarkus.quickcli.model;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import io.quarkus.quickcli.CommandSpec;
import io.quarkus.quickcli.Factory;
import io.quarkus.quickcli.VersionProvider;
import io.quarkus.quickcli.OptionSpec;
import io.quarkus.quickcli.ParameterSpec;
import io.quarkus.quickcli.ParseResult;
import io.quarkus.quickcli.ScopeType;
import io.quarkus.quickcli.TypeConverter;

/**
 * Data-driven {@link CommandModel} implementation. Instead of generating a full
 * model class per command, metadata is collected at Quarkus build time (via Jandex)
 * and field accessors are generated with Gizmo. This class holds the metadata and
 * delegates field writes to the generated accessors.
 */
public final class BuiltCommandModel implements CommandModel {

    private final Class<?> commandClass;
    private final Supplier<Object> instanceCreator;

    // CommandSpec metadata
    private final String name;
    private final String[] description;
    private final String[] version;
    private final boolean mixinStandardHelpOptions;
    private final String[] header;
    private final String[] footer;
    private final ScopeType scope;
    private final String[] aliases;
    private final boolean hasUnmatchedField;
    private final boolean hasSpecField;
    private final Class<? extends VersionProvider> versionProviderClass;
    private final List<List<String>> exclusiveGroups;

    // UsageMessage customization
    private final String commandListHeading;
    private final String synopsisHeading;
    private final String optionListHeading;
    private final String headerHeading;
    private final String parameterListHeading;
    private final boolean showDefaultValues;

    // Bindings
    private final List<OptionBinding> optionBindings;
    private final List<ParameterBinding> parameterBindings;
    private final List<Class<?>> subcommandClasses;
    private final List<MixinBinding> mixinBindings;
    private final List<ArgGroupBinding> argGroupBindings;
    private final FieldAccessor parentCommandAccessor;
    private final FieldAccessor unmatchedAccessor;
    private final FieldAccessor specAccessor;
    private final List<FieldAccessor> mixeeSpecAccessors;

    private BuiltCommandModel(Builder builder) {
        this.commandClass = builder.commandClass;
        this.instanceCreator = builder.instanceCreator;
        this.name = builder.name;
        this.description = builder.description;
        this.version = builder.version;
        this.mixinStandardHelpOptions = builder.mixinStandardHelpOptions;
        this.header = builder.header;
        this.footer = builder.footer;
        this.scope = builder.scope;
        this.aliases = builder.aliases;
        this.hasUnmatchedField = builder.hasUnmatchedField;
        this.hasSpecField = builder.hasSpecField;
        this.versionProviderClass = builder.versionProviderClass;
        this.exclusiveGroups = builder.exclusiveGroups;
        this.commandListHeading = builder.commandListHeading;
        this.synopsisHeading = builder.synopsisHeading;
        this.optionListHeading = builder.optionListHeading;
        this.headerHeading = builder.headerHeading;
        this.parameterListHeading = builder.parameterListHeading;
        this.showDefaultValues = builder.showDefaultValues;
        this.optionBindings = List.copyOf(builder.optionBindings);
        this.parameterBindings = List.copyOf(builder.parameterBindings);
        this.subcommandClasses = List.copyOf(builder.subcommandClasses);
        this.mixinBindings = List.copyOf(builder.mixinBindings);
        this.argGroupBindings = List.copyOf(builder.argGroupBindings);
        this.parentCommandAccessor = builder.parentCommandAccessor;
        this.unmatchedAccessor = builder.unmatchedAccessor;
        this.specAccessor = builder.specAccessor;
        this.mixeeSpecAccessors = List.copyOf(builder.mixeeSpecAccessors);
    }

    @Override
    public Class<?> commandClass() {
        return commandClass;
    }

    @Override
    public Object createInstance() {
        return instanceCreator.get();
    }

    @Override
    public CommandSpec buildSpec() {
        CommandSpec spec = new CommandSpec(name, description, version,
                mixinStandardHelpOptions, header, footer, commandClass);
        spec.setScope(scope);
        spec.setAliases(aliases);
        spec.setHasUnmatchedField(hasUnmatchedField);
        spec.setHasSpecField(hasSpecField);
        if (versionProviderClass != null
                && versionProviderClass != VersionProvider.NoVersionProvider.class) {
            spec.setVersionProviderClass(versionProviderClass);
        }

        // Configure usage message
        var usage = spec.usageMessage();
        usage.commandListHeading(commandListHeading);
        usage.synopsisHeading(synopsisHeading);
        usage.optionListHeading(optionListHeading);
        usage.headerHeading(headerHeading);
        usage.parameterListHeading(parameterListHeading);
        usage.showDefaultValues(showDefaultValues);

        for (OptionBinding ob : optionBindings) {
            // For Optional<Boolean> fields, use the inner type for boolean detection
            Class<?> effectiveType = ob.fieldKind == FieldKind.OPTIONAL && ob.optionalInnerType != null
                    ? ob.optionalInnerType : ob.type;
            spec.addOption(new OptionSpec(
                    ob.names, ob.description, effectiveType, ob.required,
                    ob.defaultValue, ob.arity, ob.hidden, ob.paramLabel,
                    ob.fieldName, ob.versionHelp, ob.usageHelp, ob.negatable, ob.order));
        }
        for (ParameterBinding pb : parameterBindings) {
            spec.addParameter(new ParameterSpec(
                    pb.index, pb.description, pb.type, pb.defaultValue,
                    pb.arity, pb.hidden, pb.paramLabel, pb.fieldName, pb.isMultiValue));
        }
        for (Class<?> subCls : subcommandClasses) {
            CommandModel subModel = CommandModelRegistry.getModel(subCls);
            if (subModel != null) {
                CommandSpec subSpec = subModel.buildSpec();
                spec.addSubcommand(subSpec.name(), subSpec);
                for (String alias : subSpec.aliases()) {
                    spec.addSubcommand(alias, subSpec);
                }
            }
        }
        for (List<String> group : exclusiveGroups) {
            spec.addExclusiveGroup(group);
        }
        return spec;
    }

    @Override
    public void applyValues(Object instance, ParseResult result) {
        for (OptionBinding ob : optionBindings) {
            applyOptionValue(instance, result, ob);
        }
        for (ParameterBinding pb : parameterBindings) {
            applyParameterValue(instance, result, pb);
        }
    }

    private void applyOptionValue(Object instance, ParseResult result, OptionBinding ob) {
        switch (ob.fieldKind) {
            case SINGLE -> applySingleOption(instance, result, ob);
            case LIST -> applyListOption(instance, result, ob);
            case SET -> applySetOption(instance, result, ob);
            case ARRAY -> applyArrayOption(instance, result, ob);
            case MAP -> applyMapOption(instance, result, ob);
            case OPTIONAL -> applyOptionalOption(instance, result, ob);
        }
    }

    private void applySingleOption(Object instance, ParseResult result, OptionBinding ob) {
        String val = resolveOptionValue(result, ob.names);
        if (val == null && ob.defaultValue != null && !ob.defaultValue.isEmpty()) {
            val = ob.defaultValue;
        }
        if (val != null) {
            ob.accessor.set(instance, TypeConverter.convert(val, ob.type));
        }
    }

    private void applyListOption(Object instance, ParseResult result, OptionBinding ob) {
        List<String> vals = collectOptionValues(result, ob);
        if (!vals.isEmpty()) {
            List<Object> converted = new ArrayList<>();
            for (String v : vals) {
                converted.add(TypeConverter.convert(v, ob.componentType));
            }
            ob.accessor.set(instance, converted);
        }
    }

    private void applySetOption(Object instance, ParseResult result, OptionBinding ob) {
        List<String> vals = collectOptionValues(result, ob);
        if (!vals.isEmpty()) {
            var converted = new HashSet<>();
            for (String v : vals) {
                converted.add(TypeConverter.convert(v, ob.componentType));
            }
            ob.accessor.set(instance, converted);
        }
    }

    private void applyArrayOption(Object instance, ParseResult result, OptionBinding ob) {
        List<String> vals = collectOptionValues(result, ob);
        if (!vals.isEmpty()) {
            Object arr = Array.newInstance(ob.componentType, vals.size());
            for (int i = 0; i < vals.size(); i++) {
                Array.set(arr, i, TypeConverter.convert(vals.get(i), ob.componentType));
            }
            ob.accessor.set(instance, arr);
        }
    }

    private void applyMapOption(Object instance, ParseResult result, OptionBinding ob) {
        List<String> vals = collectOptionValues(result, ob);
        if (!vals.isEmpty()) {
            Map<String, String> map = new LinkedHashMap<>();
            for (String s : vals) {
                int eq = s.indexOf('=');
                if (eq >= 0) {
                    map.put(s.substring(0, eq), s.substring(eq + 1));
                } else {
                    map.put(s, ob.mapFallbackValue != null ? ob.mapFallbackValue : "");
                }
            }
            ob.accessor.set(instance, map);
        }
    }

    private void applyOptionalOption(Object instance, ParseResult result, OptionBinding ob) {
        ob.accessor.set(instance, Optional.empty());
        String val = resolveOptionValue(result, ob.names);
        if (val != null) {
            ob.accessor.set(instance, Optional.of(
                    TypeConverter.convert(val, ob.optionalInnerType)));
        }
    }

    private void applyParameterValue(Object instance, ParseResult result, ParameterBinding pb) {
        if (pb.isMultiValue) {
            List<String> allPositional = result.positionalValues();
            if (!allPositional.isEmpty()) {
                int startIndex = pb.index;
                if (startIndex < allPositional.size()) {
                    List<String> rawValues = allPositional.subList(startIndex, allPositional.size());
                    // Apply split if configured
                    if (pb.split != null && !pb.split.isEmpty()) {
                        List<String> splitValues = new ArrayList<>();
                        for (String v : rawValues) {
                            Collections.addAll(splitValues, v.split(pb.split));
                        }
                        rawValues = splitValues;
                    }
                    if (pb.fieldKind == FieldKind.SET) {
                        var converted = new HashSet<>();
                        for (String v : rawValues) {
                            converted.add(TypeConverter.convert(v, pb.componentType));
                        }
                        pb.accessor.set(instance, converted);
                    } else if (pb.fieldKind == FieldKind.ARRAY) {
                        Object arr = Array.newInstance(pb.componentType, rawValues.size());
                        for (int i = 0; i < rawValues.size(); i++) {
                            Array.set(arr, i, TypeConverter.convert(rawValues.get(i), pb.componentType));
                        }
                        pb.accessor.set(instance, arr);
                    } else {
                        List<Object> converted = new ArrayList<>();
                        for (String v : rawValues) {
                            converted.add(TypeConverter.convert(v, pb.componentType));
                        }
                        pb.accessor.set(instance, converted);
                    }
                }
            }
        } else {
            String val = result.getPositionalValue(pb.index);
            if (val == null && pb.defaultValue != null && !pb.defaultValue.isEmpty()) {
                val = pb.defaultValue;
            }
            if (val != null) {
                pb.accessor.set(instance, TypeConverter.convert(val, pb.type));
            }
        }
    }

    private static String resolveOptionValue(ParseResult result, String[] names) {
        for (String name : names) {
            String val = result.getOptionValue(name);
            if (val != null) {
                return val;
            }
        }
        return null;
    }

    private static List<String> collectOptionValues(ParseResult result, OptionBinding ob) {
        List<String> collected = new ArrayList<>();
        for (String name : ob.names) {
            List<String> vals = result.getOptionValues(name);
            if (vals != null) {
                collected.addAll(vals);
            }
        }
        if (ob.split != null && !ob.split.isEmpty()) {
            List<String> split = new ArrayList<>();
            for (String v : collected) {
                Collections.addAll(split, v.split(ob.split));
            }
            return split;
        }
        return collected;
    }

    @Override
    public void setParentCommand(Object instance, Object parent) {
        if (parentCommandAccessor != null) {
            parentCommandAccessor.set(instance, parent);
        }
    }

    @Override
    public void initMixins(Object instance, Factory factory) throws Exception {
        for (MixinBinding mb : mixinBindings) {
            Object mixin = factory.create(mb.mixinType);
            mb.accessor.set(instance, mixin);
        }
    }

    @Override
    public void injectSpec(Object instance, CommandSpec spec) {
        if (specAccessor != null) {
            specAccessor.set(instance, spec);
        }
        for (FieldAccessor mixeeAccessor : mixeeSpecAccessors) {
            mixeeAccessor.set(instance, spec);
        }
    }

    @Override
    public void setUnmatched(Object instance, List<String> unmatched) {
        if (unmatchedAccessor != null) {
            unmatchedAccessor.set(instance, unmatched);
        }
    }

    @Override
    public void initArgGroups(Object instance, Factory factory) throws Exception {
        for (ArgGroupBinding ag : argGroupBindings) {
            Object group = factory.create(ag.argGroupType);
            ag.accessor.set(instance, group);
        }
    }

    // --- Data records ---

    public enum FieldKind {
        SINGLE, LIST, SET, ARRAY, MAP, OPTIONAL
    }

    public record OptionBinding(
            String[] names,
            String description,
            Class<?> type,
            boolean required,
            String defaultValue,
            String arity,
            boolean hidden,
            String paramLabel,
            String fieldName,
            boolean versionHelp,
            boolean usageHelp,
            boolean negatable,
            int order,
            FieldKind fieldKind,
            Class<?> componentType,
            Class<?> optionalInnerType,
            String split,
            String mapFallbackValue,
            FieldAccessor accessor) {
    }

    public record ParameterBinding(
            int index,
            String description,
            Class<?> type,
            String defaultValue,
            String arity,
            boolean hidden,
            String paramLabel,
            String fieldName,
            boolean isMultiValue,
            FieldKind fieldKind,
            Class<?> componentType,
            FieldAccessor accessor,
            String split) {
    }

    public record MixinBinding(
            String fieldName,
            Class<?> mixinType,
            FieldAccessor accessor) {
    }

    public record ArgGroupBinding(
            String fieldName,
            Class<?> argGroupType,
            FieldAccessor accessor) {
    }

    // --- Builder ---

    public static Builder builder(Class<?> commandClass, Supplier<Object> instanceCreator) {
        return new Builder(commandClass, instanceCreator);
    }

    public static final class Builder {
        private final Class<?> commandClass;
        private final Supplier<Object> instanceCreator;
        private String name = "";
        private String[] description = new String[0];
        private String[] version = new String[0];
        private boolean mixinStandardHelpOptions;
        private String[] header = new String[0];
        private String[] footer = new String[0];
        private ScopeType scope = ScopeType.LOCAL;
        private String[] aliases = new String[0];
        private boolean hasUnmatchedField;
        private boolean hasSpecField;
        private Class<? extends VersionProvider> versionProviderClass = VersionProvider.NoVersionProvider.class;
        private final List<List<String>> exclusiveGroups = new ArrayList<>();
        private String commandListHeading = "Commands:%n";
        private String synopsisHeading = "Usage: ";
        private String optionListHeading = "Options:%n";
        private String headerHeading = "";
        private String parameterListHeading = "%n";
        private boolean showDefaultValues;
        private final List<OptionBinding> optionBindings = new ArrayList<>();
        private final List<ParameterBinding> parameterBindings = new ArrayList<>();
        private final List<Class<?>> subcommandClasses = new ArrayList<>();
        private final List<MixinBinding> mixinBindings = new ArrayList<>();
        private final List<ArgGroupBinding> argGroupBindings = new ArrayList<>();
        private FieldAccessor parentCommandAccessor;
        private FieldAccessor unmatchedAccessor;
        private FieldAccessor specAccessor;
        private final List<FieldAccessor> mixeeSpecAccessors = new ArrayList<>();

        Builder(Class<?> commandClass, Supplier<Object> instanceCreator) {
            this.commandClass = commandClass;
            this.instanceCreator = instanceCreator;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String... description) {
            this.description = description;
            return this;
        }

        public Builder version(String... version) {
            this.version = version;
            return this;
        }

        public Builder mixinStandardHelpOptions(boolean v) {
            this.mixinStandardHelpOptions = v;
            return this;
        }

        public Builder header(String... header) {
            this.header = header;
            return this;
        }

        public Builder footer(String... footer) {
            this.footer = footer;
            return this;
        }

        public Builder scope(ScopeType scope) {
            this.scope = scope;
            return this;
        }

        public Builder aliases(String... aliases) {
            this.aliases = aliases;
            return this;
        }

        public Builder hasUnmatchedField(boolean v) {
            this.hasUnmatchedField = v;
            return this;
        }

        public Builder hasSpecField(boolean v) {
            this.hasSpecField = v;
            return this;
        }

        public Builder versionProviderClass(Class<? extends VersionProvider> cls) {
            this.versionProviderClass = cls;
            return this;
        }

        public Builder commandListHeading(String v) {
            this.commandListHeading = v;
            return this;
        }

        public Builder synopsisHeading(String v) {
            this.synopsisHeading = v;
            return this;
        }

        public Builder optionListHeading(String v) {
            this.optionListHeading = v;
            return this;
        }

        public Builder headerHeading(String v) {
            this.headerHeading = v;
            return this;
        }

        public Builder parameterListHeading(String v) {
            this.parameterListHeading = v;
            return this;
        }

        public Builder showDefaultValues(boolean v) {
            this.showDefaultValues = v;
            return this;
        }

        public Builder addOption(OptionBinding binding) {
            this.optionBindings.add(binding);
            return this;
        }

        public Builder addParameter(ParameterBinding binding) {
            this.parameterBindings.add(binding);
            return this;
        }

        public Builder addSubcommand(Class<?> subcommandClass) {
            this.subcommandClasses.add(subcommandClass);
            return this;
        }

        public Builder addMixin(MixinBinding binding) {
            this.mixinBindings.add(binding);
            return this;
        }

        public Builder addArgGroup(ArgGroupBinding binding) {
            this.argGroupBindings.add(binding);
            return this;
        }

        public Builder addExclusiveGroup(List<String> group) {
            this.exclusiveGroups.add(group);
            return this;
        }

        public Builder parentCommandAccessor(FieldAccessor accessor) {
            this.parentCommandAccessor = accessor;
            return this;
        }

        public Builder unmatchedAccessor(FieldAccessor accessor) {
            this.unmatchedAccessor = accessor;
            return this;
        }

        public Builder specAccessor(FieldAccessor accessor) {
            this.specAccessor = accessor;
            return this;
        }

        public Builder addMixeeSpecAccessor(FieldAccessor accessor) {
            this.mixeeSpecAccessors.add(accessor);
            return this;
        }

        public BuiltCommandModel build() {
            return new BuiltCommandModel(this);
        }
    }
}
