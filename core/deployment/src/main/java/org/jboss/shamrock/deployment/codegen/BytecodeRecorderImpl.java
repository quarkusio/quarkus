package org.jboss.shamrock.deployment.codegen;

import static org.jboss.protean.gizmo.MethodDescriptor.ofConstructor;
import static org.jboss.protean.gizmo.MethodDescriptor.ofMethod;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.commons.beanutils.PropertyUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.invocation.proxy.ProxyConfiguration;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.protean.gizmo.BranchResult;
import org.jboss.protean.gizmo.CatchBlockCreator;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.protean.gizmo.TryBlock;
import org.jboss.shamrock.deployment.ClassOutput;
import org.jboss.shamrock.deployment.ShamrockConfig;
import org.jboss.shamrock.runtime.ConfiguredValue;
import org.jboss.shamrock.runtime.ContextObject;
import org.jboss.shamrock.runtime.InjectionInstance;
import org.jboss.shamrock.runtime.RuntimeInjector;
import org.jboss.shamrock.runtime.StartupContext;
import org.jboss.shamrock.runtime.StartupTask;

public class BytecodeRecorderImpl implements BytecodeRecorder {

    private static final AtomicInteger COUNT = new AtomicInteger();
    private static final MethodDescriptor COLLECTION_ADD = ofMethod(Collection.class, "add", boolean.class, Object.class);
    private static final MethodDescriptor MAP_PUT = ofMethod(Map.class, "put", Object.class, Object.class, Object.class);

    private final ClassLoader classLoader;
    private final String className;
    private final Class<?> serviceType;
    private final ClassOutput classOutput;
    private final MethodRecorder methodRecorder;
    private final Method method;

    private final Map<Class, ProxyFactory<?>> returnValueProxy = new HashMap<>();
    private final IdentityHashMap<Class<?>, String> classProxies = new IdentityHashMap<>();
    private final Map<Class<?>, SubstitutionHolder> substitutions = new HashMap<>();
    private final Map<Class<?>, NonDefaultConstructorHolder> nonDefaulConstructors = new HashMap<>();

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
    public <F, T> void registerSubstitution(Class<F> from, Class<T> to, Class<? extends ObjectSubstitution<F, T>> substitution) {
        substitutions.put(from, new SubstitutionHolder(from, to, substitution));
    }

