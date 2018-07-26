package org.jboss.shamrock.deployment.codegen;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.classfilewriter.AccessFlag;
import org.jboss.classfilewriter.ClassFile;
import org.jboss.classfilewriter.ClassMethod;
import org.jboss.classfilewriter.code.BranchEnd;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.invocation.proxy.ProxyConfiguration;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.shamrock.deployment.ClassOutput;
import org.jboss.shamrock.deployment.ShamrockConfig;
import org.jboss.shamrock.runtime.ContextObject;
import org.jboss.shamrock.runtime.InjectionInstance;
import org.jboss.shamrock.runtime.RuntimeInjector;
import org.jboss.shamrock.runtime.StartupContext;

public class BytecodeRecorderImpl implements BytecodeRecorder {

    private static final AtomicInteger COUNT = new AtomicInteger();

    private final ClassLoader classLoader;
    private final String className;
    private final Class<?> serviceType;
    private final ClassOutput classOutput;
    private final MethodRecorder methodRecorder;
    private final Method method;

    private final Map<Class, ProxyFactory<?>> returnValueProxy = new HashMap<>();
    private final IdentityHashMap<Class<?>, String> classProxies = new IdentityHashMap<>();

    public BytecodeRecorderImpl(ClassLoader classLoader, String className, Class<?> serviceType, ClassOutput classOutput) {
        this.classLoader = classLoader;
        this.className = className;
        this.serviceType = serviceType;
        this.classOutput = classOutput;
        MethodRecorder mr = null;
        Method m = null;
        for (Method method : serviceType.getMethods()) {
            if (method.getDeclaringClass() != Object.class) {
                if (mr != null) {
                    throw new RuntimeException("Invalid type, must have a single method");
                }
                mr = new MethodRecorder();
                m = method;
            }
        }
        methodRecorder = mr;
        method = m;
    }

    private String findContextName(Annotation[] annotations) {
        for (Annotation a : annotations) {
            if (a.annotationType() == ContextObject.class) {
                return ((ContextObject) a).value();
            }
        }
        return null;
    }

    @Override
    public InjectionInstance<?> newInstanceFactory(String className) {
        NewInstance instance = new NewInstance(className);
        methodRecorder.storedMethodCalls.add(instance);
        return instance;
    }

    @Override
    public <T> T getRecordingProxy(Class<T> theClass) {
        return methodRecorder.getRecordingProxy(theClass);
    }

    @Override
    public Class<?> classProxy(String name) {
        ProxyFactory<Object> factory = new ProxyFactory<>(new ProxyConfiguration<Object>()
                .setSuperClass(Object.class)
                .setClassLoader(classLoader)
                .setProxyName(getClass().getName() + "$$ClassProxy" + COUNT.incrementAndGet()));
        Class theClass = factory.defineClass();
        classProxies.put(theClass, name);
        return theClass;
    }

    private class MethodRecorder {

        private final Map<Class<?>, Object> existingProxyClasses = new HashMap<>();
        private final List<BytecodeInstruction> storedMethodCalls = new ArrayList<>();

