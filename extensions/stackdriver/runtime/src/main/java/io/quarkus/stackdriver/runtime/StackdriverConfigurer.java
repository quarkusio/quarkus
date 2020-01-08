package io.quarkus.stackdriver.runtime;

import java.io.FileInputStream;
import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auth.oauth2.ServiceAccountCredentials;

import io.opencensus.exporter.trace.stackdriver.StackdriverTraceConfiguration;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceExporter;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.stackdriver.runtime.configuration.StackdriverConfiguration;

@ApplicationScoped
public class StackdriverConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger("StackdriverConfigurer");

    private StackdriverConfiguration stackdriverConfiguration;

    public void setStackdriverConfiguration(StackdriverConfiguration stackdriverConfiguration) {
        this.stackdriverConfiguration = stackdriverConfiguration;
    }

    void onStart(@Observes StartupEvent ev) throws IOException {
        LOGGER.info("The application is starting ..");
        StackdriverTraceConfiguration.Builder builder = StackdriverTraceConfiguration.builder();
        String projectId = stackdriverConfiguration.projectId;
        if (!projectId.isEmpty()) {
            builder.setProjectId(projectId);
        }
        builder.setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(
                stackdriverConfiguration.credentials)));
        StackdriverTraceExporter.createAndRegister(builder.build());
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOGGER.info("The application is stopping...");
        StackdriverTraceExporter.unregister();
    }

}
