package io.quarkus.annotation.processor.generate_doc;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import io.quarkus.annotation.processor.Constants;

public class DocGeneratorUtil {
    private static final String CORE = "core";
    private static final String CONFIG = "Config";
    private static final String CONFIGURATION = "Configuration";
    public static final String LEVEL_HACK_URL = "https://docs.jboss.org/jbossas/javadoc/7.1.2.Final/org/jboss/logmanager/Level.html";
    private static String CONFIG_GROUP_DOC_PREFIX = "config-group-";
    static final String VERTX_JAVA_DOC_SITE = "https://vertx.io/docs/apidocs/";
    static final String OFFICIAL_JAVA_DOC_BASE_LINK = "https://docs.oracle.com/javase/8/docs/api/";
    static final String AGROAL_API_JAVA_DOC_SITE = "https://jar-download.com/javaDoc/io.agroal/agroal-api/1.5/index.html?";

    private static final Map<String, String> JAVA_PRIMITIVE_WRAPPERS = new HashMap<>();
    private static final Map<String, String> PRIMITIVE_DEFAULT_VALUES = new HashMap<>();
    private static final Map<String, String> EXTENSION_JAVA_DOC_LINK = new HashMap<>();
    private static Pattern PACKAGE_PATTERN = Pattern.compile("^(\\w+)\\.(\\w+)\\..*$");
    private static final String HYPHEN = "-";
    private static final Pattern PATTERN = Pattern.compile("([-_]+)");

    static {
        PRIMITIVE_DEFAULT_VALUES.put("int", "0");
        PRIMITIVE_DEFAULT_VALUES.put("byte", "0");
        PRIMITIVE_DEFAULT_VALUES.put("char", "");
        PRIMITIVE_DEFAULT_VALUES.put("short", "0");
        PRIMITIVE_DEFAULT_VALUES.put("long", "0l");
        PRIMITIVE_DEFAULT_VALUES.put("float", "0f");
        PRIMITIVE_DEFAULT_VALUES.put("double", "0d");
        PRIMITIVE_DEFAULT_VALUES.put("boolean", "false");

        JAVA_PRIMITIVE_WRAPPERS.put("java.lang.Character", "char");
        JAVA_PRIMITIVE_WRAPPERS.put("java.lang.Boolean", "boolean");
        JAVA_PRIMITIVE_WRAPPERS.put("java.lang.Byte", "byte");
        JAVA_PRIMITIVE_WRAPPERS.put("java.lang.Short", "short");
        JAVA_PRIMITIVE_WRAPPERS.put("java.lang.Integer", "int");
        JAVA_PRIMITIVE_WRAPPERS.put("java.lang.Long", "long");
        JAVA_PRIMITIVE_WRAPPERS.put("java.lang.Float", "float");
        JAVA_PRIMITIVE_WRAPPERS.put("java.lang.Double", "double");

        EXTENSION_JAVA_DOC_LINK.put("io.vertx.", VERTX_JAVA_DOC_SITE);
        EXTENSION_JAVA_DOC_LINK.put("io.agroal.", AGROAL_API_JAVA_DOC_SITE);
    }

    /**
     * Retrieve a default value of a primitive type.
     * If type is not a primitive, returns false
     *
     */
    static String getPrimitiveDefaultValue(String primitiveType) {
        return PRIMITIVE_DEFAULT_VALUES.get(primitiveType);
    }

    /**
     * Replaces Java primitive wrapper types with primitive types
     */
    static String unbox(String type) {
        String mapping = JAVA_PRIMITIVE_WRAPPERS.get(type);
        return mapping == null ? type : mapping;
    }

    /**
     * Get javadoc link of a given type value
     */
    static String getJavaDocSiteLink(String type) {
        if (type.equals(Level.class.getName())) {
            //hack, we don't want to link to the JUL version, but the jboss logging version
            //this seems like a one off use case so for now it is just hacked in here
            //if there are other use cases we should do something more generic
            return LEVEL_HACK_URL;
        }
        Matcher packageMatcher = PACKAGE_PATTERN.matcher(type);

        if (!packageMatcher.find()) {
            return Constants.EMPTY;
        }

        if (JAVA_PRIMITIVE_WRAPPERS.containsKey(type)) {
            return Constants.EMPTY;
        }

        if ("java".equals(packageMatcher.group(1))) {
            return OFFICIAL_JAVA_DOC_BASE_LINK + getJavaDocLinkForType(type);
        }

        String basePkgName = packageMatcher.group(1) + "." + packageMatcher.group(2) + ".";
        String javaDocBaseUrl = EXTENSION_JAVA_DOC_LINK.get(basePkgName);

        if (javaDocBaseUrl != null) {
            return javaDocBaseUrl + getJavaDocLinkForType(type);
        }

        return Constants.EMPTY;
    }

