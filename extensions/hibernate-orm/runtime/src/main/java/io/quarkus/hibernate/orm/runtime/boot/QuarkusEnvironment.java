package io.quarkus.hibernate.orm.runtime.boot;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.hibernate.cfg.Environment;
import org.hibernate.internal.CoreMessageLogger;
import org.jboss.logging.Logger;

/**
 * A lighter alternative to org.hibernate.cfg.Environment: we don't need it to look for certain environment variables,
 * and don't want it to copy all of {@link System#getProperties()} into the ORM configuration.
 * The only legacy behaviour we allow is to load the "/hibernate.properties" resource.
 */
public class QuarkusEnvironment {

    private static final Map GLOBAL_INITIAL_PROPERTIES;

    static {
        //Skip logging this as the original Environment initialization is also being triggered;
        //avoiding that will need ORM6.
        //Version.logVersion();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            InputStream stream = classLoader.getResourceAsStream("/hibernate.properties");
            if (stream != null) {
                final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, Environment.class.getName());
                LOG.warnf(
                        "The resource hibernate.properties was found in the root of your application. This configuration source is deprecated and will be removed in a future version of Quarkus, "
                                +
                                "as it's not compatible with Live Coding, multiple Persistence Units and many more cool features we have planned. Please refrain from "
                                +
                                "using it to configure your ORM and use the available configurations in application.properties (check the guide in https://quarkus.io/guides/hibernate-orm#quarkus-hibernate-orm_configuration). Should no configuration exist for your use case please report in: https://github.com/quarkusio/quarkus/issues/. Thank you! ");
                final Properties p = new Properties();
                try {
                    p.load(stream);
                } catch (Exception e) {
                    LOG.unableToLoadProperties();
                } finally {
                    try {
                        stream.close();
                    } catch (IOException ioe) {
                        LOG.unableToCloseStreamError(ioe);
                    }
                }
                GLOBAL_INITIAL_PROPERTIES = Collections.unmodifiableMap(p);
            } else {
                GLOBAL_INITIAL_PROPERTIES = Collections.EMPTY_MAP;
            }
        } else {
            GLOBAL_INITIAL_PROPERTIES = Collections.EMPTY_MAP;
        }
    }

    /**
     * @return
     * @see Environment#getProperties()
     */
    public static Map getInitialProperties() {
        return GLOBAL_INITIAL_PROPERTIES;
    }
}
