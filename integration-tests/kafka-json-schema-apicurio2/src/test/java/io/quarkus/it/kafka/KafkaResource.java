package io.quarkus.it.kafka;

import java.util.Collections;
import java.util.Map;

import io.quarkus.it.kafka.jsonschema.JsonSchemaKafkaCreator;
import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KafkaResource implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {

    JsonSchemaKafkaCreator creator;

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        Map<String, String> devServicesProperties = context.devServicesProperties();
        String bootstrapServers = devServicesProperties.get("kafka.bootstrap.servers");
        if (bootstrapServers != null) {
            String apicurioUrl = devServicesProperties.get("mp.messaging.connector.smallrye-kafka.apicurio.registry.url");
            String confluentUrl = devServicesProperties.get("mp.messaging.connector.smallrye-kafka.schema.registry.url");
            creator = new JsonSchemaKafkaCreator(bootstrapServers, apicurioUrl, confluentUrl);
        }
    }

    @Override
    public Map<String, String> start() {
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(
                creator,
                new TestInjector.MatchesType(JsonSchemaKafkaCreator.class));
    }
}
