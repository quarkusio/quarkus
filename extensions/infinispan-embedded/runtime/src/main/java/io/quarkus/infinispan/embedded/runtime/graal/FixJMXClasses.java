package io.quarkus.infinispan.embedded.runtime.graal;

import java.util.Properties;

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
        // Don't do anything or do we want to?
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

@TargetClass(GlobalJmxStatisticsConfiguration.class)
final class SubstituteGlobalJmxStatisticsConfiguration {
    @Substitute
    public boolean enabled() {
        return false;
    }
}
