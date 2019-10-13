package io.quarkus.it.infinispan.embedded;

import java.lang.management.ManagementFactory;
import java.util.Properties;

import javax.management.MBeanServer;

import org.infinispan.commons.jmx.MBeanServerLookup;

// Here to test a custom mbean lookup configured via XML
// Note this class will not be used during native as JMX is not supported
public class CustomMBeanServerLookup implements MBeanServerLookup {

    @Override
    public MBeanServer getMBeanServer(Properties properties) {
        return ManagementFactory.getPlatformMBeanServer();
    }
}
