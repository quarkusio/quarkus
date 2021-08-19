package io.quarkus.undertow.deployment;

import static io.quarkus.runtime.configuration.ConverterSupport.DEFAULT_QUARKUS_CONVERTER_PRIORITY;

import java.util.Optional;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.ConvertWith;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class ServletConfig {

    /**
     * The context path for Servlet content. This will determine the path used to
     * resolve all Servlet-based resources, including JAX-RS resources - when using the Undertow extension in conjunction with
     * RESTEasy.
     * <p>
     * This path is specified with a leading {@literal /}, but is resolved relative
     * to {@literal quarkus.http.root-path}.
     * <ul>
     * <li>If {@literal quarkus.http.root-path=/} and {@code quarkus.servlet.context-path=/bar}, the servlet path will be
     * {@literal /bar}</li>
     * <li>If {@literal quarkus.http.root-path=/foo} and {@code quarkus.servlet.context-path=/bar}, the servlet path will be
     * {@literal /foo/bar}</li>
     * </ul>
     */
    @ConfigItem
    @ConvertWith(ContextPathConverter.class)
    Optional<String> contextPath;

    /**
     * The default charset to use for reading and writing requests
     */
    @ConfigItem(defaultValue = "UTF-8")
    public String defaultCharset;

    /**
     * This converter adds a '/' at the beginning of the context path but does not add one at the end, given we want to support
     * binding to a context without an ending '/'.
     * <p>
     * See ContextPathTestCase for an example.
     */
    @Priority(DEFAULT_QUARKUS_CONVERTER_PRIORITY)
    public static class ContextPathConverter implements Converter<String> {

        private static final String SLASH = "/";

        @Override
        public String convert(String value) throws IllegalArgumentException, NullPointerException {
            if (value == null) {
                return SLASH;
            }

            value = value.trim();
            if (SLASH.equals(value)) {
                return value;
            }
            if (!value.startsWith(SLASH)) {
                value = SLASH + value;
            }

            return value;
        }
    }

}