    private static String getJavaDocLinkForType(String type) {
        int beginOfWrappedTypeIndex = type.indexOf("<");
        if (beginOfWrappedTypeIndex != -1) {
            type = type.substring(0, beginOfWrappedTypeIndex);
        }

        int indexOfFirstUpperCase = 0;
        for (int index = 0; index < type.length(); index++) {
            char charAt = type.charAt(index);
            if (charAt >= 'A' && charAt <= 'Z') {
                indexOfFirstUpperCase = index;
                break;
            }
        }

        final String base = type.substring(0, indexOfFirstUpperCase).replace('.', '/');
        final String html = type.substring(indexOfFirstUpperCase).replace('$', '.') + ".html";

        return base + html;
    }

    /**
     * Retrieve enclosed type from known optional types
     */
    static String getKnownGenericType(DeclaredType declaredType) {
        return Constants.ALIASED_TYPES.get(declaredType.toString());
    }

    static Iterator<String> camelHumpsIterator(String str) {
        return new Iterator<String>() {
            int idx;

            @Override
            public boolean hasNext() {
                return idx < str.length();
            }

            @Override
            public String next() {
                if (idx == str.length())
                    throw new NoSuchElementException();
                // known mixed-case rule-breakers
                if (str.startsWith("JBoss", idx)) {
                    idx += 5;
                    return "JBoss";
                }
                final int start = idx;
                int c = str.codePointAt(idx);
                if (Character.isUpperCase(c)) {
                    // an uppercase-starting word
                    idx = str.offsetByCodePoints(idx, 1);
                    if (idx < str.length()) {
                        c = str.codePointAt(idx);
                        if (Character.isUpperCase(c)) {
                            // all-caps word; need one look-ahead
                            int nextIdx = str.offsetByCodePoints(idx, 1);
                            while (nextIdx < str.length()) {
                                c = str.codePointAt(nextIdx);
                                if (Character.isLowerCase(c)) {
                                    // ended at idx
                                    return str.substring(start, idx);
                                }
                                idx = nextIdx;
                                nextIdx = str.offsetByCodePoints(idx, 1);
                            }
                            // consumed the whole remainder, update idx to length
                            idx = str.length();
                            return str.substring(start);
                        } else {
                            // initial caps, trailing lowercase
                            idx = str.offsetByCodePoints(idx, 1);
                            while (idx < str.length()) {
                                c = str.codePointAt(idx);
                                if (Character.isUpperCase(c)) {
                                    // end
                                    return str.substring(start, idx);
                                }
                                idx = str.offsetByCodePoints(idx, 1);
                            }
                            // consumed the whole remainder
                            return str.substring(start);
                        }
                    } else {
                        // one-letter word
                        return str.substring(start);
                    }
                } else {
                    // a lowercase-starting word
                    idx = str.offsetByCodePoints(idx, 1);
                    while (idx < str.length()) {
                        c = str.codePointAt(idx);
                        if (Character.isUpperCase(c)) {
                            // end
                            return str.substring(start, idx);
                        }
                        idx = str.offsetByCodePoints(idx, 1);
                    }
                    // consumed the whole remainder
                    return str.substring(start);
                }
            }
        };
    }

    static Iterator<String> lowerCase(Iterator<String> orig) {
        return new Iterator<String>() {
            @Override
            public boolean hasNext() {
                return orig.hasNext();
            }

            @Override
            public String next() {
                return orig.next().toLowerCase(Locale.ROOT);
            }
        };
    }

    static String join(Iterator<String> it) {
        final StringBuilder b = new StringBuilder();
        if (it.hasNext()) {
            b.append(it.next());
            while (it.hasNext()) {
                b.append("-");
                b.append(it.next());
            }
        }
        return b.toString();
    }

    static String hyphenate(String orig) {
        return join(lowerCase(camelHumpsIterator(orig)));
    }

    /**
     * This needs to be consistent with io.quarkus.runtime.configuration.HyphenateEnumConverter.
     */
    static String hyphenateEnumValue(String orig) {
        StringBuffer target = new StringBuffer();
        String hyphenate = hyphenate(orig);
        Matcher matcher = PATTERN.matcher(hyphenate);
        while (matcher.find()) {
            matcher.appendReplacement(target, HYPHEN);
        }
        matcher.appendTail(target);
        return target.toString();
    }

    static String normalizeDurationValue(String value) {
        if (!value.isEmpty() && Character.isDigit(value.charAt(value.length() - 1))) {
            try {
                value = Integer.parseInt(value) + "S";
            } catch (NumberFormatException ignore) {
            }
        }
        value = value.toUpperCase(Locale.ROOT);
        return value;
    }