    @Override
    public <T> void registerNonDefaultConstructor(Constructor<T> constructor, Function<T, List<Object>> parameters) {
        nonDefaulConstructors.put(constructor.getDeclaringClass(), new NonDefaultConstructorHolder(constructor, (Function<Object, List<Object>>) parameters));
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
                return theClass.cast(existingProxyClasses.get(theClass));
            }
            ProxyFactory<T> factory = new ProxyFactory<T>(new ProxyConfiguration<T>()
                    .setSuperClass(theClass)
                    .setClassLoader(classLoader)
                    .setProxyName(getClass().getName() + "$$RecordingProxyProxy" + COUNT.incrementAndGet()));
            try {
                T recordingProxy = factory.newInstance(new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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


    @Override
    public void close() {
        ClassCreator file = ClassCreator.builder().classOutput(ClassOutput.gizmoAdaptor(classOutput,  true)).className(className).superClass(Object.class).interfaces(StartupTask.class).build();
        MethodCreator method = file.getMethodCreator(this.method.getName(), this.method.getReturnType(), this.method.getParameterTypes());

        //now create instances of all the classes we invoke on and store them in variables as well
        Map<Class, ResultHandle> classInstanceVariables = new HashMap<>();
        Map<Object, ResultHandle> returnValueResults = new IdentityHashMap<>();
        Map<String, ResultHandle> contextResults = new HashMap<>();
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
                        ResultHandle out;
                        out = loadObjectInstance(method, param, returnValueResults, targetType);
                        params[i] = out;
                    } else if (targetType == StartupContext.class) { //hack, as this is tied to StartupTask
                        params[i] = method.getMethodParam(0);
                    } else if (contextName != null) {
                        ResultHandle existing = contextResults.get(contextName);
                        if(existing != null) {
                            params[i] = existing;
                        } else {
                            params[i] = method.invokeVirtualMethod(ofMethod(StartupContext.class, "getValue", Object.class, String.class), method.getMethodParam(0), method.load(contextName));
                            contextResults.put(contextName, params[i]);
                        }
                    } else {
                        params[i] = method.loadNull();
                    }
                }
                ResultHandle callResult = method.invokeVirtualMethod(ofMethod(call.method.getDeclaringClass(), call.method.getName(), call.method.getReturnType(), call.method.getParameterTypes()), classInstanceVariables.get(call.theClass), params);

                if (call.method.getReturnType() != void.class) {
                    ContextObject annotation = call.method.getAnnotation(ContextObject.class);
                    if (annotation != null) {
                        method.invokeVirtualMethod(ofMethod(StartupContext.class, "putValue", void.class, String.class, Object.class), method.getMethodParam(0), method.load(annotation.value()), callResult);
                        if (call.returnedProxy != null) {
                            returnValueResults.put(call.returnedProxy, callResult);
                        }
                        contextResults.remove(annotation.value());
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

    private ResultHandle loadObjectInstance(MethodCreator method, Object param, Map<Object, ResultHandle> returnValueResults, Class<?> expectedType) {

        ResultHandle existing = returnValueResults.get(param);
        if (existing != null) {
            return existing;
        }
        ResultHandle out;
        if (param == null) {
            out = method.loadNull();
        } else if (substitutions.containsKey(param.getClass())) {
            SubstitutionHolder holder = substitutions.get(param.getClass());
            try {
                ObjectSubstitution substitution = holder.sub.newInstance();
                Object res = substitution.serialize(param);
                ResultHandle serialized = loadObjectInstance(method, res, returnValueResults, holder.to);
                ResultHandle subInstane = method.newInstance(MethodDescriptor.ofConstructor(holder.sub));
                out = method.invokeInterfaceMethod(ofMethod(ObjectSubstitution.class, "deserialize", Object.class, Object.class), subInstane, serialized);
            } catch (Exception e) {
                throw new RuntimeException("Failed to substitute " + param, e);
            }

        } else if (param instanceof ConfiguredValue) {
            ConfiguredValue val = (ConfiguredValue) param;
            String value = val.getValue();
            String key = val.getKey();
            out = method.newInstance(ofConstructor(ConfiguredValue.class, String.class, String.class), method.load(key), method.load(value));
        } else if (param instanceof String) {
            String configParam = ShamrockConfig.getConfigKey((String) param);
            if (configParam != null) {
                ResultHandle config = method.invokeStaticMethod(ofMethod(ConfigProvider.class, "getConfig", Config.class));
                ResultHandle propName = method.load(configParam);
                ResultHandle configOptional = method.invokeInterfaceMethod(ofMethod(Config.class, "getOptionalValue", Optional.class, String.class, Class.class), config, propName, method.loadClass(String.class));
                ResultHandle result = method.invokeVirtualMethod(ofMethod(Optional.class, "isPresent", boolean.class), configOptional);

                BranchResult ifResult = method.ifNonZero(result);
                ResultHandle tr = ifResult.trueBranch().invokeVirtualMethod(ofMethod(Optional.class, "get", Object.class), configOptional);
                ResultHandle trs = ifResult.trueBranch().invokeVirtualMethod(ofMethod(Object.class, "toString", String.class), tr);
                ResultHandle fr = ifResult.falseBranch().load((String) param);
                out = ifResult.mergeBranches(trs, fr);
            } else {
                out = method.load((String) param);
            }
        } else if (param instanceof URL) {
            String url = ((URL) param).toExternalForm();
            TryBlock et = method.tryBlock();
            out = et.newInstance(MethodDescriptor.ofConstructor(URL.class, String.class), et.load(url));
            CatchBlockCreator malformed = et.addCatch(MalformedURLException.class);
            malformed.throwException(RuntimeException.class, "Malformed URL", malformed.getCaughtException());

        } else if (param instanceof Enum) {
            Enum e = (Enum) param;
            ResultHandle nm = method.load(e.name());
            out = method.invokeStaticMethod(ofMethod(e.getDeclaringClass(), "valueOf", e.getDeclaringClass(), String.class), nm);
        } else if (param instanceof NewInstance) {
            out = ((NewInstance) param).resultHandle;
        } else if (param instanceof ReturnedProxy) {
            throw new RuntimeException("invalid proxy passed into recorded method " + method);
        } else if (param instanceof Class<?>) {
            String name = classProxies.get(param);
            if (name == null) {
                name = ((Class) param).getName();
            }
            out = method.invokeStaticMethod(ofMethod(Class.class, "forName", Class.class, String.class), method.load(name));
        } else if (expectedType == boolean.class) {
            out = method.load((boolean) param);
        } else if (expectedType == Boolean.class) {
            out = method.invokeStaticMethod(ofMethod(Boolean.class, "valueOf", Boolean.class, boolean.class), method.load((boolean) param));
        } else if (expectedType == int.class) {
            out = method.load((int) param);
        } else if (expectedType == Integer.class) {
            out = method.invokeStaticMethod(ofMethod(Integer.class, "valueOf", Integer.class, int.class), method.load((int) param));
        } else if (expectedType == short.class) {
            out = method.load((short) param);
        } else if (expectedType == Short.class) {
            out = method.invokeStaticMethod(ofMethod(Short.class, "valueOf", Short.class, short.class), method.load((short) param));
        } else if (expectedType == byte.class) {
            out = method.load((byte) param);
        } else if (expectedType == Byte.class) {
            out = method.invokeStaticMethod(ofMethod(Byte.class, "valueOf", Byte.class, byte.class), method.load((byte) param));
        } else if (expectedType == char.class) {
            out = method.load((char) param);
        } else if (expectedType == Character.class) {
            out = method.invokeStaticMethod(ofMethod(Character.class, "valueOf", Character.class, char.class), method.load((char) param));
        } else if (expectedType == long.class) {
            out = method.load((long) param);
        } else if (expectedType == Long.class) {
            out = method.invokeStaticMethod(ofMethod(Long.class, "valueOf", Long.class, long.class), method.load((long) param));
        } else if (expectedType == float.class) {
            out = method.load((float) param);
        } else if (expectedType == Float.class) {
            out = method.invokeStaticMethod(ofMethod(Float.class, "valueOf", Float.class, float.class), method.load((float) param));
        } else if (expectedType == double.class) {
            out = method.load((double) param);
        } else if (expectedType == Double.class) {
            out = method.invokeStaticMethod(ofMethod(Double.class, "valueOf", Double.class, double.class), method.load((double) param));
        } else if (expectedType.isArray()) {
            int length = Array.getLength(param);
            out = method.newArray(expectedType.getComponentType(), method.load(length));
            for (int i = 0; i < length; ++i) {
                ResultHandle component = loadObjectInstance(method, Array.get(param, i), returnValueResults, expectedType.getComponentType());
                method.writeArrayValue(out, i, component);
            }
        } else {
            if(nonDefaulConstructors.containsKey(param.getClass())) {
                NonDefaultConstructorHolder holder = nonDefaulConstructors.get(param.getClass());
                List<Object> params = holder.paramGenerator.apply(param);
                if(params.size() != holder.constructor.getParameterCount()) {
                    throw new RuntimeException("Unable to serialize " + param + " as the wrong number of parameters were generated for " + holder.constructor);
                }
                List<ResultHandle> handles = new ArrayList<>();
                int count = 0;
                for(Object i : params) {
                    handles.add(loadObjectInstance(method, i, returnValueResults, holder.constructor.getParameterTypes()[count++]));
                }
                out = method.newInstance(ofConstructor(holder.constructor.getDeclaringClass(), holder.constructor.getParameterTypes()), handles.toArray(new ResultHandle[handles.size()]));
            } else {
                try {
                    param.getClass().getDeclaredConstructor();
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("Unable to serialize objects of type " + param.getClass() + " to bytecode as it has no default constructor");
                }

                out = method.newInstance(ofConstructor(param.getClass()));
            }
            returnValueResults.put(param, out);
            if (param instanceof Collection) {
                for (Object i : (Collection) param) {
                    ResultHandle val = loadObjectInstance(method, i, returnValueResults, i.getClass());
                    method.invokeInterfaceMethod(COLLECTION_ADD, out, val);
                }
            }
            if (param instanceof Map) {
                for (Map.Entry<?, ?> i : ((Map<?, ?>) param).entrySet()) {
                    ResultHandle key = loadObjectInstance(method, i.getKey(), returnValueResults, i.getKey().getClass());
                    ResultHandle val = i.getValue() != null ? loadObjectInstance(method, i.getValue(), returnValueResults, i.getValue().getClass()) : null;
                    method.invokeInterfaceMethod(MAP_PUT, out, key, val);
                }
            }
            PropertyDescriptor[] desc = PropertyUtils.getPropertyDescriptors(param);
            for (PropertyDescriptor i : desc) {
                if(i.getReadMethod() != null && i.getWriteMethod() == null ) {
                    try {
                    //read only prop, we may still be able to do stuff with it if it is a collection
                    if(Collection.class.isAssignableFrom(i.getPropertyType())) {
                        //special case, a collection with only a read method
                        //we assume we can just add to the connection

                        Collection propertyValue = (Collection) PropertyUtils.getProperty(param, i.getName());
                        if(!propertyValue.isEmpty()) {
                            ResultHandle prop = method.invokeVirtualMethod(MethodDescriptor.ofMethod(i.getReadMethod()), out);
                            for (Object c : propertyValue) {
                                ResultHandle toAdd = loadObjectInstance(method, c, returnValueResults, Object.class);
                                method.invokeInterfaceMethod(COLLECTION_ADD, prop, toAdd);
                            }
                        }

                    } else if(Map.class.isAssignableFrom(i.getPropertyType())) {
                        //special case, a map with only a read method
                        //we assume we can just add to the map

                        Map<Object, Object> propertyValue = (Map<Object, Object>)PropertyUtils.getProperty(param, i.getName());
                        if(!propertyValue.isEmpty()) {
                            ResultHandle prop = method.invokeVirtualMethod(MethodDescriptor.ofMethod(i.getReadMethod()), out);
                            for (Map.Entry<Object, Object> entry : propertyValue.entrySet()) {
                                ResultHandle key = loadObjectInstance(method, entry.getKey(), returnValueResults, Object.class);
                                ResultHandle val = entry.getValue() != null ? loadObjectInstance(method, entry.getValue(), returnValueResults, Object.class) : method.loadNull();
                                method.invokeInterfaceMethod(MAP_PUT, prop, key, val);
                            }
                        }
                    }

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else if (i.getReadMethod() != null && i.getWriteMethod() != null) {
                    try {
                        Object propertyValue = PropertyUtils.getProperty(param, i.getName());
                        if (propertyValue == null) {
                            //we just assume properties are null by default
                            //TODO: is this a valid assumption? Should we check this by creating an instance?
                            continue;
                        }
                        Class propertyType = i.getPropertyType();
                        if (i.getReadMethod().getReturnType() != i.getWriteMethod().getParameterTypes()[0]) {
                            //this is a weird situation where the reader and writer are different types
                            //we iterate and try and find a valid setter method for the type we have
                            //OpenAPI does some weird stuff like this

                            for (Method m : param.getClass().getMethods()) {
                                if (m.getName().equals(i.getWriteMethod().getName())) {
                                    if (m.getParameterTypes().length > 0 && m.getParameterTypes()[0].isAssignableFrom(param.getClass())) {
                                        propertyType = m.getParameterTypes()[0];
                                        break;
                                    }
                                }
                            }
                        }
                        ResultHandle val = loadObjectInstance(method, propertyValue, returnValueResults, i.getPropertyType());
                        method.invokeVirtualMethod(ofMethod(param.getClass(), i.getWriteMethod().getName(), void.class, propertyType), out, val);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        returnValueResults.put(param, out);
        return out;
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

    static final class SubstitutionHolder {
        final Class<?> from;
        final Class<?> to;
        final Class<? extends ObjectSubstitution<?, ?>> sub;

        SubstitutionHolder(Class<?> from, Class<?> to, Class<? extends ObjectSubstitution<?, ?>> sub) {
            this.from = from;
            this.to = to;
            this.sub = sub;
        }
    }

    static final class NonDefaultConstructorHolder {
        final Constructor<?> constructor;
        final Function<Object, List<Object>> paramGenerator;

        NonDefaultConstructorHolder(Constructor<?> constructor, Function<Object, List<Object>> paramGenerator) {
            this.constructor = constructor;
            this.paramGenerator = paramGenerator;
        }
    }

}
