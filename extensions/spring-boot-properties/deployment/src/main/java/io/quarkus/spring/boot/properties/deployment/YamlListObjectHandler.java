package io.quarkus.spring.boot.properties.deployment;

import java.lang.constant.ClassDesc;
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
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.GenericType;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.TypeArgument;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
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

    public Expr handle(Member member, BlockCreator bc, Expr configObject,
            String configName, String fullConfigName) {
        ClassInfo classInfo = validateType(member.type());
        validateClass(classInfo, member);
        // these need to be registered for reflection because SnakeYaml used reflection to instantiate them
        reflectiveClasses
                .produce(ReflectiveClassBuildItem.builder(classInfo.name().toString()).methods().fields().build());

        String wrapperClassName = classInfo.name().toString() + "_GeneratedListWrapper_" + configName;

        // generate a class that has a List field and getters and setter which have the proper generic type
        // this way SnakeYaml can properly populate the field
        String getterName = JavaBeanUtil.getGetterName(configName, classInfo.name());
        MethodDesc getterDesc = generateWrapperClass(wrapperClassName, configName, classInfo, getterName);

        // we always generate getters and setters, so reflection is only needed on the methods
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(wrapperClassName).methods().build());

        // generate an MP-Config converter which looks something like this
        // public class GeneratedInputsConverter extends AbstractYamlObjectConverter<SomeClass_GeneratedListWrapper_fieldName> {
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
        generateConverterClass(wrapperConverterClassName, wrapperClassName, classInfo);

        // use the generated converter to convert the string value into the wrapper
        LocalVar smallryeConfig = bc.localVar("smallryeConfig", bc.cast(configObject, SmallRyeConfig.class));
        LocalVar getValueHandle = bc.localVar("getValueHandle", bc.invokeVirtual(
                MethodDesc.of(SmallRyeConfig.class, "getValue", Object.class, String.class, Converter.class),
                smallryeConfig,
                Const.of(fullConfigName),
                bc.new_(ConstructorDesc.of(ClassDesc.of(wrapperConverterClassName)))));
        LocalVar wrapperHandle = bc.localVar("wrapperHandle", bc.cast(getValueHandle, ClassDesc.of(wrapperClassName)));
        //pull the actual value out of the wrapper
        return bc.invokeVirtual(getterDesc, wrapperHandle);
    }

    private MethodDesc generateWrapperClass(String wrapperClassName, String configName, ClassInfo classInfo,
            String getterName) {
        Gizmo gizmo = Gizmo.create(classOutput);
        ClassDesc wrapperClassDesc = ClassDesc.of(wrapperClassName);
        ClassDesc elementClassDesc = ClassDesc.of(classInfo.name().toString());
        GenericType listOfElement = GenericType.ofClass(List.class, TypeArgument.of(elementClassDesc));

        gizmo.class_(wrapperClassName, cc -> {
            cc.defaultConstructor();

            FieldDesc fieldDesc = cc.field(configName, ifc -> {
                ifc.setType(listOfElement);
                ifc.private_();
            });

            cc.method(getterName, mc -> {
                mc.returning(listOfElement);
                mc.public_();
                mc.body(b -> {
                    b.return_(b.get(mc.this_().field(fieldDesc)));
                });
            });

            cc.method(JavaBeanUtil.getSetterName(configName), mc -> {
                mc.returning(void.class);
                mc.public_();
                var param = mc.parameter("value", listOfElement);
                mc.body(b -> {
                    b.set(mc.this_().field(fieldDesc), param);
                    b.return_();
                });
            });
        });

        return ClassMethodDesc.of(wrapperClassDesc, getterName, List.class);
    }

    private void generateConverterClass(String wrapperConverterClassName, String wrapperClassName, ClassInfo classInfo) {
        Gizmo gizmo = Gizmo.create(classOutput);
        ClassDesc wrapperClassDesc = ClassDesc.of(wrapperClassName);
        gizmo.class_(wrapperConverterClassName, cc -> {
            cc.extends_(GenericType.ofClass(ClassDesc.of(ABSTRACT_YAML_CONVERTER_CNAME),
                    TypeArgument.of(wrapperClassDesc)));

            cc.defaultConstructor();

            cc.method("getClazz", mc -> {
                mc.returning(Class.class);
                mc.protected_();
                mc.body(b -> {
                    b.return_(b.classForName(Const.of(wrapperClassName)));
                });
            });

            // generate the getFieldNameMap method by searching for fields annotated with @ConfigProperty
            List<AnnotationInstance> configPropertyInstances = classInfo.annotationsMap().get(DotNames.CONFIG_PROPERTY);
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
                cc.method("getFieldNameMap", mc -> {
                    mc.returning(Map.class);
                    mc.public_();
                    mc.body(b -> {
                        LocalVar resultHandle = b.localVar("resultHandle", b.new_(ConstructorDesc.of(HashMap.class)));
                        for (Map.Entry<String, String> entry : fieldNameMap.entrySet().stream()
                                .sorted(Map.Entry.comparingByKey()).toList()) {
                            b.invokeVirtual(
                                    MethodDesc.of(HashMap.class, "put", Object.class, Object.class, Object.class),
                                    resultHandle, Const.of(entry.getKey()), Const.of(entry.getValue()));
                        }
                        b.return_(resultHandle);
                    });
                });
            }
        });
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
