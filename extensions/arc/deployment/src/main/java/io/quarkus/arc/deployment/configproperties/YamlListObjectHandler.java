package io.quarkus.arc.deployment.configproperties;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.smallrye.config.SmallRyeConfig;

/**
 * Class used to handle all the plumbing needed to support fields with types like {@code List<SomeClass>}
 * values for which can only be provided in YAML.
 *
 * The basic idea for handling these fields is to convert the string value of the field (which is SR Config
 * populates with the "serialized" value of the field) using SnakeYAML.
 * To achieve that various intermediate classes and Yaml configuration need to be generated.
 */
class YamlListObjectHandler {

    private static final String ABSTRACT_YAML_CONVERTER_CNAME = "io.quarkus.config.yaml.runtime.AbstractYamlObjectConverter";

    private final ClassOutput classOutput;
    private final IndexView index;
    private final BuildProducer<ReflectiveClassBuildItem> reflectiveClasses;

    public YamlListObjectHandler(ClassOutput classOutput, IndexView index,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        this.classOutput = classOutput;
        this.index = index;
        this.reflectiveClasses = reflectiveClasses;
    }

    public ResultHandle handle(Member member, MethodCreator configPopulator, ResultHandle configObject,
            String configName, String fullConfigName) {
        ClassInfo classInfo = validateType(member.type());
        validateClass(classInfo, member);
        // these need to be registered for reflection because SnakeYaml used reflection to instantiate them
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, classInfo.name().toString()));

        String wrapperClassName = classInfo.name().toString() + "_GeneratedListWrapper_" + configName;

        // generate a class that has a List field and getters and setter which have the proper generic type
        // this way SnakeYaml can properly populate the field
        MethodDescriptor getterDesc;
        try (ClassCreator cc = ClassCreator.builder().classOutput(classOutput)
                .className(wrapperClassName)
                .build()) {
            FieldDescriptor fieldDesc = cc.getFieldCreator(configName, List.class).setModifiers(Modifier.PRIVATE)
                    .getFieldDescriptor();

            MethodCreator getter = cc.getMethodCreator(JavaBeanUtil.getGetterName(configName, classInfo.name()), List.class);
            getter.setSignature(String.format("()Ljava/util/List<L%s;>;", forSignature(classInfo)));
            getterDesc = getter.getMethodDescriptor();
            getter.returnValue(getter.readInstanceField(fieldDesc, getter.getThis()));

            MethodCreator setter = cc.getMethodCreator(JavaBeanUtil.getSetterName(configName), void.class, List.class);
            setter.setSignature(String.format("(Ljava/util/List<L%s;>;)V", forSignature(classInfo)));
            setter.writeInstanceField(fieldDesc, setter.getThis(), setter.getMethodParam(0));
            setter.returnValue(null);
        }
        // we always generate getters and setters, so reflection is only needed on the methods
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, wrapperClassName));

        // generate an MP-Config converter which looks something like this
        // public class GeneratedInputsConverter extends io.quarkus.config.yaml.runtime.AbstractYamlObjectConverter<SomeClass_GeneratedListWrapper_fieldName> {
        //
        //     @Override
        //     Map<String, String> getFieldNameMap() {
        //         Map<String, String> result = new HashMap<>();
        //         result.put("some_field", "someField");
        //         return result;
        //     }
        //
        //     @Override
        //     Class<SomeClass_GeneratedListWrapper_fieldName> getClazz() {
        //         return SomeClass_GeneratedListWrapper_fieldName.class;
        //     }
        // }
        String wrapperConverterClassName = wrapperClassName + "_Converter";
        try (ClassCreator cc = ClassCreator.builder().classOutput(classOutput)
                .className(wrapperConverterClassName)
                .superClass(ABSTRACT_YAML_CONVERTER_CNAME)
                .signature(
                        String.format("L%s<L%s;>;", ABSTRACT_YAML_CONVERTER_CNAME.replace('.', '/'),
                                wrapperClassName.replace('.', '/')))
                .build()) {
            MethodCreator getClazz = cc.getMethodCreator("getClazz", Class.class).setModifiers(Modifier.PROTECTED);
            getClazz.returnValue(getClazz.loadClass(wrapperClassName));

            // generate the getFieldNameMap method by searching for fields annotated with @ConfigProperty
            List<AnnotationInstance> configPropertyInstances = classInfo.annotations().get(DotNames.CONFIG_PROPERTY);
            Map<String, String> fieldNameMap = new HashMap<>();
            if (configPropertyInstances != null) {
                for (AnnotationInstance instance : configPropertyInstances) {
                    if (instance.target().kind() != AnnotationTarget.Kind.FIELD) {
                        continue;
                    }
                    AnnotationValue nameValue = instance.value("name");
                    if (nameValue != null) {
                        String nameValueStr = nameValue.asString();
                        if ((nameValueStr != null) && !nameValueStr.isEmpty()) {
                            String annotatedFieldName = instance.target().asField().name();
                            fieldNameMap.put(nameValueStr, annotatedFieldName);
                        }
                    }
                }
            }
            if (!fieldNameMap.isEmpty()) {
                MethodCreator getFieldNameMap = cc.getMethodCreator("getFieldNameMap", Map.class);
                ResultHandle resultHandle = getFieldNameMap.newInstance(MethodDescriptor.ofConstructor(HashMap.class));
                for (Map.Entry<String, String> entry : fieldNameMap.entrySet()) {
                    getFieldNameMap.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(HashMap.class, "put", Object.class, Object.class, Object.class),
                            resultHandle, getFieldNameMap.load(entry.getKey()), getFieldNameMap.load(entry.getValue()));
                }
                getFieldNameMap.returnValue(resultHandle);
            }
        }

        // use the generated converter to convert the string value into the wrapper
        ResultHandle smallryeConfig = configPopulator.checkCast(configObject, SmallRyeConfig.class);
        ResultHandle getValueHandle = configPopulator.invokeVirtualMethod(
                MethodDescriptor.ofMethod(SmallRyeConfig.class, "getValue", Object.class, String.class, Converter.class),
                smallryeConfig,
                configPopulator.load(fullConfigName),
                configPopulator.newInstance(MethodDescriptor.ofConstructor(wrapperConverterClassName)));
        ResultHandle wrapperHandle = configPopulator.checkCast(getValueHandle, wrapperClassName);
        //pull the actual value out of the wrapper
        return configPopulator.invokeVirtualMethod(getterDesc, wrapperHandle);
    }

    private void validateClass(ClassInfo classInfo, Member member) {
        if (Modifier.isInterface(classInfo.flags())) {
            throw new IllegalArgumentException(
                    "The use of interfaces as the generic type of Lists fields / methods is not allowed. Offending field is '"
                            + member.name() + "' of class '" + member.declaringClass().name().toString() + "'");
        }
        if (!classInfo.hasNoArgsConstructor()) {
            throw new IllegalArgumentException(
                    String.format("Class '%s' which is used as %s in class '%s' must have a no-args constructor", classInfo,
                            member.phraseUsage(), member.declaringClass().name().toString()));
        }
        if (!Modifier.isPublic(classInfo.flags())) {
            throw new IllegalArgumentException(
                    String.format("Class '%s' which is used as %s in class '%s' must be a public class", classInfo,
                            member.phraseUsage(), member.declaringClass().name().toString()));
        }
    }

    private ClassInfo validateType(Type type) {
        if (type.kind() != Type.Kind.PARAMETERIZED_TYPE) {
            throw new IllegalArgumentException(ILLEGAL_ARGUMENT_MESSAGE);
        }
        ParameterizedType parameterizedType = (ParameterizedType) type;
        if (!DotNames.LIST.equals(parameterizedType.name())) {
            throw new IllegalArgumentException(ILLEGAL_ARGUMENT_MESSAGE);
        }
        if (parameterizedType.arguments().size() != 1) {
            throw new IllegalArgumentException(ILLEGAL_ARGUMENT_MESSAGE);
        }
        ClassInfo classInfo = index.getClassByName(parameterizedType.arguments().get(0).name());
        if (classInfo == null) {
            throw new IllegalArgumentException(ILLEGAL_ARGUMENT_MESSAGE);
        }
        return classInfo;
    }

    private String forSignature(ClassInfo classInfo) {
        return classInfo.name().toString().replace('.', '/');
    }

    /**
     * An abstraction over Field and Method which we will use in order to keep the same code for Class and Interface cases
     */
    static abstract class Member {
        private final ClassInfo declaringClass;
        private final Type type;
        private final String name;

        protected abstract String phraseUsage();

        public Member(ClassInfo declaringClass, Type type, String name) {
            this.declaringClass = declaringClass;
            this.type = type;
            this.name = name;
        }

        public ClassInfo declaringClass() {
            return declaringClass;
        }

        public Type type() {
            return type;
        }

        public String name() {
            return name;
        }
    }

    static class FieldMember extends Member {

        public FieldMember(FieldInfo fieldInfo) {
            super(fieldInfo.declaringClass(), fieldInfo.type(), fieldInfo.name());
        }

        @Override
        protected String phraseUsage() {
            return "field '" + name() + "'";
        }
    }

    static class MethodReturnTypeMember extends Member {

        public MethodReturnTypeMember(MethodInfo methodInfo) {
            super(methodInfo.declaringClass(), methodInfo.returnType(), methodInfo.name());
        }

        @Override
        protected String phraseUsage() {
            return "return type of method '" + name() + "'";
        }
    }

    private static final String ILLEGAL_ARGUMENT_MESSAGE = "YamlListObjectHandler can only be used for fields / methods that are of type 'List<SomeClass>' where 'SomeClass' is an application class";
}
