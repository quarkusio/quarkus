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

            //figure out where we can start using local variables
            int localVarCounter = 1;
            for (Class<?> t : entry.getKey().getParameterTypes()) {
                if (t == double.class || t == long.class) {
                    localVarCounter += 2;
                } else {
                    localVarCounter++;
                }
            }

            //now create instances of all the classes we invoke on and store them in variables as well
            Map<Class, Integer> classInstanceVariables = new HashMap<>();
            for (StoredMethodCall call : entry.getValue().storedMethodCalls) {
                if (classInstanceVariables.containsKey(call.theClass)) {
                    continue;
                }
                ca.newInstruction(call.theClass);
                ca.dup();
                ca.invokespecial(call.theClass.getName(), "<init>", "()V");
                ca.astore(localVarCounter);
                classInstanceVariables.put(call.theClass, localVarCounter++);
            }
            //now we invoke the actual method call
            for (StoredMethodCall call : entry.getValue().storedMethodCalls) {
                ca.aload(classInstanceVariables.get(call.theClass));
                ca.checkcast(call.theClass);
                for (int i = 0; i < call.parameters.length; ++i) {
                    Class<?> targetType = call.method.getParameterTypes()[i];
                    Annotation[] annotations = call.method.getParameterAnnotations()[i];
                    String contextName = null;
                    if (annotations != null) {
                        for (Annotation a : annotations) {
                            if (a.annotationType() == ContextObject.class) {
                                ContextObject obj = (ContextObject) a;
                                contextName = obj.value();
                                break;
                            }
                        }
                    }
                    if (call.parameters[i] != null) {
                        Object param = call.parameters[i];
                        if (param instanceof String) {
                            ca.ldc((String) param);
                        } else {
                            //TODO: rest of primities
                            ca.ldc((int) param);
                        }
                    } else if (targetType == StartupContext.class) { //hack, as this is tied to StartupTask
                        ca.aload(1);
                    } else if (contextName != null) {
                        ca.aload(1);
                        ca.ldc(contextName);
                        ca.invokevirtual(StartupContext.class.getName(), "getValue", "(Ljava/lang/String;)Ljava/lang/Object;");
                        ca.checkcast(targetType);
                    } else {
                        ca.aconstNull();
                    }
                }
                ca.invokevirtual(call.method);
                if (call.method.getReturnType() != void.class) {
                    ContextObject annotation = call.method.getAnnotation(ContextObject.class);
                    if (annotation != null) {
                        ca.aload(1);
                        ca.swap();
                        ca.ldc(annotation.value());
                        ca.swap();
                        ca.invokevirtual(StartupContext.class.getName(), "putValue", "(Ljava/lang/String;Ljava/lang/Object;)V");
                    } else if (call.method.getReturnType() == long.class || call.method.getReturnType() == double.class) {
                        ca.pop2();
                    } else {
                        ca.pop();
                    }
                }
            }

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
