package io.quarkus.rest.data.panache.deployment.utils;

import static io.quarkus.runtime.util.StringUtil.camelHumpsIterator;
import static io.quarkus.runtime.util.StringUtil.lowerCase;
import static io.quarkus.runtime.util.StringUtil.toList;
import static io.quarkus.runtime.util.StringUtil.withoutSuffix;

public final class ResourceName {

    private static final String[] SUFFIXES = { "controller", "resource", "repository" };

    public static String fromClass(String resourceClassName) {
        return String.join("-",
                toList(withoutSuffix(lowerCase(camelHumpsIterator(toSimpleName(resourceClassName))), SUFFIXES)));
    }

    private static String toSimpleName(String className) {
        if (className.contains(".")) {
            return className.substring(className.lastIndexOf(".") + 1);
        }
        return className;
    }
}
