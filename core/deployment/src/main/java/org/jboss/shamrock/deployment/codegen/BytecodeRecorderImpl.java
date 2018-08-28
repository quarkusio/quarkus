package org.jboss.shamrock.deployment.codegen;

import static org.jboss.protean.gizmo.MethodDescriptor.ofMethod;

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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.classfilewriter.AccessFlag;
import org.jboss.classfilewriter.ClassFile;
import org.jboss.classfilewriter.ClassMethod;
import org.jboss.classfilewriter.code.BranchEnd;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.classfilewriter.util.DescriptorUtils;
import org.jboss.invocation.proxy.ProxyConfiguration;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.protean.gizmo.BranchResult;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.shamrock.deployment.ClassOutput;
import org.jboss.shamrock.deployment.ShamrockConfig;
import org.jboss.shamrock.runtime.ContextObject;
import org.jboss.shamrock.runtime.InjectionInstance;
import org.jboss.shamrock.runtime.RuntimeInjector;
import org.jboss.shamrock.runtime.StartupContext;
import org.jboss.shamrock.runtime.StartupTask;

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
            if(params[i] instanceof Enum) {
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
        ClassCreator file = ClassCreator.builder().classOutput(new org.jboss.protean.gizmo.ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                try {
                    classOutput.writeClass(true, name, data);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).className(className).superClass(Object.class).interfaces(StartupTask.class).build();
        MethodCreator method = file.getMethodCreator(this.method.getName(), this.method.getReturnType(), this.method.getParameterTypes());

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
        Map<Class, ResultHandle> classInstanceVariables = new HashMap<>();
        Map<Object, ResultHandle> returnValueResults = new IdentityHashMap<>();
        for (BytecodeInstruction set : this.methodRecorder.storedMethodCalls) {
            if (set instanceof StoredMethodCall) {
                StoredMethodCall call = (StoredMethodCall) set;
                if (classInstanceVariables.containsKey(call.theClass)) {
                    continue;
                }
                ResultHandle instance = method.newInstance(MethodDescriptor.ofConstructor(call.theClass));
                classInstanceVariables.put(call.theClass, instance);
            }
        }
        //now we invoke the actual method call
        for (BytecodeInstruction set : methodRecorder.storedMethodCalls) {
            if (set instanceof StoredMethodCall) {
                StoredMethodCall call = (StoredMethodCall) set;
                ResultHandle[] params = new ResultHandle[call.parameters.length];

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
                            params[i] = method.load((String)param);
                            if (configParam != null) {
                                ResultHandle config = method.invokeStaticMethod(ofMethod(ConfigProvider.class, "getConfig", Config.class));
                                ResultHandle propName = method.load(configParam);
                                ResultHandle configOptional = method.invokeInterfaceMethod(ofMethod(Config.class, "getOptionalValue", Optional.class, String.class, Class.class), config, propName, method.loadClass(String.class));
                                ResultHandle result = method.invokeVirtualMethod(ofMethod(Optional.class, "isPresent", boolean.class), configOptional);

                                BranchResult ifResult = method.ifNonZero(result);
                                ResultHandle tr =  ifResult.trueBranch().invokeVirtualMethod(ofMethod(Optional.class, "get", Object.class), configOptional);
                                ResultHandle trs =  ifResult.trueBranch().invokeVirtualMethod(ofMethod(Object.class, "toString", String.class), tr);
                                ResultHandle fr = ifResult.falseBranch().load((String) param);
                                params[i] = ifResult.mergeBranches(trs, fr);
                            } else {
                                params[i] = method.load((String)param);
                            }
                        }  else if (param instanceof Enum) {
                            Enum e = (Enum) param;
                            ResultHandle nm = method.load(e.name());
                            params[i] = method.invokeStaticMethod(ofMethod(e.getDeclaringClass(), "valueOf", e.getDeclaringClass(), String.class), nm);
                        } else if (param instanceof Boolean) {
                            params[i] = method.load((boolean)param);
                        } else if (param instanceof NewInstance) {
                            params[i] = ((NewInstance) param).resultHandle;
                        } else if (param instanceof ReturnedProxy) {
                            ResultHandle pos = returnValueResults.get(param);
                            if (pos == null) {
                                throw new RuntimeException("invalid proxy passed into recorded method " + call.method);
                            }
                            params[i] = pos;
                        } else if (param instanceof Class<?>) {
                            String name = classProxies.get(param);
                            if (name == null) {
                                name = ((Class) param).getName();
                            }
                            params[i] = method.invokeStaticMethod(ofMethod(Class.class, "forName", Class.class, String.class), method.load(name));
                        } else {
                            //TODO: rest of primitives
                            params[i] = method.load((int) param);
                        }
                    } else if (targetType == StartupContext.class) { //hack, as this is tied to StartupTask
                        params[i] = method.getMethodParam(0);
                    } else if (contextName != null) {
                        params[i] = method.invokeVirtualMethod(ofMethod(StartupContext.class, "getValue", Object.class, String.class), method.getMethodParam(0), method.load(contextName));
                    } else {
                        params[i] = method.loadNull();
                    }
                }
                ResultHandle callResult = method.invokeVirtualMethod(ofMethod(call.method.getDeclaringClass(), call.method.getName(), call.method.getReturnType(), call.method.getParameterTypes()), classInstanceVariables.get(call.theClass), params);

                if (call.method.getReturnType() != void.class) {
                    ContextObject annotation = call.method.getAnnotation(ContextObject.class);
                    if (annotation != null) {
                        method.invokeVirtualMethod(ofMethod(StartupContext.class, "putValue", void.class, String.class, Object.class), method.getMethodParam(0), method.load(annotation.value()), callResult);
                        if(call.returnedProxy != null) {
                            returnValueResults.put(call.returnedProxy, callResult);
                        }
                    } else if (call.returnedProxy != null) {
                        returnValueResults.put(call.returnedProxy, callResult);
                    }
                }
            } else if (set instanceof NewInstance) {
                NewInstance ni = (NewInstance) set;
                ni.resultHandle = method.invokeStaticMethod(ofMethod(RuntimeInjector.class, "newFactory", InjectionInstance.class, Class.class), method.loadClass(ni.className));
            } else {
                throw new RuntimeException("unkown type " + set);
            }
        }

        method.returnValue(null);
        file.close();
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
        ResultHandle resultHandle;

        NewInstance(String className) {
            this.className = className;
        }

        @Override
        public Object newInstance() {
            throw new RuntimeException();
        }
    }

}
