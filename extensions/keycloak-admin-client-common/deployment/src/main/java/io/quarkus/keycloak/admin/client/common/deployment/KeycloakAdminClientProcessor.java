package io.quarkus.keycloak.admin.client.common.deployment;

import java.lang.reflect.Modifier;

import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.gizmo.ClassTransformer;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.keycloak.admin.client.common.runtime.KeycloakAdminClientJsonMapper;
import tools.jackson.databind.json.JsonMapper;

public class KeycloakAdminClientProcessor {

    private static final String KEYCLOAK_JSON_MAPPER_SPI = "org.keycloak.json.KeycloakJsonMapper";
    private static final String KEYCLOAK_RAW_JSON_VALUE_SUPPORT_SPI = "org.keycloak.json.RawJsonValueSupport";
    private static final String KEYCLOAK_MAPPER_HOLDER = "org.keycloak.admin.client.jackson3.Jackson3MapperHolder";
    private static final String KEYCLOAK_MAPPER_FIELD = "MAPPER";

    @BuildStep
    public void nativeImage(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit,
            BuildProducer<ServiceProviderBuildItem> serviceProviders,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        runtimeInit.produce(new RuntimeInitializedClassBuildItem("org.keycloak.common.util.SecretGenerator"));

        // the Keycloak representations resolve their JSON mapper (and the RawJsonValue support) through the ServiceLoader
        serviceProviders.produce(ServiceProviderBuildItem.allProvidersFromClassPath(KEYCLOAK_JSON_MAPPER_SPI));
        serviceProviders.produce(ServiceProviderBuildItem.allProvidersFromClassPath(KEYCLOAK_RAW_JSON_VALUE_SUPPORT_SPI));

        // Jackson 3 (de)serializers instantiated reflectively through the annotation introspector
        // of the Keycloak admin client mapper, see org.keycloak.admin.client.jackson3.KeycloakAnnotationIntrospector3
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.keycloak.admin.client.jackson3.StringOrArrayDeserializer3",
                "org.keycloak.admin.client.jackson3.StringOrArraySerializer3",
                "org.keycloak.admin.client.jackson3.StringListMapDeserializer3",
                "org.keycloak.admin.client.jackson3.MultivaluedHashMapValueDeserializer3",
                "org.keycloak.admin.client.jackson3.MultivaluedHashMapValueSerializer3",
                "org.keycloak.admin.client.jackson3.ResourceTypeMapDeserializer3")
                .reason(getClass().getName())
                .constructors().build());
    }

    /**
     * Registers the {@link KeycloakAdminClientJsonMapper} service with the mapper of the Keycloak admin client, unless
     * the Keycloak authorization extension is present: that extension brings the Jackson 2 variant of the Keycloak
     * representations, makes it win on the classpath and registers the service with a mapper handling it.
     */
    @BuildStep
    void jsonMapper(ServiceRegistrar serviceRegistrar, Capabilities capabilities) {
        if (capabilities.isPresent(Capability.KEYCLOAK_AUTHORIZATION)) {
            return;
        }
        serviceRegistrar
                .forService(KeycloakAdminClientJsonMapper.class)
                .atPhase(Phase.INIT)
                .onStart(ctx -> KeycloakAdminClientJsonMapper.keycloak());
    }

    /**
     * Implements {@link KeycloakAdminClientJsonMapper#keycloakMapper()}: the mapper of the Keycloak admin client is held
     * by the package-private {@code Jackson3MapperHolder}, so the holder and its field are made public and the native
     * method is replaced with one returning the field.
     * <p>
     * TODO: drop once the holder is public in Keycloak
     */
    @BuildStep
    void keycloakJsonMapperAccess(BuildProducer<BytecodeTransformerBuildItem> transformers) {
        FieldDescriptor mapperField = FieldDescriptor.of(KEYCLOAK_MAPPER_HOLDER, KEYCLOAK_MAPPER_FIELD, JsonMapper.class);
        transformers.produce(new BytecodeTransformerBuildItem(KEYCLOAK_MAPPER_HOLDER, (className, classVisitor) -> {
            ClassTransformer transformer = new ClassTransformer(className);
            transformer.addModifiers(Modifier.PUBLIC);
            transformer.modifyField(mapperField).addModifiers(Modifier.PUBLIC);
            return transformer.applyTo(classVisitor);
        }));
        transformers.produce(new BytecodeTransformerBuildItem(KeycloakAdminClientJsonMapper.class.getName(),
                (className, classVisitor) -> {
                    ClassTransformer transformer = new ClassTransformer(className);
                    MethodDescriptor keycloakMapper = MethodDescriptor.ofMethod(KeycloakAdminClientJsonMapper.class,
                            "keycloakMapper", JsonMapper.class);
                    // the native method is removed first as methods are created in 'visitEnd'
                    // while the method is removed by simply being ignored in 'visitMethod'
                    transformer.removeMethod(keycloakMapper);
                    try (MethodCreator method = transformer.addMethod(keycloakMapper)) {
                        method.setModifiers(Modifier.STATIC);
                        method.returnValue(method.readStaticField(mapperField));
                    }
                    return transformer.applyTo(classVisitor);
                }));
    }
}
