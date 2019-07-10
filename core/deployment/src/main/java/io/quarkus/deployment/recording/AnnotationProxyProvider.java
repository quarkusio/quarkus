package io.quarkus.deployment.recording;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import javax.enterprise.util.AnnotationLiteral;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.util.IoUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;

public class AnnotationProxyProvider {

    private final ConcurrentMap<DotName, String> annotationLiterals;
    private final ConcurrentMap<DotName, ClassInfo> annotationClasses;
    private final ConcurrentMap<String, Boolean> generatedLiterals;
    private final ClassLoader classLoader;
    private final IndexView index;
    private final Indexer indexer;

    AnnotationProxyProvider(IndexView index) {
        this.annotationLiterals = new ConcurrentHashMap<>();
        this.annotationClasses = new ConcurrentHashMap<>();
        this.generatedLiterals = new ConcurrentHashMap<>();
        this.index = index;
        this.indexer = new Indexer();
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
                    clazz = indexer.index(annotationStream);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to index: " + name, e);
                }
            }
            return clazz;
        });
        String annotationLiteral = annotationLiterals.computeIfAbsent(annotationInstance.name(), name ->
        // com.foo.MyAnnotation -> com.foo.MyAnnotation_Proxy_AnnotationLiteral
        name.toString().replace('.', '/') + "_Proxy_AnnotationLiteral");

        return new AnnotationProxyBuilder<>(annotationInstance, annotationType, annotationLiteral, annotationClass);
    }

    public interface AnnotationProxy {

        String getAnnotationLiteralType();

        ClassInfo getAnnotationClass();

        AnnotationInstance getAnnotationInstance();

        Map<String, Object> getDefaultValues();

    }

    public class AnnotationProxyBuilder<A> {

        private final ClassInfo annotationClass;
        private final String annotationLiteral;
        private final AnnotationInstance annotationInstance;
        private final Class<A> annotationType;
        private final Map<String, Object> defaultValues = new HashMap<>();

        AnnotationProxyBuilder(AnnotationInstance annotationInstance, Class<A> annotationType, String annotationLiteral,
                ClassInfo annotationClass) {
            this.annotationInstance = annotationInstance;
            this.annotationType = annotationType;
            this.annotationLiteral = annotationLiteral;
            this.annotationClass = annotationClass;
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

        @SuppressWarnings("unchecked")
        public A build(ClassOutput classOutput) {

            // Generate literal class if needed
            generatedLiterals.computeIfAbsent(annotationLiteral, generatedName -> {

                String name = annotationInstance.name().toString();

                // Ljavax/enterprise/util/AnnotationLiteral<Lcom/foo/MyAnnotation;>;Lcom/foo/MyAnnotation;
                String signature = String.format("Ljavax/enterprise/util/AnnotationLiteral<L%1$s;>;L%1$s;",
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

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = AnnotationProxy.class.getClassLoader();
            }
            return (A) Proxy.newProxyInstance(classLoader, new Class[] { annotationType, AnnotationProxy.class },
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            switch (method.getName()) {
                                case "getAnnotationLiteralType":
                                    return annotationLiteral;
                                case "getAnnotationClass":
                                    return annotationClass;
                                case "getAnnotationInstance":
                                    return annotationInstance;
                                case "getDefaultValues":
                                    return defaultValues;
                                default:
                                    break;
                            }
                            throw new UnsupportedOperationException("Method " + method + " not implemented");
                        }
                    });
        }
    }

}