    static String joinAcceptedValues(List<String> acceptedValues) {
        if (acceptedValues == null || acceptedValues.isEmpty()) {
            return "";
        }

        return acceptedValues.stream().collect(Collectors.joining("`, `", "`", "`"));
    }

    static String getTypeFormatInformationNote(ConfigDocKey configDocKey) {
        if (configDocKey.getType().equals(Duration.class.getName())) {
            return Constants.DURATION_INFORMATION;
        } else if (configDocKey.getType().equals(Constants.MEMORY_SIZE_TYPE)) {
            return Constants.MEMORY_SIZE_INFORMATION;
        }

        return Constants.EMPTY;
    }

    static boolean hasDurationInformationNote(ConfigDocKey configDocKey) {
        return configDocKey.hasType() && configDocKey.getType().equals(Duration.class.getName());
    }

    static boolean hasMemoryInformationNote(ConfigDocKey configDocKey) {
        return configDocKey.hasType() && configDocKey.getType().equals(Constants.MEMORY_SIZE_TYPE);
    }

    /**
     * Guess extension name from given configuration root class name
     */
    public static String computeExtensionDocFileName(String configRoot) {
        StringBuilder extensionNameBuilder = new StringBuilder();
        final Matcher matcher = Constants.PKG_PATTERN.matcher(configRoot);
        if (!matcher.find()) {
            extensionNameBuilder.append(configRoot);
        } else {
            String extensionName = matcher.group(1);
            extensionNameBuilder.append(Constants.QUARKUS);
            extensionNameBuilder.append(Constants.DASH);

            if (Constants.DEPLOYMENT.equals(extensionName) || Constants.RUNTIME.equals(extensionName)) {
                extensionNameBuilder.append(CORE);
            } else {
                extensionNameBuilder.append(extensionName);
                for (int i = 2; i <= matcher.groupCount(); i++) {
                    String subgroup = matcher.group(i);
                    if (Constants.DEPLOYMENT.equals(subgroup)
                            || Constants.RUNTIME.equals(subgroup)
                            || Constants.COMMON.equals(subgroup)
                            || !subgroup.matches(Constants.DIGIT_OR_LOWERCASE)) {
                        break;
                    }
                    if (i > 3 && Constants.CONFIG.equals(subgroup)) {
                        // this is a bit dark magic but we have config packages as valid extension names
                        // and config packages where the configuration is stored
                        break;
                    }
                    extensionNameBuilder.append(Constants.DASH);
                    extensionNameBuilder.append(matcher.group(i));
                }
            }
        }

        extensionNameBuilder.append(Constants.ADOC_EXTENSION);
        return extensionNameBuilder.toString();
    }

    /**
     * Guess config group file name from given configuration group class name
     */
    public static String computeConfigGroupDocFileName(String configGroupClassName) {
        final String sanitizedClassName;
        final Matcher matcher = Constants.PKG_PATTERN.matcher(configGroupClassName);

        if (!matcher.find()) {
            sanitizedClassName = CONFIG_GROUP_DOC_PREFIX + Constants.DASH + hyphenate(configGroupClassName);
        } else {
            String replacement = Constants.DASH + CONFIG_GROUP_DOC_PREFIX + Constants.DASH;
            sanitizedClassName = configGroupClassName
                    .replaceFirst("io.", "")
                    .replaceFirst("\\.runtime\\.", replacement)
                    .replaceFirst("\\.deployment\\.", replacement);
        }

        return hyphenate(sanitizedClassName)
                .replaceAll("[\\.-]+", Constants.DASH)
                + Constants.ADOC_EXTENSION;
    }

    /**
     * Guess config root file name from given configuration root class name.
     */
    public static String computeConfigRootDocFileName(String configRootClassName, String rootName) {
        String sanitizedClassName;
        final Matcher matcher = Constants.PKG_PATTERN.matcher(configRootClassName);

        if (!matcher.find()) {
            sanitizedClassName = rootName + Constants.DASH + hyphenate(configRootClassName);
        } else {
            String deployment = Constants.DOT + Constants.DEPLOYMENT + Constants.DOT;
            String runtime = Constants.DOT + Constants.RUNTIME + Constants.DOT;

            if (configRootClassName.contains(deployment)) {
                sanitizedClassName = configRootClassName
                        .substring(configRootClassName.indexOf(deployment) + deployment.length());
            } else if (configRootClassName.contains(runtime)) {
                sanitizedClassName = configRootClassName.substring(configRootClassName.indexOf(runtime) + runtime.length());
            } else {
                sanitizedClassName = configRootClassName.replaceFirst("io.quarkus.", "");
            }

            sanitizedClassName = rootName + Constants.DASH + sanitizedClassName;
        }

        return hyphenate(sanitizedClassName)
                .replaceAll("[\\.-]+", Constants.DASH)
                + Constants.ADOC_EXTENSION;
    }

