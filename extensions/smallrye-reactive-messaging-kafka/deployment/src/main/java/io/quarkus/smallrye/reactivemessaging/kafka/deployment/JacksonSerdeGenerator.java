package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import org.jboss.jandex.Type;

import io.quarkus.deployment.GeneratedClassGizmo2Adaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.GeneratedServiceProviderBuildItem;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import io.quarkus.runtime.util.HashUtil;

public class JacksonSerdeGenerator {

    private JacksonSerdeGenerator() {
        // Avoid direct instantiation
    }

    public static String generateSerializer(BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<GeneratedServiceProviderBuildItem> generatedServiceProviders, Type type) {
        var classOutput = new GeneratedClassGizmo2Adaptor(generatedClass, generatedResources, generatedServiceProviders, true);
        var gizmo = Gizmo.create(classOutput);
        String baseName = type.name().withoutPackagePrefix();
        String out = baseName + "_Serializer_" + HashUtil.sha1(type.name().toString());
        String className = type.name().packagePrefix() + "." + out;
        gizmo.class_(className, cc -> {
            cc.extends_(ObjectMapperSerializer.class);
            cc.defaultConstructor();
        });
        return className;
    }

    public static String generateDeserializer(BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<GeneratedServiceProviderBuildItem> generatedServiceProviders, Type type) {
        var classOutput = new GeneratedClassGizmo2Adaptor(generatedClass, generatedResources, generatedServiceProviders, true);
        var gizmo = Gizmo.create(classOutput);
        String baseName = type.name().withoutPackagePrefix();
        String out = baseName + "_Deserializer_" + HashUtil.sha1(type.name().toString());
        String className = type.name().packagePrefix() + "." + out;
        gizmo.class_(className, cc -> {
            cc.extends_(ObjectMapperDeserializer.class);
            cc.constructor(ctc -> {
                ctc.public_();
                ctc.body(bc -> {
                    ConstructorDesc superCtor = ConstructorDesc.of(ObjectMapperDeserializer.class, Class.class);
                    bc.invokeSpecial(superCtor, cc.this_(),
                            bc.classForName(Const.of(type.name().toString())));
                    bc.return_();
                });
            });
        });
        return className;
    }
}
