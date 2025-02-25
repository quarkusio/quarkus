package io.quarkus.bootstrap.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.bootstrap.BootstrapConstants;

public class MutableStandardJvmOption extends MutableBaseJvmOption<MutableStandardJvmOption> {

    public static final String PROPERTY_GROUP_PREFIX = "std.";

    private static final String COMPLETE_PROPERTY_PREFIX = BootstrapConstants.EXT_DEV_MODE_JVM_OPTION_PREFIX
            + PROPERTY_GROUP_PREFIX;

    private static final String DASH_DASH = "--";
    private static final String COMMA = ",";
    private static final String EQUALS = "=";

    public static MutableStandardJvmOption fromQuarkusExtensionProperty(String propertyName, String value) {
        final String optionName = propertyName.substring(COMPLETE_PROPERTY_PREFIX.length());
        return value.isBlank() ? newInstance(optionName) : newInstance(optionName, value);
    }

    public static MutableStandardJvmOption newInstance(String name) {
        return newInstance(name, null);
    }

    public static MutableStandardJvmOption newInstance(String name, String value) {
        var result = new MutableStandardJvmOption();
        result.setName(name);
        if (value != null) {
            result.addValue(value);
        }
        return result;
    }

    @Override
    protected String getQuarkusExtensionPropertyPrefix() {
        return COMPLETE_PROPERTY_PREFIX;
    }

    @Override
    public List<String> toCliOptions() {
        switch (getName()) {
            case "add-modules":
                return toCliSortedValueList(COMMA);
            case "add-opens":
                return toCliModulePackageList();
            default:
                return toCliGenericArgument();
        }
    }

    private List<String> toCliModulePackageList() {
        if (!hasValue()) {
            return List.of();
        }
        var name = getName();
        var values = getValues();
        if (values.size() == 1) {
            return List.of(DASH_DASH + name + EQUALS + EQUALS + values.iterator().next());
        }
        final Map<String, Set<String>> modulePackages = new HashMap<>(values.size());
        for (String value : values) {
            final int slash = value.indexOf('=');
            if (slash < 1) {
                throw new IllegalArgumentException(
                        "Value '" + value + "' does not follow module/package=target-module(,target-module) format");
            }
            final Set<String> targetModules = modulePackages.computeIfAbsent(value.substring(0, slash), k -> new HashSet<>());
            final String[] packageNames = value.substring(slash + 1).split(COMMA);
            for (String packageName : packageNames) {
                targetModules.add(packageName);
            }
        }
        final String[] modulePackageList = toSortedArray(modulePackages.keySet());
        final List<String> result = new ArrayList<>(modulePackageList.length * 2);
        for (String modulePackage : modulePackageList) {
            final Set<String> targetModules = modulePackages.get(modulePackage);
            if (!targetModules.isEmpty()) {
                result.add(DASH_DASH + name);
                final StringBuilder sb = new StringBuilder()
                        .append(modulePackage).append(EQUALS);
                final String[] targetModuleNames = toSortedArray(targetModules);
                appendItems(sb, targetModuleNames, COMMA);
                result.add(sb.toString());
            }
        }
        return result;
    }

    private List<String> toCliSortedValueList(String valueSeparator) {
        if (!hasValue()) {
            return List.of();
        }
        var sb = new StringBuilder().append(DASH_DASH).append(getName()).append(EQUALS);
        var values = getValues();
        if (values.size() == 1) {
            sb.append(values.iterator().next());
            return List.of(sb.toString());
        }
        appendItems(sb, toSortedArray(values), valueSeparator);
        return List.of(sb.toString());
    }

    private List<String> toCliGenericArgument() {
        return getValues().isEmpty() ? List.of(DASH_DASH + getName()) : toCliSortedValueList(COMMA);
    }

    private static void appendItems(StringBuilder sb, String[] arr, String itemSeparator) {
        sb.append(arr[0]);
        for (int i = 1; i < arr.length; ++i) {
            sb.append(itemSeparator).append(arr[i]);
        }
    }

    private static String[] toSortedArray(Collection<String> value) {
        final String[] arr = value.toArray(new String[0]);
        Arrays.sort(arr);
        return arr;
    }
}