    public static void appendConfigItemsIntoExistingOnes(List<ConfigDocItem> existingConfigItems,
            List<ConfigDocItem> configDocItems) {
        for (ConfigDocItem configDocItem : configDocItems) {
            if (configDocItem.isConfigKey()) {
                existingConfigItems.add(configDocItem);
            } else {
                ConfigDocSection configDocSection = configDocItem.getConfigDocSection();
                boolean configSectionMerged = mergeSectionIntoPreviousExistingConfigItems(configDocSection,
                        existingConfigItems);
                if (!configSectionMerged) {
                    existingConfigItems.add(configDocItem);
                }
            }
        }
    }

    /**
     * returns true if section is merged into one of the existing config items, false otherwise
     */
    private static boolean mergeSectionIntoPreviousExistingConfigItems(ConfigDocSection section,
            List<ConfigDocItem> configDocItems) {
        for (ConfigDocItem configDocItem : configDocItems) {
            if (configDocItem.isConfigKey()) {
                continue;
            }

            ConfigDocSection configDocSection = configDocItem.getConfigDocSection();
            if (configDocSection.equals(section)) {
                appendConfigItemsIntoExistingOnes(configDocSection.getConfigDocItems(), section.getConfigDocItems());
                return true;
            } else {
                boolean configSectionMerged = mergeSectionIntoPreviousExistingConfigItems(section,
                        configDocSection.getConfigDocItems());
                if (configSectionMerged) {
                    return true;
                }
            }
        }

        return false;
    }

    static String stringifyType(TypeMirror typeMirror) {
        List<? extends TypeMirror> typeArguments = ((DeclaredType) typeMirror).getTypeArguments();
        String simpleName = typeSimpleName(typeMirror);
        if (typeArguments.isEmpty()) {
            return simpleName;
        } else if (typeArguments.size() == 1) {
            return String.format("%s<%s>", simpleName, stringifyType(typeArguments.get(0)));
        } else if (typeArguments.size() == 2) {
            return String.format("%s<%s,%s>", simpleName, stringifyType(typeArguments.get(0)),
                    stringifyType(typeArguments.get(1)));
        }

        return "unknown"; // we should not reach here
    }

    private static String typeSimpleName(TypeMirror typeMirror) {
        String type = ((DeclaredType) typeMirror).asElement().toString();
        return type.substring(1 + type.lastIndexOf(Constants.DOT));
    }

    static String getName(String prefix, String name, String simpleClassName, ConfigPhase configPhase) {
        if (name.equals(Constants.HYPHENATED_ELEMENT_NAME)) {
            return deriveConfigRootName(simpleClassName, prefix, configPhase);
        }

        if (!prefix.isEmpty()) {
            if (!name.isEmpty()) {
                return prefix + Constants.DOT + name;
            } else {
                return prefix;
            }
        } else {
            return name;
        }
    }

    static String deriveConfigRootName(String simpleClassName, String prefix, ConfigPhase configPhase) {
        String simpleNameInLowerCase = simpleClassName.toLowerCase();
        int length = simpleNameInLowerCase.length();

        if (simpleNameInLowerCase.endsWith(CONFIG.toLowerCase())) {
            String sanitized = simpleClassName.substring(0, length - CONFIG.length());
            return deriveConfigRootName(sanitized, prefix, configPhase);
        } else if (simpleNameInLowerCase.endsWith(CONFIGURATION.toLowerCase())) {
            String sanitized = simpleClassName.substring(0, length - CONFIGURATION.length());
            return deriveConfigRootName(sanitized, prefix, configPhase);
        } else if (simpleNameInLowerCase.endsWith(configPhase.getConfigSuffix().toLowerCase())) {
            String sanitized = simpleClassName.substring(0, length - configPhase.getConfigSuffix().length());
            return deriveConfigRootName(sanitized, prefix, configPhase);
        }

        return !prefix.isEmpty() ? prefix + Constants.DOT + hyphenate(simpleClassName)
                : Constants.QUARKUS + Constants.DOT + hyphenate(simpleClassName);
    }

    /**
     * Sort docs keys. The sorted list will contain the properties in the following order
     * - 1. Map config items as last elements of the generated docs.
     * - 2. Build time properties will come first.
     * - 3. Otherwise respect source code declaration order.
     * - 4. Elements within a configuration section will appear at the end of the generated doc while preserving described in
     * 1-4.
     */
    public static void sort(List<ConfigDocItem> configDocItems) {
        Collections.sort(configDocItems);
        for (ConfigDocItem configDocItem : configDocItems) {
            if (configDocItem.isConfigSection()) {
                sort(configDocItem.getConfigDocSection().getConfigDocItems());
            }
        }
    }

}
