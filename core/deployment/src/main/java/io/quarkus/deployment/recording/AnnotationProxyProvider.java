package io.quarkus.deployment.recording;

import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.constant.ClassDesc;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import jakarta.enterprise.util.AnnotationLiteral;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.util.IoUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo2.GenericType;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.TypeArgument;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;

public class AnnotationProxyProvider {

    private final ConcurrentMap<DotName, String> annotationLiterals;
    private final ConcurrentMap<DotName, ClassInfo> annotationClasses;
    private final ConcurrentMap<String, Boolean> generatedLiterals;
    private final ClassLoader classLoader;
    private final IndexView index;

    AnnotationProxyProvider(IndexView index) {
        this.annotationLiterals = new ConcurrentHashMap<>();
        this.annotationClasses = new ConcurrentHashMap<>();
        this.generatedLiterals = new ConcurrentHashMap<>();
        this.index = index;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = AnnotationProxy.class.getClassLoader();
        }
        this.classLoader = classLoader;
    }

    public <A extends Annotation> AnnotationProxyBuilder<A> builder(AnnotationInstance annotationInstance,
            Class<A> annotationType) {
        if (!annotationInstance.name().toString().equals(annotationType.getName())) {
            throw new IllegalArgumentException("Annotation instance " + annotationInstance + " does not match annotation type "
                    + annotationType.getName());
        }
        ClassInfo annotationClass = annotationClasses.computeIfAbsent(annotationInstance.name(), name -> {
            ClassInfo clazz = index.getClassByName(name);
            if (clazz == null) {
                try (InputStream annotationStream = IoUtil.readClass(classLoader, name.toString())) {
                    clazz = Index.singleClass(annotationStream);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to index: " + name, e);
                }
            }
            return clazz;
        });
        String annotationLiteral = annotationLiterals.computeIfAbsent(annotationInstance.name(),
                // com.foo.MyAnnotation -> com.foo.MyAnnotation_Proxy_AnnotationLiteral
                name -> name + "_Proxy_AnnotationLiteral");

        return new AnnotationProxyBuilder<>(annotationInstance, annotationType, annotationLiteral, annotationClass);
    }

    public interface AnnotationProxy {

        String getAnnotationLiteralType();

        ClassInfo getAnnotationClass();

        AnnotationInstance getAnnotationInstance();

        Map<String, Object> getDefaultValues();

        Map<String, Object> getValues();

    }

    public class AnnotationProxyBuilder<A> {

        private final ClassInfo annotationClass;
        private final String annotationLiteral;
        private final AnnotationInstance annotationInstance;
        private final Class<A> annotationType;
        private final Map<String, Object> defaultValues = new HashMap<>();
        private final Map<String, Object> values = new HashMap<>();

        AnnotationProxyBuilder(AnnotationInstance annotationInstance, Class<A> annotationType, String annotationLiteral,
                ClassInfo annotationClass) {
            this.annotationInstance = annotationInstance;
            this.annotationType = annotationType;
            this.annotationLiteral = annotationLiteral;
            this.annotationClass = annotationClass;
        }

        /**
         * Explicit values override the default values from the annotation class.
         *
         * @param name
         * @param value
         * @return self
         */
        public AnnotationProxyBuilder<A> withValue(String name, Object value) {
            values.put(name, value);
            return this;
        }

        /**
         * Explicit default values override the default values from the annotation class.
         *
         * @param name
         * @param value
         * @return self
         */
        public AnnotationProxyBuilder<A> withDefaultValue(String name, Object value) {
            if (annotationInstance.value(name) == null) {
                defaultValues.put(name, value);
            }
            return this;
        }

        public A build(ClassOutput classOutput) {

            // Generate literal class if needed
            generatedLiterals.computeIfAbsent(annotationLiteral, generatedName -> {

                String name = annotationInstance.name().toString();

                // Ljakarta/enterprise/util/AnnotationLiteral<Lcom/foo/MyAnnotation;>;Lcom/foo/MyAnnotation;
                String signature = String.format("L%1$s<L%2$s;>;L%2$s;",
                        AnnotationLiteral.class.getName().replace('.', '/'),
                        name.replace('.', '/'));

                ClassCreator literal = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                        .superClass(AnnotationLiteral.class)
                        .interfaces(name).signature(signature).build();

                List<MethodInfo> constructorParams = annotationClass.methods().stream()
                        .filter(m -> !m.name().equals("<clinit>") && !m.name().equals("<init>"))
                        .collect(Collectors.toList());

                MethodCreator constructor = literal.getMethodCreator("<init>", "V",
                        constructorParams.stream().map(m -> m.returnType().name().toString()).toArray());
                constructor.invokeSpecialMethod(MethodDescriptor.ofConstructor(AnnotationLiteral.class), constructor.getThis());

                for (ListIterator<MethodInfo> iterator = constructorParams.listIterator(); iterator.hasNext();) {
                    MethodInfo param = iterator.next();
                    String returnType = param.returnType().name().toString();
                    // field
                    literal.getFieldCreator(param.name(), returnType).setModifiers(ACC_PRIVATE | ACC_FINAL);
                    // constructor param
                    constructor.writeInstanceField(FieldDescriptor.of(literal.getClassName(), param.name(), returnType),
                            constructor.getThis(),
                            constructor.getMethodParam(iterator.previousIndex()));
                    // value method
                    MethodCreator value = literal.getMethodCreator(param.name(), returnType).setModifiers(ACC_PUBLIC);
                    value.returnValue(value.readInstanceField(
                            FieldDescriptor.of(literal.getClassName(), param.name(), returnType), value.getThis()));
                }
                constructor.returnValue(null);
                literal.close();
                return Boolean.TRUE;
            });

            return proxy();
        }

        @SuppressWarnings("unchecked")
        private A proxy() {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = AnnotationProxy.class.getClassLoader();
            }
            return (A) Proxy.newProxyInstance(classLoader, new Class[] { annotationType, AnnotationProxy.class },
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            String name = method.getName();
                            return switch (name) {
                                case "getAnnotationLiteralType" -> annotationLiteral;
                                case "getAnnotationClass" -> annotationClass;
                                case "getAnnotationInstance" -> annotationInstance;
                                case "getDefaultValues" -> defaultValues;
                                case "getValues" -> values;
                                default -> {
                                    MethodInfo member = annotationClass.firstMethod(name);
                                    if (member != null) {
                                        if (values.containsKey(name)) {
                                            yield values.get(name);
                                        }
                                        if (annotationInstance.value(name) != null) {
                                            yield annotationInstance.value(name).value();
                                        }
                                        if (defaultValues.containsKey(name)) {
                                            yield defaultValues.get(name);
                                        }
                                        if (member.defaultValue() != null) {
                                            yield member.defaultValue().value();
                                        }
                                        throw new UnsupportedOperationException("Unknown value of annotation member " + name);
                                    }
                                    throw new UnsupportedOperationException("Method " + method + " not implemented");
                                }
                            };
                        }
                    });
        }

        public A build(io.quarkus.gizmo2.ClassOutput classOutput) {
            generatedLiterals.computeIfAbsent(annotationLiteral, generatedName -> {
                Gizmo gizmo = Gizmo.create(classOutput)
                        .withDebugInfo(false)
                        .withParameters(false);
                gizmo.class_(generatedName, cc -> {
                    ClassDesc annotationClassDesc = classDescOf(annotationInstance.name());
                    cc.extends_(GenericType.ofClass(AnnotationLiteral.class, TypeArgument.of(annotationClassDesc)));
                    cc.implements_(annotationClassDesc);

                    List<MethodInfo> members = annotationClass.methods()
                            .stream()
                            .filter(m -> !m.isStaticInitializer() && !m.isConstructor())
                            .toList();

                    List<FieldDesc> fields = new ArrayList<>(members.size());
                    for (MethodInfo member : members) {
                        fields.add(cc.field(member.name(), fc -> {
                            fc.private_();
                            fc.final_();
                            fc.setType(classDescOf(member.returnType()));
                        }));
                    }

                    cc.constructor(mc -> {
                        List<ParamVar> params = new ArrayList<>(members.size());
                        for (MethodInfo member : members) {
                            params.add(mc.parameter(member.name(), classDescOf(member.returnType())));
                        }

                        mc.body(bc -> {
                            bc.invokeSpecial(ConstructorDesc.of(AnnotationLiteral.class), cc.this_());
                            for (int i = 0; i < members.size(); i++) {
                                bc.set(cc.this_().field(fields.get(i)), params.get(i));
                            }
                            bc.return_();
                        });
                    });

                    for (int i = 0; i < members.size(); i++) {
                        MethodInfo member = members.get(i);
                        FieldDesc field = fields.get(i);
                        cc.method(member.name(), mc -> {
                            mc.returning(classDescOf(member.returnType()));
                            mc.body(bc -> bc.return_(cc.this_().field(field)));
                        });
                    }
                });

                return Boolean.TRUE;
            });

            return proxy();
        }
    }

}
