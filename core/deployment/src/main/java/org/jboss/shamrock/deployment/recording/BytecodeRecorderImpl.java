package org.jboss.shamrock.deployment.recording;

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
import org.jboss.shamrock.runtime.RuntimeValue;
import org.jboss.shamrock.runtime.StartupContext;
import org.jboss.shamrock.runtime.StartupTask;

public class BytecodeRecorderImpl implements RecorderContext {

    private static final AtomicInteger COUNT = new AtomicInteger();

    private static final String PROXY_KEY = "proxykey";

    private static final Map<Object, String> proxyMap = new IdentityHashMap<>();

    private static final MethodDescriptor COLLECTION_ADD = ofMethod(Collection.class, "add", boolean.class, Object.class);
    private static final MethodDescriptor MAP_PUT = ofMethod(Map.class, "put", Object.class, Object.class, Object.class);

    private final boolean staticInit;
    private final ClassLoader classLoader;
    private final MethodRecorder methodRecorder = new MethodRecorder();

    private final Map<Class, ProxyFactory<?>> returnValueProxy = new HashMap<>();
    private final IdentityHashMap<Class<?>, String> classProxies = new IdentityHashMap<>();
    private final Map<Class<?>, SubstitutionHolder> substitutions = new HashMap<>();
    private final Map<Class<?>, NonDefaultConstructorHolder> nonDefaulConstructors = new HashMap<>();

    public BytecodeRecorderImpl(ClassLoader classLoader, boolean staticInit) {
        this.classLoader = classLoader;
        this.staticInit = staticInit;
    }

    public BytecodeRecorderImpl(boolean staticInit) {
        this(Thread.currentThread().getContextClassLoader(), staticInit);
    }

    public boolean isEmpty() {
        return methodRecorder.storedMethodCalls.isEmpty();
    }

    @Override
    public <F, T> void registerSubstitution(Class<F> from, Class<T> to, Class<? extends ObjectSubstitution<F, T>> substitution) {
        substitutions.put(from, new SubstitutionHolder(from, to, substitution));
    }

    @Override
    public <T> void registerNonDefaultConstructor(Constructor<T> constructor, Function<T, List<Object>> parameters) {
        nonDefaulConstructors.put(constructor.getDeclaringClass(), new NonDefaultConstructorHolder(constructor, (Function<Object, List<Object>>) parameters));
    }

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

