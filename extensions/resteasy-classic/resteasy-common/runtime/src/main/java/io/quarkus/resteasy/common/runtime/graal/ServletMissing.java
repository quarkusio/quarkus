package io.quarkus.resteasy.common.runtime.graal;

import java.util.function.BooleanSupplier;

/**
 * Checks if servlet classes are available in the classpath and a suitable implementation. This is required because
 * ConfigSource's provided by RESTEasy, need the servlet classes to function properly. If these classes are not found,
 * we rewrite the sources to remove any references to the servlet classes and the internal RESTEasy sources in native
 * mode.
 *
 * The offending RESTEasy sources are:
 * {@link org.jboss.resteasy.microprofile.config.ServletConfigSource}
 * {@link org.jboss.resteasy.microprofile.config.ServletContextConfigSource}
 * {@link org.jboss.resteasy.microprofile.config.FilterConfigSource}
 *
 * The main instances sources are still part of the Config instance, but without the backing implementations, the source
 * do not return any values.
 *
 * Ideally, this should be fixed in RESTEasy.
 *
 * See https://github.com/quarkusio/quarkus/issues/5492
 * See https://github.com/quarkusio/quarkus/issues/9086
 * See https://github.com/quarkusio/quarkus/issues/14876
 */
public final class ServletMissing implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName("javax.servlet.ServletConfig");
            Class.forName("javax.servlet.ServletContext");
            Class.forName("javax.servlet.FilterConfig");
            Class.forName("io.quarkus.undertow.runtime.UndertowDeploymentRecorder");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
