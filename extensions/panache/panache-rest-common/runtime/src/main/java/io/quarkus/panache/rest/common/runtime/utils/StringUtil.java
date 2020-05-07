package io.quarkus.panache.rest.common.runtime.utils;

import static io.quarkus.runtime.util.StringUtil.camelHumpsIterator;
import static io.quarkus.runtime.util.StringUtil.lowerCase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public final class StringUtil {

    private static final List<String> REQUIRE_ES = Arrays.asList("s", "sh", "ch", "x", "z", "o");

    private static final Map<String, String> IRREGULARS = new HashMap<>();

    static {
        IRREGULARS.put("child", "children");
        IRREGULARS.put("goose", "geese");
        IRREGULARS.put("man", "men");
        IRREGULARS.put("woman", "women");
        IRREGULARS.put("tooth", "teeth");
        IRREGULARS.put("foot", "feet");
        IRREGULARS.put("mouse", "mice");
        IRREGULARS.put("person", "people");
        IRREGULARS.put("photo", "photos");
        IRREGULARS.put("piano", "pianos");
        IRREGULARS.put("halo", "halos");
        IRREGULARS.put("roof", "roofs");
        IRREGULARS.put("belief", "beliefs");
        IRREGULARS.put("chef", "chefs");
        IRREGULARS.put("chief", "chiefs");
        IRREGULARS.put("pojo", "pojos");
    }

    public static String camelToHyphenated(String camelString) {
        if (camelString == null) {
            return null;
        }

        StringJoiner joiner = new StringJoiner("-");
        Iterator<String> strings = lowerCase(camelHumpsIterator(camelString));

        while (strings.hasNext()) {
            joiner.add(strings.next());
        }

        return joiner.toString();
    }

    public static String toPlural(String singular) {
        if (singular == null) {
            return null;
        }

        if (IRREGULARS.containsKey(singular)) {
            return IRREGULARS.get(singular);
        }

        if (singular.endsWith("f")) {
            return replaceSuffix(singular, "f", "ves");
        }

        if (singular.endsWith("fe")) {
            return replaceSuffix(singular, "fe", "ves");
        }

        if (singular.endsWith("y")) {
            return replaceSuffix(singular, "y", "ies");
        }

        if (singular.endsWith("is")) {
            return replaceSuffix(singular, "is", "es");
        }

        if (singular.endsWith("on")) {
            return replaceSuffix(singular, "on", "a");
        }

        if (REQUIRE_ES.stream().anyMatch(singular::endsWith)) {
            return singular + "es";
        }

        return singular + "s";
    }

    private static String replaceSuffix(String word, String originalSuffix, String newSuffix) {
        return word.substring(0, word.length() - originalSuffix.length()) + newSuffix;
    }
}
