package io.quarkus.qute.runtime.extensions;

import java.util.Locale;

import jakarta.enterprise.inject.Vetoed;

import io.quarkus.qute.TemplateExtension;

@Vetoed // Make sure no bean is created from this class
public class StringTemplateExtensions {

    static final String STR = "str";

    /**
     * E.g. {@code strVal.fmt(name,surname)}. The priority must be lower than
     * {@link #fmtInstance(String, String, Locale, Object...)}.
     *
     * @param format
     * @param ignoredPropertyName
     * @param args
     * @return the formatted value
     */
    @TemplateExtension(matchNames = { "fmt", "format" }, priority = 2)
    static String fmtInstance(String format, String ignoredPropertyName, Object... args) {
        return String.format(format, args);
    }

    /**
     * E.g. {@code strVal.format(locale,name)}. The priority must be higher than
     * {@link #fmtInstance(String, String, Object...)}.
     *
     * @param format
     * @param ignoredPropertyName
     * @param locale
     * @param args
     * @return the formatted value
     */
    @TemplateExtension(matchNames = { "fmt", "format" }, priority = 3)
    static String fmtInstance(String format, String ignoredPropertyName, Locale locale, Object... args) {
        return String.format(locale, format, args);
    }

    /**
     * E.g. {@cde str:fmt("Hello %s",name)}. The priority must be lower than {@link #fmt(String, Locale, String, Object...)}.
     *
     * @param ignoredPropertyName
     * @param format
     * @param args
     * @return the formatted value
     */
    @TemplateExtension(namespace = STR, matchNames = { "fmt", "format" }, priority = 2)
    static String fmt(String ignoredPropertyName, String format, Object... args) {
        return String.format(format, args);
    }

    /**
     * E.g. {@code str:fmt(locale,"Hello %s",name)}. The priority must be higher than {@link #fmt(String, String, Object...)}.
     *
     * @param ignoredPropertyName
     * @param locale
     * @param format
     * @param args
     * @return the formatted value
     */
    @TemplateExtension(namespace = STR, matchNames = { "fmt", "format" }, priority = 3)
    static String fmt(String ignoredPropertyName, Locale locale, String format, Object... args) {
        return String.format(locale, format, args);
    }

}
