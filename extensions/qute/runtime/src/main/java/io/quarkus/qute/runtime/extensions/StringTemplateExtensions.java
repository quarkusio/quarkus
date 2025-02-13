package io.quarkus.qute.runtime.extensions;

import static io.quarkus.qute.TemplateExtension.ANY;

import java.util.Locale;
import java.util.Objects;

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

    @TemplateExtension(matchName = "+")
    static String plus(String str, Object val) {
        return str + val;
    }

    /**
     * E.g. {@code str:concat("Hello ",name)}. The priority must be lower than {@link #fmt(String, String, Object...)}.
     *
     * @param args
     */
    @TemplateExtension(namespace = STR, priority = 1)
    static String concat(Object... args) {
        StringBuilder b = new StringBuilder(args.length * 10);
        for (Object obj : args) {
            b.append(obj.toString());
        }
        return b.toString();
    }

    /**
     * E.g. {@code str:join("_", "Hello",name)}. The priority must be lower than {@link #concat(Object...)}.
     *
     * @param delimiter
     * @param args
     */
    @TemplateExtension(namespace = STR, priority = 0)
    static String join(String delimiter, Object... args) {
        CharSequence[] elements = new CharSequence[args.length];
        for (int i = 0; i < args.length; i++) {
            elements[i] = args[i].toString();
        }
        return String.join(delimiter, elements);
    }

    /**
     * E.g. {@code str:builder}. The priority must be lower than {@link #join(String, Object...)}.
     */
    @TemplateExtension(namespace = STR, priority = -1)
    static StringBuilder builder() {
        return new StringBuilder();
    }

    /**
     * E.g. {@code str:builder('Hello')}. The priority must be lower than {@link #builder()}.
     */
    @TemplateExtension(namespace = STR, priority = -2)
    static StringBuilder builder(Object val) {
        return new StringBuilder(Objects.toString(val));
    }

    /**
     * E.g. {@code str:['Foo and bar']}. The priority must be lower than any other {@code str:} resolver.
     *
     * @param name
     */
    @TemplateExtension(namespace = STR, priority = -10, matchName = ANY)
    static String self(String name) {
        return name;
    }

    @TemplateExtension(matchName = "+")
    static StringBuilder plus(StringBuilder builder, Object val) {
        return builder.append(val);
    }

}
