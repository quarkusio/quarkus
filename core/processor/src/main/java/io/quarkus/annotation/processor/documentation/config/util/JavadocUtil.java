package io.quarkus.annotation.processor.documentation.config.util;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JavadocUtil {

    static final String VERTX_JAVA_DOC_SITE = "https://vertx.io/docs/apidocs/";
    static final String OFFICIAL_JAVA_DOC_BASE_LINK = "https://docs.oracle.com/en/java/javase/17/docs/api/java.base/";
    static final String AGROAL_API_JAVA_DOC_SITE = "https://javadoc.io/doc/io.agroal/agroal-api/latest/";
    static final String LOG_LEVEL_REDIRECT_URL = "https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html";

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^(\\w+)\\.(\\w+)\\..*$");

    private static final Map<String, String> EXTENSION_JAVA_DOC_LINK = new HashMap<>();

    static {
        EXTENSION_JAVA_DOC_LINK.put("io.vertx.", VERTX_JAVA_DOC_SITE);
        EXTENSION_JAVA_DOC_LINK.put("io.agroal.", AGROAL_API_JAVA_DOC_SITE);
    }

    private JavadocUtil() {
    }

    /**
     * Get javadoc link of a given type value
     */
    public static String getJavadocSiteLink(String binaryName) {
        if (binaryName.equals(Level.class.getName())) {
            //hack, we don't want to link to the JUL version, but the jboss logging version
            //this seems like a one off use case so for now it is just hacked in here
            //if there are other use cases we should do something more generic
            return LOG_LEVEL_REDIRECT_URL;
        }
        Matcher packageMatcher = PACKAGE_PATTERN.matcher(binaryName);

        if (!packageMatcher.find()) {
            return null;
        }

        if (TypeUtil.isPrimitiveWrapper(binaryName) || Types.ALIASED_TYPES.containsKey(binaryName)) {
            return null;
        }

        if ("java".equals(packageMatcher.group(1))) {
            return OFFICIAL_JAVA_DOC_BASE_LINK + getJavaDocLinkForType(binaryName);
        }

        String basePkgName = packageMatcher.group(1) + "." + packageMatcher.group(2) + ".";
        String javaDocBaseUrl = EXTENSION_JAVA_DOC_LINK.get(basePkgName);

        if (javaDocBaseUrl != null) {
            return javaDocBaseUrl + getJavaDocLinkForType(binaryName);
        }

        return null;
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
}
