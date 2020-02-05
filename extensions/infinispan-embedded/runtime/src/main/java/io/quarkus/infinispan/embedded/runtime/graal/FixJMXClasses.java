package io.quarkus.infinispan.embedded.runtime.graal;

import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalJmxStatisticsConfiguration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.Parser;
import org.infinispan.configuration.parsing.XMLExtendedStreamReader;
import org.infinispan.factories.GlobalComponentRegistry;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

public class FixJMXClasses {
}

@TargetClass(GlobalComponentRegistry.class)
final class SubstituteGlobalComponentRegistry {
    @Substitute
    private boolean isMBeanServerRunning() {
        return false;
    }

    @Substitute
    protected synchronized void addShutdownHook() {
        // Don't install any shutdown hook
    }
}

@TargetClass(Parser.class)
final class SubstituteParser {
    @Substitute
    private void parseJmx(XMLExtendedStreamReader reader, ConfigurationBuilderHolder holder) {
        // Ignore JMX configuration - but we need to skip to next element
        parseProperties(reader);
    }

    @Alias
    public static Properties parseProperties(final XMLExtendedStreamReader reader) {
        return null;
    }
}

@TargetClass(GlobalConfiguration.class)
final class SubstituteGlobalConfiguration {
    @Substitute
    public boolean statistics() {
        return false;
    }
}

@TargetClass(GlobalJmxStatisticsConfiguration.class)
final class SubstituteGlobalJmxStatisticsConfiguration {
    @Substitute
    public boolean enabled() {
        return false;
    }
}

@TargetClass(className = "org.infinispan.jmx.AbstractJmxRegistration")
final class SubstituteAbstractJmxRegistration {
    @Substitute
    public void start() {
        // Do nothing
    }

    @Substitute
    public void stop() {
        // Do nothing
    }

    @Substitute
    public final String getDomain() {
        return "";
    }

    @Substitute
    public final String getGroupName() {
        return "";
    }

    @Substitute
    public final MBeanServer getMBeanServer() {
        return null;
    }

    @Substitute
    public ObjectName registerExternalMBean(Object managedComponent, String groupName) throws Exception {
        return null;
    }

    @Substitute
    public void registerMBean(Object managedComponent) throws Exception {
        // Do nothing
    }

    @Substitute
    public void registerMBean(Object managedComponent, String groupName) throws Exception {
        // Do nothing
    }

    @Substitute
    public void unregisterMBean(ObjectName objectName) throws Exception {
        // Do nothing
    }
}
