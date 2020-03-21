package io.quarkus.arango.runtime;

import com.arangodb.ArangoDB;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.ssl.SslContextConfiguration;
import org.graalvm.nativeimage.ImageInfo;
import org.jboss.logging.Logger;
import org.neo4j.driver.*;

import java.util.logging.Level;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

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


        String[] hostPorts = configuration.uri.split(":");
        String hostPort = hostPorts[1];
        ArangoDB arangoDB = builder.host(hostPorts[0], Integer.parseInt(hostPort)).acquireHostList(true).build();
        shutdownContext.addLastShutdownTask(arangoDB::shutdown);
        ;
        return arangoDB;
    }
}
