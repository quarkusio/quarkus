package io.quarkus.devui.spi.workspace;

import java.util.regex.Pattern;

/**
 * Common patterns for filters
 */
public interface Patterns {
    public static Pattern JAVA_ANY = Pattern.compile(".*\\.java$");
    public static Pattern JAVA_SRC = Pattern.compile("^((?!test).)*\\.java$");
    public static Pattern JAVA_TEST = Pattern.compile("^.*test.*\\.java$");

    public static Pattern README_MD = Pattern.compile(".*README\\.md$");
    public static Pattern POM_XML = Pattern.compile("^pom\\.xml$");
    public static Pattern APPLICATION_PROPERTIES = Pattern.compile("^.*application\\.properties$");

    public static Pattern DOCKER_FILE = Pattern.compile("^.*Dockerfile(\\..*)?$");

    public static Pattern SHELL_SCRIPT = Pattern.compile(".*\\.(sh|bash|zsh|ksh)$");

    public static Pattern HTML = Pattern.compile(".*\\.(html|htm)$");
    public static Pattern CSS = Pattern.compile(".*\\.css$");
    public static Pattern JS = Pattern.compile(".*\\.js$");
    public static Pattern JSON = Pattern.compile(".*\\.json$");
    public static Pattern XML = Pattern.compile(".*\\.xml$");
    public static Pattern WSDL = Pattern.compile(".*\\.wsdl$");
    public static Pattern PROPERTIES = Pattern.compile(".*\\.properties$");
    public static Pattern YAML = Pattern.compile(".*\\.(yaml|yml)$");

    public static Pattern ANY_KNOWN_TEXT = Pattern.compile(
            ".*(" +
                    "README\\.md" + "|" +
                    "pom\\.xml" + "|" +
                    "application\\.properties" + "|" +
                    "Dockerfile(\\..*)?" + "|" +
                    "\\.(java|sh|bash|zsh|ksh|html|htm|css|js|json|xml|wsdl|properties|yaml|yml)" +
                    ")$");
}
