package io.quarkus.camel.servlet.runtime;

import java.util.List;
import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * {@link ConfigRoot} for {@link #defaultServlet}.
 */
@ConfigRoot(name = "camel", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class CamelServletConfig {

    /** {@code camel-servlet} component configuration */
    public ServletsConfig servlet;

    @ConfigGroup
    public static class ServletsConfig {

        /** The default servlet with implicit name {@value ServletConfig.DEFAULT_SERVLET_NAME} */
        @ConfigItem(name = ConfigItem.PARENT)
        public ServletConfig defaultServlet;

        /** A collection of named servlets */
        @ConfigItem(name = ConfigItem.PARENT)
        public Map<String, ServletConfig> namedServlets;

    }

    /** {@code camel-servlet} component configuration */
    @ConfigGroup
    public static class ServletConfig {

        public static final String DEFAULT_SERVLET_NAME = "CamelServlet";
        public static final String DEFAULT_SERVLET_CLASS = "org.apache.camel.component.servlet.CamelHttpTransportServlet";

        /**
         * A comma separated list of path patterns under which the CamelServlet should be accessible. Example path
         * patterns: {@code /*}, {@code /services/*}
         */
        @ConfigItem
        public List<String> urlPatterns;

        /** A fully qualified name of a servlet class to serve paths that match {@link #urlPatterns} */
        @ConfigItem(defaultValue = DEFAULT_SERVLET_CLASS)
        public String servletClass;

        /**
         * A servletName as it would be defined in a `web.xml` file or in the
         * {@link javax.servlet.annotation.WebServlet#name()} annotation.
         */
        @ConfigItem(defaultValue = DEFAULT_SERVLET_NAME)
        public String servletName;

        /**
         * @return {@code true} if this {@link ServletConfig} is valid as a whole. This currently translates to
         *         {@link #urlPatterns} being non-empty because {@link #servletClass} and {@link #servletName} have
         *         default values. Otherwise returns {@code false}.
         */
        public boolean isValid() {
            return !urlPatterns.isEmpty();
        }

        /**
         * Setting the servlet name is possible both via {@link #servletName} and the key in the
         * {@link io.quarkus.camel.servlet.runtime.CamelServletConfig.ServletsConfig#namedServlets} map. This method
         * sets the precedence: the {@link #servletName} gets effective only if it has a non-default value; otherwise
         * the {@code key} is returned as the servlet name.
         *
         * @param key the key used in
         *        {@link io.quarkus.camel.servlet.runtime.CamelServletConfig.ServletsConfig#namedServlets}
         * @return the effective servlet name to use
         */
        public String getEffectiveServletName(final String key) {
            return DEFAULT_SERVLET_NAME.equals(servletName) ? key : servletName;
        }

    }

}
