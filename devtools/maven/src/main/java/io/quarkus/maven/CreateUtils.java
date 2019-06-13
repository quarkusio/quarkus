package io.quarkus.maven;

import org.apache.commons.lang3.StringUtils;

public final class CreateUtils {

    private CreateUtils() {
        //Not to be constructed
    }

    public static String getDerivedPath(String className) {
        String[] resourceClassName = StringUtils.splitByCharacterTypeCamelCase(
                className.substring(className.lastIndexOf(".") + 1));
        return "/" + resourceClassName[0].toLowerCase();
    }
}