        public <T> T getRecordingProxy(Class<T> theClass) {
            if (existingProxyClasses.containsKey(theClass)) {
                return (T) existingProxyClasses.get(theClass);
            }
            ProxyFactory<T> factory = new ProxyFactory<>(new ProxyConfiguration<T>()
                    .setSuperClass((Class) theClass)
                    .setClassLoader(classLoader)
                    .setProxyName(getClass().getName() + "$$RecordingProxyProxy" + COUNT.incrementAndGet()));
            try {
                T recordingProxy = factory.newInstance(new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        validateMethod(method, args);
                        StoredMethodCall storedMethodCall = new StoredMethodCall(theClass, method, args);
                        storedMethodCalls.add(storedMethodCall);
                        if (method.getReturnType().isPrimitive()) {
                            return 0;
                        }
                        if (Modifier.isFinal(method.getReturnType().getModifiers())) {
                            return null;
                        }
                        boolean returnInterface = method.getReturnType().isInterface();
                        if (!returnInterface) {
                            try {
                                method.getReturnType().getConstructor();
                            } catch (NoSuchMethodException e) {
                                return null;
                            }
                        }
                        ProxyFactory<?> proxyFactory = returnValueProxy.get(method.getReturnType());
                        if (proxyFactory == null) {
                            ProxyConfiguration<Object> proxyConfiguration = new ProxyConfiguration<Object>()
                                    .setSuperClass(returnInterface ? Object.class : (Class) method.getReturnType())
                                    .setClassLoader(classLoader)
                                    .addAdditionalInterface(ReturnedProxy.class)
                                    .setProxyName(getClass().getName() + "$$ReturnValueProxy" + COUNT.incrementAndGet());

                            if (returnInterface) {
                                proxyConfiguration.addAdditionalInterface(method.getReturnType());
                            }
                            returnValueProxy.put(method.getReturnType(), proxyFactory = new ProxyFactory<>(proxyConfiguration));
                        }

                        Object proxyInstance = proxyFactory.newInstance(new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                throw new RuntimeException("You cannot invoke directly on an object returned from the bytecode recorded, you can only pass is back into the recorder as a parameter");
                            }
                        });
                        storedMethodCall.returnedProxy = proxyInstance;
                        return proxyInstance;
                    }
                });
                existingProxyClasses.put(theClass, recordingProxy);
                return recordingProxy;
            } catch (IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        }

    }


    private void validateMethod(Method method, Object[] params) {
        for (int i = 0; i < method.getParameterCount(); ++i) {
            Class<?> type = method.getParameterTypes()[i];
            if (type.isPrimitive() || type.equals(String.class) || type.equals(Class.class) || type.equals(StartupContext.class)) {
                continue;
            }
            if (type.isAssignableFrom(NewInstance.class)) {
                continue;
            }
            if (params[i] instanceof ReturnedProxy) {
                continue;
            }
            if (params[i] instanceof Class) {
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
        ClassFile file = new ClassFile(className, AccessFlag.PUBLIC, Object.class.getName(), classLoader, serviceType.getName());
        ClassMethod method = file.addMethod(this.method);
        CodeAttribute ca = method.getCodeAttribute();

        //figure out where we can start using local variables
        int localVarCounter = 1;
        for (Class<?> t : this.method.getParameterTypes()) {
            if (t == double.class || t == long.class) {
                localVarCounter += 2;
            } else {
                localVarCounter++;
            }
        }

        //now create instances of all the classes we invoke on and store them in variables as well
        Map<Class, Integer> classInstanceVariables = new HashMap<>();
        Map<Object, Integer> returnValuePositions = new IdentityHashMap<>();
        for (BytecodeInstruction set : this.methodRecorder.storedMethodCalls) {
            if (set instanceof StoredMethodCall) {
                StoredMethodCall call = (StoredMethodCall) set;
                if (classInstanceVariables.containsKey(call.theClass)) {
                    continue;
                }
                ca.newInstruction(call.theClass);
                ca.dup();
                ca.invokespecial(call.theClass.getName(), "<init>", "()V");
                ca.astore(localVarCounter);
                classInstanceVariables.put(call.theClass, localVarCounter++);
            } else if (set instanceof NewInstance) {
                ((NewInstance) set).varPos = localVarCounter++;
            }
        }
        //now we invoke the actual method call
        for (BytecodeInstruction set : methodRecorder.storedMethodCalls) {
            if (set instanceof StoredMethodCall) {
                StoredMethodCall call = (StoredMethodCall) set;
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
                            String configParam = ShamrockConfig.getConfigKey((String) param);
                            if (configParam != null) {
                                ca.invokestatic("org.eclipse.microprofile.config.ConfigProvider", "getConfig", "()Lorg/eclipse/microprofile/config/Config;");
                                ca.ldc(configParam);
                                ca.loadClass("java/lang/String");
                                ca.invokeinterface("org/eclipse/microprofile/config/Config", "getOptionalValue", "(Ljava/lang/String;Ljava/lang/Class;)Ljava/util/Optional;");
                                ca.dup();
                                ca.invokevirtual("java/util/Optional", "isPresent", "()Z");
                                BranchEnd end = ca.ifeq();

                                ca.invokevirtual("java/util/Optional", "get", "()Ljava/lang/Object;");
                                ca.invokevirtual("java/lang/Object", "toString", "()Ljava/lang/String;"); //checkcast is buggy in classfilewriter

                                BranchEnd jmp = ca.gotoInstruction();
                                ca.branchEnd(end);
                                ca.pop();
                                ca.ldc((String) param);


                                ca.branchEnd(jmp);
                            } else {
                                ca.ldc((String) param);
                            }
                        } else if (param instanceof Boolean) {
                            ca.ldc((boolean) param ? 1 : 0);
                        } else if (param instanceof NewInstance) {
                            ca.aload(((NewInstance) param).varPos);
                        } else if (param instanceof ReturnedProxy) {
                            Integer pos = returnValuePositions.get(param);
                            if (pos == null) {
                                throw new RuntimeException("invalid proxy passed into recorded method " + call.method);
                            }
                            ca.aload(pos);
                        } else if (param instanceof Class<?>) {
                            String name = classProxies.get(param);
                            if (name == null) {
                                name = ((Class) param).getName();
                            }
                            ca.ldc(name);
                            ca.invokestatic("java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
                            //ca.loadClass(name);
                        } else {
                            //TODO: rest of primitives
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
                        ca.dup();
                        ca.aload(1);
                        ca.swap();
                        ca.ldc(annotation.value());
                        ca.swap();
                        ca.invokevirtual(StartupContext.class.getName(), "putValue", "(Ljava/lang/String;Ljava/lang/Object;)V");
                        if(call.returnedProxy != null) {
                            Integer pos = returnValuePositions.get(call.returnedProxy);
                            if (pos == null) {
                                returnValuePositions.put(call.returnedProxy, pos = localVarCounter++);
                            }
                            ca.astore(pos);
                        }
                    } else if (call.returnedProxy != null) {
                        Integer pos = returnValuePositions.get(call.returnedProxy);
                        if (pos == null) {
                            returnValuePositions.put(call.returnedProxy, pos = localVarCounter++);
                        }
                        ca.astore(pos);
                    } else if (call.method.getReturnType() == long.class || call.method.getReturnType() == double.class) {
                        ca.pop2();
                    } else {
                        ca.pop();
                    }
                }
            } else if (set instanceof NewInstance) {
                NewInstance ni = (NewInstance) set;
                ca.loadClass(ni.className);
                ca.invokestatic(RuntimeInjector.class.getName(), "newFactory", "(Ljava/lang/Class;)Lorg/jboss/shamrock/runtime/InjectionInstance;");
                ca.astore(ni.varPos);
            } else {
                throw new RuntimeException("unkown type " + set);
            }
        }

        ca.returnInstruction();

        ClassMethod ctor = file.addMethod(AccessFlag.PUBLIC, "<init>", "V");
        ca = ctor.getCodeAttribute();
        ca.aload(0);
        ca.invokespecial(Object.class.getName(), "<init>", "()V");
        ca.returnInstruction();
        classOutput.writeClass(file.getName(), file.toBytecode());
    }

    public interface BytecodeInstruction {

    }


    public interface ReturnedProxy {

    }

    static final class StoredMethodCall implements BytecodeInstruction {
        final Class<?> theClass;
        final Method method;
        final Object[] parameters;
        Object returnedProxy;

        StoredMethodCall(Class<?> theClass, Method method, Object[] parameters) {
            this.theClass = theClass;
            this.method = method;
            this.parameters = parameters;
        }
    }

    static final class NewInstance implements BytecodeInstruction, InjectionInstance {
        final String className;
        int varPos = -1;

        NewInstance(String className) {
            this.className = className;
        }

        @Override
        public Object newInstance() {
            throw new RuntimeException();
        }
    }

}
