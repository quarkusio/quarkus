package io.quarkus.bootstrap.util;

import io.quarkus.bootstrap.model.AppArtifactKey;
import java.util.regex.Pattern;

public class BootstrapUtils {

    private static Pattern splitByWs;

    public static String[] splitByWhitespace(String s) {
        if (s == null) {
            return null;
        }
        if (splitByWs == null) {
            splitByWs = Pattern.compile("\\s+");
        }
        return splitByWs.split(s);
    }

    public static AppArtifactKey[] parseDependencyCondition(String s) {
        final String[] strArr = splitByWhitespace(s);
        if (strArr == null) {
            return null;
        }
        final AppArtifactKey[] keys = new AppArtifactKey[strArr.length];
        for (int i = 0; i < strArr.length; ++i) {
            keys[i] = AppArtifactKey.fromString(strArr[i]);
        }
        return keys;
    }
}