    @Override
    public <T> RuntimeValue<T> newInstance(String name) {
        try {
            ProxyInstance ret = methodRecorder.getProxyInstance(RuntimeValue.class);
            NewInstance instance = new NewInstance(name, ret.proxy, ret.key);
            methodRecorder.storedMethodCalls.add(instance);
            return (RuntimeValue<T>) ret.proxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
                        Class<?> returnType = method.getReturnType();
                        if (returnType.isPrimitive()) {
                            return 0;
                        }
                        if (Modifier.isFinal(returnType.getModifiers())) {
                            return null;
                        }
                        ProxyInstance instance = getProxyInstance(returnType);
                        if (instance == null) return null;

                        storedMethodCall.returnedProxy = instance.proxy;
                        storedMethodCall.proxyId = instance.key;
                        return instance.proxy;
                    }

                });
                existingProxyClasses.put(theClass, recordingProxy);
                return recordingProxy;
            } catch (IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        }

        private ProxyInstance getProxyInstance(Class<?> returnType) throws InstantiationException, IllegalAccessException {
            boolean returnInterface = returnType.isInterface();
            if (!returnInterface) {
                try {
                    returnType.getConstructor();
                } catch (NoSuchMethodException e) {
                    return null;
                }
            }
            ProxyFactory<?> proxyFactory = returnValueProxy.get(returnType);
            if (proxyFactory == null) {
                ProxyConfiguration<Object> proxyConfiguration = new ProxyConfiguration<Object>()
                        .setSuperClass(returnInterface ? Object.class : (Class) returnType)
                        .setClassLoader(classLoader)
                        .addAdditionalInterface(ReturnedProxy.class)
                        .setProxyName(getClass().getName() + "$$ReturnValueProxy" + COUNT.incrementAndGet());

                if (returnInterface) {
                    proxyConfiguration.addAdditionalInterface(returnType);
                }
                returnValueProxy.put(returnType, proxyFactory = new ProxyFactory<>(proxyConfiguration));
            }

            String key = PROXY_KEY + COUNT.incrementAndGet();
            Object proxyInstance = proxyFactory.newInstance(new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getName().equals("__returned$proxy$key")) {
                        return key;
                    }
                    if (method.getName().equals("__static$$init")) {
                        return staticInit;
                    }
                    throw new RuntimeException("You cannot invoke directly on an object returned from the bytecode recorded, you can only pass is back into the recorder as a parameter");
                }
            });
            ProxyInstance instance = new ProxyInstance(proxyInstance, key);
            return instance;
        }
    }

    public void writeBytecode(ClassOutput classOutput, String className) {
        ClassCreator file = ClassCreator.builder().classOutput(ClassOutput.gizmoAdaptor(classOutput, true)).className(className).superClass(Object.class).interfaces(StartupTask.class).build();
        MethodCreator method = file.getMethodCreator("deploy", void.class, StartupContext.class);
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
                    if (call.parameters[i] != null) {
                        Object param = call.parameters[i];
                        ResultHandle out;
                        out = loadObjectInstance(method, param, returnValueResults, targetType);
                        params[i] = out;
                    } else if (targetType == StartupContext.class) { //hack, as this is tied to StartupTask
                        params[i] = method.getMethodParam(0);
                    } else {
                        params[i] = method.loadNull();
                    }
                }
                ResultHandle callResult = method.invokeVirtualMethod(ofMethod(call.method.getDeclaringClass(), call.method.getName(), call.method.getReturnType(), call.method.getParameterTypes()), classInstanceVariables.get(call.theClass), params);

                if (call.method.getReturnType() != void.class) {
                    if (call.returnedProxy != null) {
                        returnValueResults.put(call.returnedProxy, callResult);
                        method.invokeVirtualMethod(ofMethod(StartupContext.class, "putValue", void.class, String.class, Object.class), method.getMethodParam(0), method.load(call.proxyId), callResult);
                    }
                }
            } else if (set instanceof NewInstance) {
                NewInstance ni = (NewInstance) set;
                ResultHandle val = method.newInstance(ofConstructor(ni.theClass));
                ResultHandle rv = method.newInstance(ofConstructor(RuntimeValue.class, Object.class), val);
                method.invokeVirtualMethod(ofMethod(StartupContext.class, "putValue", void.class, String.class, Object.class), method.getMethodParam(0), method.load(ni.proxyId), rv);
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
        } else if (param instanceof ReturnedProxy) {

            ReturnedProxy rp = (ReturnedProxy) param;
            if (!rp.__static$$init() && staticInit) {
                throw new RuntimeException("Invalid proxy passed to template. " + rp + " was created in a runtime recorder method, while this recorder is for a static init method. The object will not have been created at the time this method is run.");
            }
            String proxyId = rp.__returned$proxy$key();
            out = method.invokeVirtualMethod(ofMethod(StartupContext.class, "getValue", Object.class, String.class), method.getMethodParam(0), method.load(proxyId));
        } else if (param instanceof Class<?>) {
            String name = classProxies.get(param);
            if (name == null) {
                name = ((Class) param).getName();
            }
            ResultHandle currentThread = method.invokeStaticMethod(ofMethod(Thread.class, "currentThread", Thread.class));
            ResultHandle tccl = method.invokeVirtualMethod(ofMethod(Thread.class, "getContextClassLoader", ClassLoader.class), currentThread);
            out = method.invokeStaticMethod(ofMethod(Class.class, "forName", Class.class, String.class, boolean.class, ClassLoader.class), method.load(name), method.load(true), tccl);
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
            if (nonDefaulConstructors.containsKey(param.getClass())) {
                NonDefaultConstructorHolder holder = nonDefaulConstructors.get(param.getClass());
                List<Object> params = holder.paramGenerator.apply(param);
                if (params.size() != holder.constructor.getParameterCount()) {
                    throw new RuntimeException("Unable to serialize " + param + " as the wrong number of parameters were generated for " + holder.constructor);
                }
                List<ResultHandle> handles = new ArrayList<>();
                int count = 0;
                for (Object i : params) {
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
                if (i.getReadMethod() != null && i.getWriteMethod() == null) {
                    try {
                        //read only prop, we may still be able to do stuff with it if it is a collection
                        if (Collection.class.isAssignableFrom(i.getPropertyType())) {
                            //special case, a collection with only a read method
                            //we assume we can just add to the connection

                            Collection propertyValue = (Collection) PropertyUtils.getProperty(param, i.getName());
                            if (!propertyValue.isEmpty()) {
                                ResultHandle prop = method.invokeVirtualMethod(MethodDescriptor.ofMethod(i.getReadMethod()), out);
                                for (Object c : propertyValue) {
                                    ResultHandle toAdd = loadObjectInstance(method, c, returnValueResults, Object.class);
                                    method.invokeInterfaceMethod(COLLECTION_ADD, prop, toAdd);
                                }
                            }

                        } else if (Map.class.isAssignableFrom(i.getPropertyType())) {
                            //special case, a map with only a read method
                            //we assume we can just add to the map

                            Map<Object, Object> propertyValue = (Map<Object, Object>) PropertyUtils.getProperty(param, i.getName());
                            if (!propertyValue.isEmpty()) {
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

    interface BytecodeInstruction {

    }


    public interface ReturnedProxy {
        String __returned$proxy$key();

        boolean __static$$init();
    }

    static final class StoredMethodCall implements BytecodeInstruction {
        final Class<?> theClass;
        final Method method;
        final Object[] parameters;
        Object returnedProxy;
        String proxyId;

        StoredMethodCall(Class<?> theClass, Method method, Object[] parameters) {
            this.theClass = theClass;
            this.method = method;
            this.parameters = parameters;
        }
    }


    static final class NewInstance implements BytecodeInstruction {
        final String theClass;
        final Object returnedProxy;
        final String proxyId;

        NewInstance(String theClass, Object returnedProxy, String proxyId) {
            this.theClass = theClass;
            this.returnedProxy = returnedProxy;
            this.proxyId = proxyId;
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

    class ProxyInstance {
        final Object proxy;
        final String key;

        ProxyInstance(Object proxy, String key) {
            this.proxy = proxy;
            this.key = key;
        }

        public Object getProxy() {
            return proxy;
        }

        public String getKey() {
            return key;
        }
    }

}
