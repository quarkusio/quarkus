package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import java.io.IOException;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class JacksonSerializerFactory {

    private static final String SUPER_CLASS_NAME = "com.fasterxml.jackson.databind.ser.std.StdSerializer";
    private static final String JSON_GEN_CLASS_NAME = "com.fasterxml.jackson.core.JsonGenerator";

    final BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer;

    public JacksonSerializerFactory(BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer) {
        this.generatedClassBuildItemBuildProducer = generatedClassBuildItemBuildProducer;
    }

    public String create(ClassInfo classInfo) {
        String beanClassName = classInfo.name().toString();
        String generatedClassName = beanClassName + "$quarkusjacksonserializer";

        try (ClassCreator classCreator = new ClassCreator(
                new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true), generatedClassName, null,
                SUPER_CLASS_NAME)) {

            createConstructor(classCreator, beanClassName);

            createSerializeMethod(classInfo, classCreator, beanClassName);
        }

        return generatedClassName;
    }

    private static void createConstructor(ClassCreator classCreator, String beanClassName) {
        MethodCreator constructor = classCreator.getConstructorCreator(new String[0]);
        constructor.invokeSpecialMethod(
                MethodDescriptor.ofConstructor(SUPER_CLASS_NAME, "java.lang.Class"),
                constructor.getThis(), constructor.loadClass(beanClassName));
        constructor.returnVoid();
    }

    private void createSerializeMethod(ClassInfo classInfo, ClassCreator classCreator, String beanClassName) {
        MethodCreator serialize = classCreator.getMethodCreator("serialize", "void", "java.lang.Object", JSON_GEN_CLASS_NAME,
                "com.fasterxml.jackson.databind.SerializerProvider");
        serialize.setModifiers(ACC_PUBLIC);
        serialize.addException(IOException.class);

        ResultHandle valueHandle = serialize.checkCast(serialize.getMethodParam(0), beanClassName);
        ResultHandle jsonGenerator = serialize.getMethodParam(1);

        // jsonGenerator.writeStartObject();
        MethodDescriptor writeStartObject = MethodDescriptor.ofMethod(JSON_GEN_CLASS_NAME, "writeStartObject", "void");
        serialize.invokeVirtualMethod(writeStartObject, jsonGenerator);

        for (FieldInfo fieldInfo : classInfo.fields()) {
            String typeName = fieldInfo.type().name().toString();
            switch (typeName) {
                case "java.lang.String":
                case "int":
                case "long":
                case "float":
                case "double":
                    String writeMethodName = typeName.equals("java.lang.String") ? "writeStringField" : "writeNumberField";

                    MethodDescriptor readString = MethodDescriptor.ofMethod(beanClassName,
                            getterMethodName(classInfo, fieldInfo), typeName);
                    MethodDescriptor writeField = MethodDescriptor.ofMethod(JSON_GEN_CLASS_NAME, writeMethodName, "void",
                            "java.lang.String", typeName);
                    serialize.invokeVirtualMethod(writeField, jsonGenerator, serialize.load(fieldInfo.name()),
                            serialize.invokeVirtualMethod(readString, valueHandle));
                    break;
            }
        }

        // jsonGenerator.writeEndObject();
        MethodDescriptor writeEndObject = MethodDescriptor.ofMethod(JSON_GEN_CLASS_NAME, "writeEndObject", "void");
        serialize.invokeVirtualMethod(writeEndObject, jsonGenerator);

        serialize.returnVoid();
    }

    private String getterMethodName(ClassInfo classInfo, FieldInfo fieldInfo) {
        if (classInfo.method(fieldInfo.name()) != null) {
            return fieldInfo.name();
        }
        return "get" + ucFirst(fieldInfo.name());
    }

    public String ucFirst(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
