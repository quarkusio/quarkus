package org.jboss.shamrock.codegen;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.classfilewriter.AccessFlag;
import org.jboss.classfilewriter.ClassFile;
import org.jboss.classfilewriter.ClassMethod;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.invocation.proxy.ProxyConfiguration;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.shamrock.core.ClassOutput;
import org.jboss.shamrock.startup.StartupContext;

public class BytecodeRecorder implements AutoCloseable {

    private static final AtomicInteger COUNT = new AtomicInteger();

    private final String className;
    private final Class<?> serviceType;
    private final ClassOutput classOutput;
    private final Map<Method, MethodRecorder> methods = new HashMap<>();

    public BytecodeRecorder(String className, Class<?> serviceType, ClassOutput classOutput) {
        this.className = className;
        this.serviceType = serviceType;
        this.classOutput = classOutput;
        for (Method method : serviceType.getMethods()) {
            if (method.getDeclaringClass() != Object.class) {
                methods.put(method, new MethodRecorder(method));
            }
        }
    }

    public MethodRecorder getMethodRecorder(Method method) {
        if (!methods.containsKey(method)) {
            throw new RuntimeException("Method not found");
        }
        return methods.get(method);
    }

    public MethodRecorder getMethodRecorder() {
        if (methods.size() != 1) {
            throw new RuntimeException("More than one method present, the method must be specified explicitly");
        }
        return methods.values().iterator().next();
    }

    public class MethodRecorder {

        private final Map<Class<?>, Object> existingProxyClasses = new HashMap<>();
        private final List<StoredMethodCall> storedMethodCalls = new ArrayList<>();
        private final Method method;

        MethodRecorder(Method method) {
            this.method = method;
        }

        public <T> T getRecordingProxy(Class<T> theClass) {
            if (existingProxyClasses.containsKey(theClass)) {
                return (T) existingProxyClasses.get(theClass);
            }
            ProxyFactory<T> factory = new ProxyFactory<>(new ProxyConfiguration<T>()
                    .setSuperClass((Class) theClass)
                    .setClassLoader(getClass().getClassLoader())
                    .setProxyName(getClass().getName() + "$$RecordingProxyProxy" + COUNT.incrementAndGet()));
            try {
                T recordingProxy = factory.newInstance(new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        validateMethod(method);
                        storedMethodCalls.add(new StoredMethodCall(theClass, method, args));
                        if (method.getReturnType().isPrimitive()) {
                            return 0;
                        }
                        return null;
                    }
                });
                existingProxyClasses.put(theClass, recordingProxy);
                return recordingProxy;
            } catch (IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        }

    }


    private void validateMethod(Method method) {
        for (int i = 0; i < method.getParameterCount(); ++i) {
            Class<?> type = method.getParameterTypes()[i];
            if (type.isPrimitive() || type.equals(String.class) || type.equals(Class.class) || type.equals(StartupContext.class)) {
                continue;
            }
            Annotation[] annotations = method.getParameterAnnotations()[i];
            boolean found = false;
            for (Annotation j : annotations) {
                if (j.annotationType() == ContextObject.class) {
                    found = true;
                    break;
                }
            }
            if (found) {
                continue;
            }
            throw new RuntimeException("Cannot invoke method " + method + " as parameter " + i + " cannot be recorded");
        }

    }

    @Override
    public void close() throws IOException {
        ClassFile file = new ClassFile(className, AccessFlag.PUBLIC, Object.class.getName(), getClass().getClassLoader(), serviceType.getName());
        for (Map.Entry<Method, MethodRecorder> entry : methods.entrySet()) {
            ClassMethod method = file.addMethod(entry.getKey());
            CodeAttribute ca = method.getCodeAttribute();

            //first we need to create the context map
            //ca.newInstruction(HashMap.class);
            //ca.dup();
            //ca.invokespecial(HashMap.class.getName(), "<init>", "()V");
            ca.returnInstruction();


        }
        ClassMethod ctor = file.addMethod(AccessFlag.PUBLIC, "<init>", "V");
        CodeAttribute ca = ctor.getCodeAttribute();
        ca.aload(0);
        ca.invokespecial(Object.class.getName(), "<init>", "()V");
        ca.returnInstruction();
        classOutput.writeClass(file.getName(), file.toBytecode());
    }

    static final class StoredMethodCall {
        final Class<?> theClass;
        final Method method;
        final Object[] parameters;

        StoredMethodCall(Class<?> theClass, Method method, Object[] parameters) {
            this.theClass = theClass;
            this.method = method;
            this.parameters = parameters;
        }
    }

}
