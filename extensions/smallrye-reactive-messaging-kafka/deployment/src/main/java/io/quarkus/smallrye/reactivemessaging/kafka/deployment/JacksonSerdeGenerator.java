package io.quarkus.smallrye.reactivemessaging.kafka.deployment;

import java.util.UUID;

import org.jboss.jandex.Type;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import io.quarkus.runtime.util.HashUtil;

public class JacksonSerdeGenerator {

    private JacksonSerdeGenerator() {
        // Avoid direct instantiation
    }

    public static String generateSerializer(BuildProducer<GeneratedClassBuildItem> generatedClass, Type type) {
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);
        String baseName = type.name().withoutPackagePrefix();
        String targetPackage = io.quarkus.arc.processor.DotNames.internalPackageNameWithTrailingSlash(type.name());
        String out = baseName + "_Serializer_" + HashUtil.sha1(UUID.randomUUID().toString());
        String generatedName = targetPackage + out;
        ClassCreator creator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .superClass(ObjectMapperSerializer.class).build();
        creator.close();
        return type.name().packagePrefix() + "." + out;
    }

    public static String generateDeserializer(BuildProducer<GeneratedClassBuildItem> generatedClass, Type type) {
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);
        String baseName = type.name().withoutPackagePrefix();
        String targetPackage = io.quarkus.arc.processor.DotNames.internalPackageNameWithTrailingSlash(type.name());
        String out = baseName + "_Deserializer_"
                + HashUtil.sha1(Long.toString(UUID.randomUUID().getMostSignificantBits()));
        String generatedName = targetPackage + out;
        ClassCreator creator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .superClass(ObjectMapperDeserializer.class).build();
        MethodCreator constructor = creator.getMethodCreator("<init>", void.class);
        MethodDescriptor superConstructor = MethodDescriptor.ofConstructor(ObjectMapperDeserializer.class, Class.class);
        constructor.invokeSpecialMethod(superConstructor, constructor.getThis(),
                constructor.loadClassFromTCCL(type.name().toString()));
        constructor.returnValue(null);
        constructor.close();
        creator.close();
        return type.name().packagePrefix() + "." + out;
    }
}
