package io.quarkus.panache.rest.common.deployment.utils;

import static io.quarkus.runtime.util.StringUtil.camelHumpsIterator;
import static io.quarkus.runtime.util.StringUtil.lowerCase;
import static io.quarkus.runtime.util.StringUtil.toList;
import static io.quarkus.runtime.util.StringUtil.withoutSuffix;

public final class ResourceName {

    private static final String[] SUFFIXES = { "controller", "resource" };

    public static String fromClass(Class<?> resourceClass) {
        return fromClass(resourceClass.getSimpleName());
    }

    public static String fromClass(String resourceClassName) {
        return String.join("-", toList(withoutSuffix(lowerCase(camelHumpsIterator(resourceClassName)), SUFFIXES)));
    }
}
