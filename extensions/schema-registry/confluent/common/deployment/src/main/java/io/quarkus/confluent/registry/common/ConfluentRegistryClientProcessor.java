package io.quarkus.confluent.registry.common;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;

public class ConfluentRegistryClientProcessor {

    @BuildStep
    public void confluentRegistryClient(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ServiceProviderBuildItem> serviceProviders,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) {

        reflectiveClass
                .produce(new ReflectiveClassBuildItem(true, false, false,
                        "io.confluent.kafka.serializers.context.NullContextNameStrategy"));

        reflectiveClass
                .produce(new ReflectiveClassBuildItem(true, true, false,
                        "io.confluent.kafka.serializers.subject.TopicNameStrategy",
                        "io.confluent.kafka.serializers.subject.TopicRecordNameStrategy",
                        "io.confluent.kafka.serializers.subject.RecordNameStrategy"));

        reflectiveClass
                .produce(new ReflectiveClassBuildItem(true, true, false,
                        "io.confluent.kafka.schemaregistry.client.rest.entities.ErrorMessage",
                        "io.confluent.kafka.schemaregistry.client.rest.entities.Schema",
                        "io.confluent.kafka.schemaregistry.client.rest.entities.Config",
                        "io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference",
                        "io.confluent.kafka.schemaregistry.client.rest.entities.SchemaString",
                        "io.confluent.kafka.schemaregistry.client.rest.entities.SchemaTypeConverter",
                        "io.confluent.kafka.schemaregistry.client.rest.entities.ServerClusterId",
                        "io.confluent.kafka.schemaregistry.client.rest.entities.SujectVersion"));

        reflectiveClass
                .produce(new ReflectiveClassBuildItem(true, true, false,
                        "io.confluent.kafka.schemaregistry.client.rest.entities.requests.CompatibilityCheckResponse",
                        "io.confluent.kafka.schemaregistry.client.rest.entities.requests.ConfigUpdateRequest",
                        "io.confluent.kafka.schemaregistry.client.rest.entities.requests.ModeGetResponse",
                        "io.confluent.kafka.schemaregistry.client.rest.entities.requests.ModeUpdateRequest",
                        "io.confluent.kafka.schemaregistry.client.rest.entities.requests.RegisterSchemaRequest",
                        "io.confluent.kafka.schemaregistry.client.rest.entities.requests.RegisterSchemaResponse"));

        serviceProviders
                .produce(new ServiceProviderBuildItem(
                        "io.confluent.kafka.schemaregistry.client.security.basicauth.BasicAuthCredentialProvider",
                        "io.confluent.kafka.schemaregistry.client.security.basicauth.SaslBasicAuthCredentialProvider",
                        "io.confluent.kafka.schemaregistry.client.security.basicauth.UrlBasicAuthCredentialProvider",
                        "io.confluent.kafka.schemaregistry.client.security.basicauth.UserInfoCredentialProvider"));
    }
}
