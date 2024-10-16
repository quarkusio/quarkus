package io.quarkus.annotation.processor.documentation.config.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.JavadocBlockTag.Type;
import com.github.javaparser.javadoc.description.JavadocDescription;

import io.quarkus.annotation.processor.documentation.config.discovery.ParsedJavadoc;
import io.quarkus.annotation.processor.documentation.config.discovery.ParsedJavadocSection;
import io.quarkus.annotation.processor.documentation.config.model.JavadocFormat;

public final class JavadocUtil {

    private static final Pattern START_OF_LINE = Pattern.compile("^", Pattern.MULTILINE);
    private static final Pattern REPLACE_WINDOWS_EOL = Pattern.compile("\r\n");
    private static final Pattern REPLACE_MACOS_EOL = Pattern.compile("\r");
    private static final String DOT = ".";
    private static final String NEW_LINE = "\n";

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

    public static ParsedJavadoc parseConfigItemJavadoc(String rawJavadoc) {
        if (rawJavadoc == null || rawJavadoc.isBlank()) {
            return ParsedJavadoc.empty();
        }

        // the parser expects all the lines to start with "* "
        // we add it as it has been previously removed
        Javadoc javadoc = StaticJavaParser.parseJavadoc(START_OF_LINE.matcher(rawJavadoc).replaceAll("* "));

        String description;
        JavadocFormat format;

        if (isAsciidoc(javadoc)) {
            description = normalizeEol(javadoc.getDescription().toText());
            format = JavadocFormat.ASCIIDOC;
        } else if (isMarkdown(javadoc)) {
            // this is to prepare the Markdown Javadoc that will come up soon enough
            // I don't know exactly how the parser will deal with them though
            description = normalizeEol(javadoc.getDescription().toText());
            format = JavadocFormat.MARKDOWN;
        } else {
            description = normalizeEol(javadoc.getDescription().toText());
            format = JavadocFormat.JAVADOC;
        }

        Optional<String> since = javadoc.getBlockTags().stream()
                .filter(t -> t.getType() == Type.SINCE)
                .map(JavadocBlockTag::getContent)
                .map(JavadocDescription::toText)
                .findFirst();

        Optional<String> deprecated = javadoc.getBlockTags().stream()
                .filter(t -> t.getType() == Type.DEPRECATED)
                .map(JavadocBlockTag::getContent)
                .map(JavadocDescription::toText)
                .findFirst();

        if (description != null && description.isBlank()) {
            description = null;
        }

        return new ParsedJavadoc(description, format, since.orElse(null), deprecated.orElse(null));
    }

    public static ParsedJavadocSection parseConfigSectionJavadoc(String javadocComment) {
        if (javadocComment == null || javadocComment.trim().isEmpty()) {
            return ParsedJavadocSection.empty();
        }

        // the parser expects all the lines to start with "* "
        // we add it as it has been previously removed
        javadocComment = START_OF_LINE.matcher(javadocComment).replaceAll("* ");
        Javadoc javadoc = StaticJavaParser.parseJavadoc(javadocComment);

        Optional<String> deprecated = javadoc.getBlockTags().stream()
                .filter(t -> t.getType() == Type.DEPRECATED)
                .map(JavadocBlockTag::getContent)
                .map(JavadocDescription::toText)
                .findFirst();

        String description;
        JavadocFormat format;

        if (isAsciidoc(javadoc)) {
            description = normalizeEol(javadoc.getDescription().toText());
            format = JavadocFormat.ASCIIDOC;
        } else if (isMarkdown(javadoc)) {
            // this is to prepare the Markdown Javadoc that will come up soon enough
            // I don't know exactly how the parser will deal with them though
            description = normalizeEol(javadoc.getDescription().toText());
            format = JavadocFormat.MARKDOWN;
        } else {
            description = normalizeEol(javadoc.getDescription().toText());
            format = JavadocFormat.JAVADOC;
        }

        if (description == null || description.isBlank()) {
            return ParsedJavadocSection.empty();
        }

        final int newLineIndex = description.indexOf(NEW_LINE);
        final int dotIndex = description.indexOf(DOT);

        final int endOfTitleIndex;
        if (newLineIndex > 0 && newLineIndex < dotIndex) {
            endOfTitleIndex = newLineIndex;
        } else {
            endOfTitleIndex = dotIndex;
        }

        String title;
        String details;

        if (endOfTitleIndex == -1) {
            title = description.trim();
            details = null;
        } else {
            title = description.substring(0, endOfTitleIndex).trim();
            details = description.substring(endOfTitleIndex + 1).trim();
        }

        if (title.contains("<")) {
            title = Jsoup.parse(title).text();
        }

        title = title.replaceAll("^([^\\w])+", "");

        return new ParsedJavadocSection(title == null || title.isBlank() ? null : title,
                details == null || details.isBlank() ? null : details, format,
                deprecated.orElse(null));
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

    private static String normalizeEol(String javadoc) {
        // it's Asciidoc, so we just pass through
        // it also uses platform specific EOL, so we need to convert them back to \n
        String normalizedJavadoc = javadoc;
        normalizedJavadoc = REPLACE_WINDOWS_EOL.matcher(normalizedJavadoc).replaceAll("\n");
        normalizedJavadoc = REPLACE_MACOS_EOL.matcher(normalizedJavadoc).replaceAll("\n");
        return normalizedJavadoc;
    }

    private static boolean isAsciidoc(Javadoc javadoc) {
        for (JavadocBlockTag blockTag : javadoc.getBlockTags()) {
            if ("asciidoclet".equals(blockTag.getTagName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMarkdown(Javadoc javadoc) {
        for (JavadocBlockTag blockTag : javadoc.getBlockTags()) {
            if ("markdown".equals(blockTag.getTagName())) {
                return true;
            }
        }
        return false;
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
