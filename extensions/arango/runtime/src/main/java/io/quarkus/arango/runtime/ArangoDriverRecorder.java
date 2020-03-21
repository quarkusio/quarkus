package io.quarkus.arango.runtime;

import java.util.*;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import com.arangodb.ArangoDB;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ArangoDriverRecorder {

    private static final Logger log = Logger.getLogger(ArangoDriverRecorder.class);

    public void configureArangoProducer(BeanContainer beanContainer, ArangoConfiguration configuration,
            ShutdownContext shutdownContext) {

        ArangoDB driver = initializeDriver(configuration, shutdownContext);

        ArangoDriverProducer driverProducer = beanContainer.instance(ArangoDriverProducer.class);
        driverProducer.initialize(driver);
    }

    private ArangoDB initializeDriver(ArangoConfiguration configuration,
            ShutdownContext shutdownContext) {

        ArangoDB.Builder builder = new ArangoDB.Builder();
        buildUris(configuration.uri).forEach(builder::host);
        builder.acquireHostList(configuration.acquireHostList);
        builder.acquireHostListInterval(configuration.acquireHostListInterval);
        builder.user(configuration.user);

        if (!configuration.password.isEmpty()) {
            builder.password(configuration.password);
        }
        builder.timeout(configuration.timeout);
        builder.maxConnections(configuration.maxConnections);
        ArangoDB driver = builder.build();
        shutdownContext.addLastShutdownTask(driver::shutdown);

        return driver;
    }

    private Map<String, Integer> buildUris(String uri) {
        Map<String, Integer> hosts = new HashMap<>();
        String[] urls = uri.split(",");

        Stream.of(urls).forEach(url -> {
            String[] hostPort = url.split(":");

            String ip = hostPort[0];
            String port = hostPort[1];
            if (log.isDebugEnabled()) {
                log.debugf("Arango url:{}, port:{}", ip, port);
            }
            hosts.put(ip, Integer.parseInt(port));
        });

        return hosts;
    }
}
