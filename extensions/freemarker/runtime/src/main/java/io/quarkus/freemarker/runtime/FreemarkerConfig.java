package io.quarkus.freemarker.runtime;

import java.util.List;
import java.util.Optional;

import freemarker.template.TemplateExceptionHandler;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "freemarker", phase = ConfigPhase.RUN_TIME)
public class FreemarkerConfig {

    /**
     * Comma-separated of file system paths where freemarker templates are located
     */
    @ConfigItem
    public Optional<List<String>> filePaths;

    /**
     * Set the preferred charset template files are stored in.
     */
    @ConfigItem
    public Optional<String> defaultEncoding;

    /**
     * Sets how errors will appear. rethrow, debug, html-debug, ignore.
     * 
     * @see freemarker.template.Configuration#setTemplateExceptionHandler(TemplateExceptionHandler)
     */
    @ConfigItem
    public Optional<String> templateExceptionHandler;

    /**
     * If false, don't log exceptions inside FreeMarker that it will be thrown at you anyway.
     * 
     * @see freemarker.template.Configuration#setLogTemplateExceptions(boolean)
     */
    @ConfigItem
    public Optional<Boolean> logTemplateExceptions;

    /**
     * Wrap unchecked exceptions thrown during template processing into TemplateException-s.
     * 
     * @see freemarker.template.Configuration#setWrapUncheckedExceptions(boolean)
     */
    @ConfigItem
    public Optional<Boolean> wrapUncheckedExceptions;

    /**
     * If false, do not fall back to higher scopes when reading a null loop variable.
     * 
     * @see freemarker.template.Configuration#setFallbackOnNullLoopVariable(boolean)
     */
    @ConfigItem
    public Optional<Boolean> fallbackOnNullLoopVariable;

    /**
     * The string value for the boolean {@code true} and {@code false} values, usually intended for human consumption (not for a
     * computer language), separated with comma.
     * 
     * @see freemarker.template.Configuration#setBooleanFormat(String)
     */
    @ConfigItem
    public Optional<String> booleanFormat;

    /**
     * Sets the default number format used to convert numbers to strings.
     * 
     * @see freemarker.template.Configuration#setNumberFormat(String)
     */
    @ConfigItem
    public Optional<String> numberFormat;

    /**
     * If true, the object wrapper will be configured to expose fields.
     * 
     * @see freemarker.ext.beans.BeansWrapper#setExposeFields(boolean)
     */
    @ConfigItem
    public Optional<Boolean> objectWrapperExposeFields;

}
