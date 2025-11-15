package io.quarkus.devui.spi.workspace;

import java.util.regex.Pattern;

/**
 * Common patterns for filters
 */
public interface Patterns {
    public static final Pattern JAVA_ANY = Pattern.compile(".*\\.java$");
    public static final Pattern JAVA_SRC = Pattern.compile("^((?!test).)*\\.java$");
    public static final Pattern JAVA_TEST = Pattern.compile("^.*test.*\\.java$");

    public static final Pattern README_MD = Pattern.compile(".*README\\.md$");
    public static final Pattern ANY_MD = Pattern.compile(".*\\.md$");
    public static final Pattern POM_XML = Pattern.compile("^pom\\.xml$");
    public static final Pattern APPLICATION_PROPERTIES = Pattern.compile("^.*application\\.properties$");

    public static final Pattern DOCKER_FILE = Pattern.compile("^.*Dockerfile(\\..*)?$");

    public static final Pattern SHELL_SCRIPT = Pattern.compile(".*\\.(sh|bash|zsh|ksh)$");

    public static final Pattern HTML = Pattern.compile(".*\\.(html|htm)$");
    public static final Pattern CSS = Pattern.compile(".*\\.css$");
    public static final Pattern JS = Pattern.compile(".*\\.js$");
    public static final Pattern JSON = Pattern.compile(".*\\.json$");
    public static final Pattern XML = Pattern.compile(".*\\.xml$");
    public static final Pattern WSDL = Pattern.compile(".*\\.wsdl$");
    public static final Pattern PROPERTIES = Pattern.compile(".*\\.properties$");
    public static final Pattern YAML = Pattern.compile(".*\\.(yaml|yml)$");

    public static final Pattern ANY_KNOWN_TEXT = Pattern.compile(
            ".*(" +
                    "README\\.md" + "|" +
                    "pom\\.xml" + "|" +
                    "application\\.properties" + "|" +
                    "Dockerfile(\\..*)?" + "|" +
                    "\\.(java|sh|bash|zsh|ksh|html|htm|css|js|json|xml|wsdl|properties|yaml|yml)" +
                    ")$");
}
