package io.quarkus.deployment.recording;

import static io.quarkus.gizmo.MethodDescriptor.ofConstructor;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.beans.PropertyDescriptor;
import java.io.Closeable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.invocation.proxy.ProxyConfiguration;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.wildfly.common.Assert;

import io.quarkus.deployment.ClassOutput;
import io.quarkus.deployment.recording.AnnotationProxyProvider.AnnotationProxy;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.runtime.ObjectSubstitution;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.StartupContext;
import io.quarkus.runtime.StartupTask;

/**
 * A class that can be used to record invocations to bytecode so they can be replayed later. This is done through the
 * use of class recorders and recording proxies.
 * <p>
 * A class recorder is simply a stateless class with a no arg constructor. This recorder will contain the runtime logic
 * used to bootstrap the various frameworks.
 * <p>
 * A recording proxy is a proxy of a recorder that records all invocations on the recorder, and then writes out a sequence
 * of java bytecode that performs the same invocations.
 * <p>
 * There are some limitations on what can be recorded. Only the following objects are allowed as parameters to
 * recording proxies:
 * <p>
 * - primitives
 * - String
 * - Class (see {@link #classProxy(String)} to handle classes that are not loadable at generation time)
 * - Objects with a no-arg constructor and getter/setters for all properties
 * - Any arbitrary object via the {@link #registerSubstitution(Class, Class, Class)} mechanism
 * - arrays, lists and maps of the above
 */
public class BytecodeRecorderImpl implements RecorderContext {

    private static final Class<?> SINGLETON_LIST_CLASS = Collections.singletonList(1).getClass();
    private static final Class<?> SINGLETON_SET_CLASS = Collections.singleton(1).getClass();
    private static final Class<?> SINGLETON_MAP_CLASS = Collections.singletonMap(1, 1).getClass();

    private static final AtomicInteger COUNT = new AtomicInteger();
    private static final AtomicInteger OUTPUT_COUNT = new AtomicInteger();
    private static final String BASE_PACKAGE = "io.quarkus.deployment.steps.";

    private static final String PROXY_KEY = "proxykey";

    private static final MethodDescriptor COLLECTION_ADD = ofMethod(Collection.class, "add", boolean.class, Object.class);
    private static final MethodDescriptor MAP_PUT = ofMethod(Map.class, "put", Object.class, Object.class, Object.class);

    private final boolean staticInit;
    private final ClassLoader classLoader;

    private final Map<Class<?>, Object> existingProxyClasses = new HashMap<>();
    private final List<BytecodeInstruction> storedMethodCalls = new ArrayList<>();

    private final Map<Class, ProxyFactory<?>> returnValueProxy = new HashMap<>();
    private final IdentityHashMap<Class<?>, String> classProxies = new IdentityHashMap<>();
    private final Map<Class<?>, SubstitutionHolder> substitutions = new HashMap<>();
    private final Map<Class<?>, NonDefaultConstructorHolder> nonDefaultConstructors = new HashMap<>();
    private final String className;

    private final List<ObjectLoader> loaders = new ArrayList<>();

    /**
     * the maximum number of instruction groups that can be added to a method. This is to limit the size of the method
     * so that the 65k limit is not reached.
     *
     * This is fairly arbitrary, as there is no fixed size for the instruction groups, but in practice this limit
     * seems to be fairly reasonable
     */
    private static final int MAX_INSTRUCTION_GROUPS = 300;

    private int deferredParameterCount = 0;
    private boolean loadComplete;
    private PropertyUtils introspection = new PropertyUtils();

    public BytecodeRecorderImpl(ClassLoader classLoader, boolean staticInit, String className) {
        this.classLoader = classLoader;
        this.staticInit = staticInit;
        this.className = className;
    }

    public BytecodeRecorderImpl(boolean staticInit, String buildStepName, String methodName) {
        this(Thread.currentThread().getContextClassLoader(), staticInit,
                BASE_PACKAGE + buildStepName + "$" + methodName + OUTPUT_COUNT.incrementAndGet());
    }

    public boolean isEmpty() {
        return storedMethodCalls.isEmpty();
    }

    @Override
    public <F, T> void registerSubstitution(Class<F> from, Class<T> to,
            Class<? extends ObjectSubstitution<F, T>> substitution) {
        substitutions.put(from, new SubstitutionHolder(from, to, substitution));
    }

    @Override
    public <T> void registerNonDefaultConstructor(Constructor<T> constructor, Function<T, List<Object>> parameters) {
        nonDefaultConstructors.put(constructor.getDeclaringClass(),
                new NonDefaultConstructorHolder(constructor, (Function<Object, List<Object>>) parameters));
    }

    @Override
    public void registerObjectLoader(ObjectLoader loader) {
        Assert.checkNotNullParam("loader", loader);
        loaders.add(loader);
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
            ProxyInstance ret = getProxyInstance(RuntimeValue.class);
            NewInstance instance = new NewInstance(name, ret.proxy, ret.key);
            storedMethodCalls.add(instance);
            return (RuntimeValue<T>) ret.proxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isProxiable(Class<?> returnType) {

        if (returnType.isPrimitive()) {
            return false;
        }
        if (Modifier.isFinal(returnType.getModifiers())) {
            return false;
        }
        boolean returnInterface = returnType.isInterface();
        if (!returnInterface) {
            try {
                returnType.getConstructor();
            } catch (NoSuchMethodException e) {
                return false;
            }
        }
        return true;
    }

    public <T> T getRecordingProxy(Class<T> theClass) {
        if (existingProxyClasses.containsKey(theClass)) {
            return theClass.cast(existingProxyClasses.get(theClass));
        }
        String proxyName = getClass().getName() + "$$RecordingProxyProxy" + COUNT.incrementAndGet();
        ProxyFactory<T> factory = new ProxyFactory<T>(new ProxyConfiguration<T>()
                .setSuperClass(theClass)
                .setClassLoader(classLoader)
                .setProxyName(proxyName));
        try {
            T recordingProxy = factory.newInstance(new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    StoredMethodCall storedMethodCall = new StoredMethodCall(theClass, method, args);
                    storedMethodCalls.add(storedMethodCall);
                    Class<?> returnType = method.getReturnType();
                    if (method.getName().equals("toString")
                            && method.getParameterTypes().length == 0
                            && returnType.equals(String.class)) {
                        return proxyName;
                    }

                    boolean voidMethod = method.getReturnType().equals(void.class);
                    if (!voidMethod && !isProxiable(method.getReturnType())) {
                        throw new RuntimeException("Cannot use " + method
                                + " as a recorder method as the return type cannot be proxied. Use RuntimeValue to wrap the return value instead.");
                    }
                    if (voidMethod) {
                        return null;
                    }
                    ProxyInstance instance = getProxyInstance(returnType);
                    if (instance == null)
                        return null;

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
                if (method.getName().equals("toString")
                        && method.getParameterTypes().length == 0
                        && method.getReturnType().equals(String.class)) {
                    return "Runtime proxy of " + returnType + " with id " + key;
                }
                throw new RuntimeException(
                        "You cannot invoke " + method.getName()
                                + "() directly on an object returned from the bytecode recorder, you can only pass it back into the recorder as a parameter");
            }
        });
        ProxyInstance instance = new ProxyInstance(proxyInstance, key);
        return instance;
    }

    public String getClassName() {
        return className;
    }

    public void writeBytecode(ClassOutput classOutput) {
        try {
            ClassCreator file = ClassCreator.builder().classOutput(ClassOutput.gizmoAdaptor(classOutput, true))
                    .className(className)
                    .superClass(Object.class).interfaces(StartupTask.class).build();
            MethodCreator mainMethod = file.getMethodCreator("deploy", void.class, StartupContext.class);
            //now create instances of all the classes we invoke on and store them in variables as well
            Map<Class, DeferredArrayStoreParameter> classInstanceVariables = new HashMap<>();

            Map<Object, DeferredParameter> parameterMap = new IdentityHashMap<>();

            //THIS IS FAIRLY COMPLEX
            //the simple approach of just writing out the serialized invocations and method parameters as they are needed
            //runs into trouble if you have a moderate number of items (in practice we seem to hit it around the 1k item
            //mark, so apps with 1k entities or endpoints will fail)

            //to get around this we break the code up into multiple methods, however this is not as simple as it sounds
            //because it is not just the number of invocations that can be a problem but also the size of collections
            //we need to account for both collections with a large number or items, and recorders with a large
            //number of invocations.

            //we solve this by breaking up all serialized state into a global array, that is passed between methods
            //and non-primitive that is serialized will be stored into this array in a specific index, which allows
            //for access from any method without needing to track ResultHandle's between methods and attempt to
            //calculate which methods require which data

            //note that this does not necessarily give the most efficient bytecode, however it does seem to be the simplest
            //implementation wise.

            //the first step is to create list of all these values that need to be placed into the array. We do that
            //here, as well as creating the recorder instances (and also preparing them to be stored in the array)
            for (BytecodeInstruction set : storedMethodCalls) {
                if (set instanceof StoredMethodCall) {
                    StoredMethodCall call = (StoredMethodCall) set;
                    if (!classInstanceVariables.containsKey(call.theClass)) {
                        //this is a new recorder, create a deffered value that will allocate an array position for
                        //the recorder
                        DeferredArrayStoreParameter value = new DeferredArrayStoreParameter() {
                            @Override
                            ResultHandle createValue(MethodContext context, MethodCreator method, ResultHandle array) {
                                return method.newInstance(ofConstructor(call.theClass));
                            }
                        };
                        classInstanceVariables.put(call.theClass, value);
                    }
                    //for every parameter that was passed into the method we create a deferred value
                    //this will allocate a space in the array, so the value can be deserialized correctly
                    //even if the code for an invocation is split over several methods
                    for (int i = 0; i < call.parameters.length; ++i) {
                        call.deferredParameters[i] = loadObjectInstance(call.parameters[i], parameterMap,
                                call.method.getParameterTypes()[i]);
                    }
                }
            }

            //when this is true it is no longer possible to allocate items in the array. this is a guard against programmer error

            loadComplete = true;
            //now we know know many items we have, create the array
            ResultHandle array = mainMethod.newArray(Object.class, mainMethod.load(deferredParameterCount));

            //this context manages the creation of new methods
            //it tracks the number of instruction groups and when they hit a threshold it
            //allocates a new method
            SplitMethodContext context = new SplitMethodContext(array, mainMethod, file);

            //now we invoke the actual method call
            for (BytecodeInstruction set : storedMethodCalls) {

                if (set instanceof StoredMethodCall) {
                    //this instruction is a recorder invocation
                    StoredMethodCall call = (StoredMethodCall) set;
                    for (int i = 0; i < call.parameters.length; ++i) {
                        //we need to doPrepare the loading before the write instruction call
                        //this actually writes out the code to load the methods calls parameters
                        //this code can end up in a different method to the actual call itself (or even multiple methods)
                        //so this has to be outside the writeInstruction call, which will naturally group everything inside
                        //it into a single method
                        call.deferredParameters[i].prepare(context);
                    }
                    final DeferredArrayStoreParameter recorderInstance = classInstanceVariables.get(call.theClass);
                    recorderInstance.prepare(context);
                    //write the method invocation. Everything in the instruction group is scoped to a single method
                    context.writeInstruction(new InstructionGroup() {
                        @Override
                        public void write(MethodContext context, MethodCreator method, ResultHandle array) {
                            ResultHandle[] params = new ResultHandle[call.parameters.length];

                            //now we actually load the arguments
                            //this will retreive them from the array and create a ResultHandle
                            //(or possible re-use an existing ResultHandler if there is already one for the current method)
                            for (int i = 0; i < call.parameters.length; ++i) {
                                params[i] = context.loadDeferred(call.deferredParameters[i]);
                            }
                            //do the invocation
                            ResultHandle callResult = method.invokeVirtualMethod(ofMethod(call.method.getDeclaringClass(),
                                    call.method.getName(), call.method.getReturnType(), call.method.getParameterTypes()),
                                    context.loadDeferred(recorderInstance), params);

                            if (call.method.getReturnType() != void.class) {
                                if (call.returnedProxy != null) {
                                    //if the invocation had a return valye put it in the startup context
                                    //to make it availible to other recorders (and also this recorder)
                                    method.invokeVirtualMethod(
                                            ofMethod(StartupContext.class, "putValue", void.class, String.class, Object.class),
                                            method.getMethodParam(0), method.load(call.proxyId), callResult);
                                }
                            }
                        }
                    });
                } else if (set instanceof NewInstance) {
                    context.writeInstruction(new InstructionGroup() {
                        @Override
                        public void write(MethodContext context, MethodCreator method, ResultHandle array) {
                            //this instruction creates a new instance
                            //it just goes in the startup context
                            NewInstance ni = (NewInstance) set;
                            ResultHandle val = method.newInstance(ofConstructor(ni.theClass));
                            ResultHandle rv = method.newInstance(ofConstructor(RuntimeValue.class, Object.class), val);
                            method.invokeVirtualMethod(
                                    ofMethod(StartupContext.class, "putValue", void.class, String.class, Object.class),
                                    method.getMethodParam(0), method.load(ni.proxyId), rv);
                        }
                    });
                } else {
                    throw new RuntimeException("unknown type " + set);
                }

            }
            context.close();
            mainMethod.returnValue(null);
            file.close();
        } finally {
            introspection.close();
        }
    }

    /**
     * Returns a representation of a serialized parameter.
     */
    private DeferredParameter loadObjectInstance(Object param, Map<Object, DeferredParameter> existing, Class<?> expectedType) {
        if (loadComplete) {
            throw new RuntimeException("All parameters have already been loaded, it is too late to call loadObjectInstance");
        }
        if (existing.containsKey(param)) {
            return existing.get(param);
        }
        DeferredParameter ret = loadObjectInstanceImpl(param, existing, expectedType);
        existing.put(param, ret);
        return ret;
    }

    /**
     * Returns a representation of a serialized parameter.
     */
    private DeferredParameter loadObjectInstanceImpl(Object param, Map<Object, DeferredParameter> existing,
            Class<?> expectedType) {
        //null is easy
        if (param == null) {
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext creator, MethodCreator method, ResultHandle array) {
                    return method.loadNull();
                }
            };
        }
        //check the loaded object support (i.e. config) to see if this is a config item
        DeferredParameter loadedObject = findLoaded(param);
        if (loadedObject != null) {
            return loadedObject;
        }

        //Handle empty collections as returned by the Collections object
        loadedObject = handleCollectionsObjects(param, existing);
        if (loadedObject != null) {
            return loadedObject;
        }

        //create the appropriate DeferredParmater, a lot of these a fairly simple constant values,
        //but some are quite complex when dealing with objects and collections
        if (substitutions.containsKey(param.getClass()) || substitutions.containsKey(expectedType)) {
            //check for substitution types, if present we invoke recursivly on the substitution
            SubstitutionHolder holder = substitutions.get(param.getClass());
            if (holder == null) {
                holder = substitutions.get(expectedType);
            }
            try {
                ObjectSubstitution substitution = holder.sub.newInstance();
                Object res = substitution.serialize(param);
                DeferredParameter serialized = loadObjectInstance(res, existing, holder.to);
                SubstitutionHolder finalHolder = holder;
                return new DeferredArrayStoreParameter() {

                    @Override
                    void doPrepare(MethodContext context) {
                        serialized.prepare(context);
                        super.doPrepare(context);
                    }

                    @Override
                    ResultHandle createValue(MethodContext creator, MethodCreator method, ResultHandle array) {
                        ResultHandle subInstance = method.newInstance(MethodDescriptor.ofConstructor(finalHolder.sub));
                        return method.invokeInterfaceMethod(
                                ofMethod(ObjectSubstitution.class, "deserialize", Object.class, Object.class), subInstance,
                                creator.loadDeferred(serialized));
                    }
                };

            } catch (Exception e) {
                throw new RuntimeException("Failed to substitute " + param, e);
            }

        } else if (param instanceof Optional) {
            Optional val = (Optional) param;
            if (val.isPresent()) {
                DeferredParameter res = loadObjectInstance(val.get(), existing, Object.class);
                return new DeferredArrayStoreParameter() {
                    @Override
                    ResultHandle createValue(MethodContext context, MethodCreator method, ResultHandle array) {
                        return method.invokeStaticMethod(ofMethod(Optional.class, "of", Optional.class, Object.class),
                                context.loadDeferred(res));
                    }
                };
            } else {
                return new DeferredArrayStoreParameter() {
                    @Override
                    ResultHandle createValue(MethodContext context, MethodCreator method, ResultHandle array) {
                        return method.invokeStaticMethod(ofMethod(Optional.class, "empty", Optional.class));
                    }
                };
            }
        } else if (param instanceof String) {
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method.load((String) param);
                }
            };
        } else if (param instanceof Integer) {
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method.invokeStaticMethod(ofMethod(Integer.class, "valueOf", Integer.class, int.class),
                            method.load((Integer) param));
                }
            };
        } else if (param instanceof Boolean) {
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method.invokeStaticMethod(ofMethod(Boolean.class, "valueOf", Boolean.class, boolean.class),
                            method.load((Boolean) param));
                }
            };
        } else if (param instanceof URL) {
            String url = ((URL) param).toExternalForm();
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    AssignableResultHandle value = method.createVariable(URL.class);
                    try (TryBlock et = method.tryBlock()) {
                        et.assign(value, et.newInstance(MethodDescriptor.ofConstructor(URL.class, String.class), et.load(url)));
                        try (CatchBlockCreator malformed = et.addCatch(MalformedURLException.class)) {
                            malformed.throwException(RuntimeException.class, "Malformed URL", malformed.getCaughtException());
                        }
                    }
                    return value;
                }
            };
        } else if (param instanceof Enum) {
            Enum e = (Enum) param;
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    ResultHandle nm = method.load(e.name());
                    return method.invokeStaticMethod(
                            ofMethod(e.getDeclaringClass(), "valueOf", e.getDeclaringClass(), String.class),
                            nm);
                }
            };
        } else if (param instanceof ReturnedProxy) {
            //if this is a proxy we just grab the value from the StartupContext
            ReturnedProxy rp = (ReturnedProxy) param;
            if (!rp.__static$$init() && staticInit) {
                throw new RuntimeException("Invalid proxy passed to recorder. " + rp
                        + " was created in a runtime recorder method, while this recorder is for a static init method. The object will not have been created at the time this method is run.");
            }
            String proxyId = rp.__returned$proxy$key();
            //because this is the result of a method invocation that may not have happened at param deserialization time
            //we just load it from the startup context
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method.invokeVirtualMethod(ofMethod(StartupContext.class, "getValue", Object.class, String.class),
                            method.getMethodParam(0), method.load(proxyId));
                }
            };
        } else if (param instanceof Class<?>) {
            if (!((Class) param).isPrimitive()) {
                // Only try to load the class by name if it is not a primitive class
                String name = classProxies.get(param);
                if (name == null) {
                    name = ((Class) param).getName();
                }
                String finalName = name;
                return new DeferredParameter() {
                    @Override
                    ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {

                        ResultHandle currentThread = method
                                .invokeStaticMethod(ofMethod(Thread.class, "currentThread", Thread.class));
                        ResultHandle tccl = method.invokeVirtualMethod(
                                ofMethod(Thread.class, "getContextClassLoader", ClassLoader.class),
                                currentThread);
                        return method.invokeStaticMethod(
                                ofMethod(Class.class, "forName", Class.class, String.class, boolean.class, ClassLoader.class),
                                method.load(finalName), method.load(true), tccl);
                    }
                };
            } else {
                // Else load the primitive type by reference; double.class => Class var9 = Double.TYPE;
                return new DeferredParameter() {
                    @Override
                    ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                        return method.loadClass((Class) param);
                    }
                };
            }
        } else if (expectedType == boolean.class) {
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method.load((boolean) param);
                }
            };
        } else if (expectedType == Boolean.class) {
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method.invokeStaticMethod(ofMethod(Boolean.class, "valueOf", Boolean.class, boolean.class),
                            method.load((boolean) param));
                }
            };
        } else if (expectedType == int.class) {
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method.load((int) param);
                }
            };
        } else if (expectedType == Integer.class) {
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method.invokeStaticMethod(ofMethod(Integer.class, "valueOf", Integer.class, int.class),
                            method.load((int) param));
                }
            };
        } else if (expectedType == short.class) {
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method.load((short) param);
                }
            };
        } else if (expectedType == Short.class || param instanceof Short) {
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method.invokeStaticMethod(ofMethod(Short.class, "valueOf", Short.class, short.class),
                            method.load((short) param));
                }
            };
        } else if (expectedType == byte.class) {
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method.load((byte) param);
                }
            };
        } else if (expectedType == Byte.class || param instanceof Byte) {
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method.invokeStaticMethod(ofMethod(Byte.class, "valueOf", Byte.class, byte.class),
                            method.load((byte) param));
                }
            };
        } else if (expectedType == char.class) {
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method.load((char) param);
                }
            };
        } else if (expectedType == Character.class || param instanceof Character) {
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method.invokeStaticMethod(ofMethod(Character.class, "valueOf", Character.class, char.class),
                            method.load((char) param));
                }
            };
        } else if (expectedType == long.class) {
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method.load((long) param);
                }
            };
        } else if (expectedType == Long.class || param instanceof Long) {
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method.invokeStaticMethod(ofMethod(Long.class, "valueOf", Long.class, long.class),
                            method.load((long) param));
                }
            };
        } else if (expectedType == float.class) {
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method.load((float) param);
                }
            };
        } else if (expectedType == Float.class || param instanceof Float) {
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method.invokeStaticMethod(ofMethod(Float.class, "valueOf", Float.class, float.class),
                            method.load((float) param));
                }
            };
        } else if (expectedType == double.class) {
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method.load((double) param);
                }
            };
        } else if (expectedType == Double.class || param instanceof Double) {
            return new DeferredParameter() {
                @Override
                ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method.invokeStaticMethod(ofMethod(Double.class, "valueOf", Double.class, double.class),
                            method.load((double) param));
                }
            };
        } else if (expectedType.isArray()) {
            int length = Array.getLength(param);
            DeferredParameter[] components = new DeferredParameter[length];

            for (int i = 0; i < length; ++i) {
                DeferredParameter component = loadObjectInstance(Array.get(param, i), existing,
                        expectedType.getComponentType());
                components[i] = component;
            }
            return new DeferredArrayStoreParameter() {

                @Override
                void doPrepare(MethodContext context) {
                    for (int i = 0; i < length; ++i) {
                        components[i].prepare(context);
                    }
                    super.doPrepare(context);
                }

                @Override
                ResultHandle createValue(MethodContext context, MethodCreator method, ResultHandle array) {
                    //TODO large arrays can still generate a fair bit of bytecode, and there appears to be a gizmo issue that prevents casting to an array
                    //fix this later
                    ResultHandle out = method.newArray(expectedType.getComponentType(), method.load(length));
                    for (int i = 0; i < length; ++i) {
                        method.writeArrayValue(out, i, context.loadDeferred(components[i]));
                    }
                    return out;
                }
            };
        } else if (AnnotationProxy.class.isAssignableFrom(expectedType)) {
            // new com.foo.MyAnnotation_Proxy_AnnotationLiteral("foo")
            AnnotationProxy annotationProxy = (AnnotationProxy) param;
            List<MethodInfo> constructorParams = annotationProxy.getAnnotationClass().methods().stream()
                    .filter(m -> !m.name().equals("<clinit>") && !m.name().equals("<init>"))
                    .collect(Collectors.toList());
            Map<String, AnnotationValue> annotationValues = annotationProxy.getAnnotationInstance().values().stream()
                    .collect(Collectors.toMap(AnnotationValue::name, Function.identity()));
            DeferredParameter[] constructorParamsHandles = new DeferredParameter[constructorParams.size()];

            for (ListIterator<MethodInfo> iterator = constructorParams.listIterator(); iterator.hasNext();) {
                MethodInfo valueMethod = iterator.next();
                AnnotationValue value = annotationValues.get(valueMethod.name());
                if (value == null) {
                    // method.invokeInterfaceMethod(MAP_PUT, valuesHandle, method.load(entry.getKey()), loadObjectInstance(method, entry.getValue(), returnValueResults, entry.getValue().getClass()));
                    Object defaultValue = annotationProxy.getDefaultValues().get(valueMethod.name());
                    if (defaultValue != null) {
                        constructorParamsHandles[iterator.previousIndex()] = loadObjectInstance(defaultValue,
                                existing, defaultValue.getClass());
                        continue;
                    }
                    if (value == null) {
                        value = valueMethod.defaultValue();
                    }
                }
                if (value == null) {
                    throw new NullPointerException("Value not set for " + param);
                }
                DeferredParameter retValue = loadValue(value, annotationProxy.getAnnotationClass(), valueMethod);
                constructorParamsHandles[iterator.previousIndex()] = retValue;
            }
            return new DeferredArrayStoreParameter() {
                @Override
                ResultHandle createValue(MethodContext context, MethodCreator method, ResultHandle array) {
                    return method
                            .newInstance(MethodDescriptor.ofConstructor(annotationProxy.getAnnotationLiteralType(),
                                    constructorParams.stream().map(m -> m.returnType().name().toString()).toArray()),
                                    Arrays.stream(constructorParamsHandles).map(m -> context.loadDeferred(m))
                                            .toArray(ResultHandle[]::new));
                }
            };

        } else {
            return loadComplexObject(param, existing, expectedType);
        }
    }

    /**
     * Loads objects returned from the {@link Collections} static utility methods. If this value is not of the type
     * that this method can handle then null is returned.
     *
     * @param param The object to load
     * @param existing
     * @return
     */
    private DeferredParameter handleCollectionsObjects(Object param, Map<Object, DeferredParameter> existing) {
        if (param instanceof Collection) {
            if (param.getClass().equals(Collections.emptyList().getClass())) {
                return new DeferredParameter() {
                    @Override
                    ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                        return method.invokeStaticMethod(MethodDescriptor.ofMethod(Collections.class, "emptyList", List.class));
                    }
                };
            } else if (param.getClass().equals(Collections.emptySet().getClass())) {
                return new DeferredParameter() {
                    @Override
                    ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                        return method.invokeStaticMethod(MethodDescriptor.ofMethod(Collections.class, "emptySet", Set.class));
                    }
                };
            } else if (param.getClass().equals(Collections.emptySortedSet().getClass())) {
                return new DeferredParameter() {
                    @Override
                    ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                        return method.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Collections.class, "emptySortedSet", SortedSet.class));
                    }
                };
            } else if (param.getClass().equals(Collections.emptyNavigableSet().getClass())) {
                return new DeferredParameter() {
                    @Override
                    ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                        return method.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Collections.class, "emptyNavigableSet", NavigableSet.class));
                    }
                };
            } else if (param.getClass().equals(SINGLETON_LIST_CLASS)) {
                DeferredParameter deferred = loadObjectInstance(((List) param).get(0), existing, Object.class);
                return new DeferredParameter() {
                    @Override
                    void doPrepare(MethodContext context) {
                        super.doPrepare(context);
                        deferred.doPrepare(context);
                    }

                    @Override
                    ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                        ResultHandle res = context.loadDeferred(deferred);
                        return method.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Collections.class, "singletonList", List.class, Object.class), res);
                    }
                };
            } else if (param.getClass().equals(SINGLETON_SET_CLASS)) {
                DeferredParameter deferred = loadObjectInstance(((Set) param).iterator().next(), existing, Object.class);
                return new DeferredParameter() {
                    @Override
                    void doPrepare(MethodContext context) {
                        super.doPrepare(context);
                        deferred.doPrepare(context);
                    }

                    @Override
                    ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                        ResultHandle res = context.loadDeferred(deferred);
                        return method.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Collections.class, "singleton", Set.class, Object.class), res);
                    }
                };
            }
        } else if (param instanceof Map) {
            if (param.getClass().equals(Collections.emptyMap().getClass())) {
                return new DeferredParameter() {
                    @Override
                    ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                        return method.invokeStaticMethod(MethodDescriptor.ofMethod(Collections.class, "emptyMap", Map.class));
                    }
                };
            } else if (param.getClass().equals(Collections.emptySortedMap().getClass())) {
                return new DeferredParameter() {
                    @Override
                    ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                        return method.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Collections.class, "emptySortedMap", SortedMap.class));
                    }
                };
            } else if (param.getClass().equals(Collections.emptyNavigableMap().getClass())) {
                return new DeferredParameter() {
                    @Override
                    ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                        return method.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Collections.class, "emptyNavigableMap", SortedMap.class));
                    }
                };
            } else if (param.getClass().equals(SINGLETON_MAP_CLASS)) {
                Map.Entry<?, ?> entry = ((Map<?, ?>) param).entrySet().iterator().next();

                DeferredParameter key = loadObjectInstance(entry.getKey(), existing, Object.class);
                DeferredParameter value = loadObjectInstance(entry.getValue(), existing, Object.class);
                return new DeferredParameter() {
                    @Override
                    void doPrepare(MethodContext context) {
                        super.doPrepare(context);
                        key.doPrepare(context);
                        value.doPrepare(context);
                    }

                    @Override
                    ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
                        ResultHandle k = context.loadDeferred(key);
                        ResultHandle v = context.loadDeferred(value);
                        return method.invokeStaticMethod(
                                MethodDescriptor.ofMethod(Collections.class, "singletonMap", Map.class, Object.class,
                                        Object.class),
                                k, v);
                    }
                };
            }
        }

        return null;
    }

    /**
     * Created a {@link DeferredParameter} to load a complex object, such as a javabean or collection. This is basically
     * just an extension of {@link #loadObjectInstanceImpl(Object, Map, Class)} but it removes some of the more complex
     * code from that method.
     *
     * @param param The object to load
     * @param existing The existing object map
     * @param expectedType The expected type of the object
     * @return
     */
    private DeferredParameter loadComplexObject(Object param, Map<Object, DeferredParameter> existing, Class<?> expectedType) {
        //a list of steps that are performed on the object after it has been created
        //we need to create all these first, to ensure the required objects have already
        //been deserialized
        List<SerialzationStep> setupSteps = new ArrayList<>();

        if (param instanceof Collection) {
            //if this is a collection we want to serialize every element
            for (Object i : (Collection) param) {
                DeferredParameter val = loadObjectInstance(i, existing, i.getClass());
                setupSteps.add(new SerialzationStep() {
                    @Override
                    public void handle(MethodContext context, MethodCreator method, DeferredArrayStoreParameter out) {
                        //each step can happen in a new method, so it is safe to do this
                        method.invokeInterfaceMethod(COLLECTION_ADD, context.loadDeferred(out), context.loadDeferred(val));
                    }

                    @Override
                    public void prepare(MethodContext context) {
                        //handle the value serialization
                        val.prepare(context);
                    }
                });
            }
        }
        if (param instanceof Map) {
            //map works the same as collection
            for (Map.Entry<?, ?> i : ((Map<?, ?>) param).entrySet()) {
                DeferredParameter key = loadObjectInstance(i.getKey(), existing, i.getKey().getClass());
                DeferredParameter val = i.getValue() != null
                        ? loadObjectInstance(i.getValue(), existing, i.getValue().getClass())
                        : null;
                setupSteps.add(new SerialzationStep() {
                    @Override
                    public void handle(MethodContext context, MethodCreator method, DeferredArrayStoreParameter out) {
                        method.invokeInterfaceMethod(MAP_PUT, context.loadDeferred(out), context.loadDeferred(key),
                                context.loadDeferred(val));
                    }

                    @Override
                    public void prepare(MethodContext context) {
                        key.prepare(context);
                        val.prepare(context);
                    }
                });
            }
        }
        Set<String> handledProperties = new HashSet<>();
        PropertyDescriptor[] desc = introspection.getPropertyDescriptors(param);
        for (PropertyDescriptor i : desc) {
            if (i.getReadMethod() != null && i.getWriteMethod() == null) {
                try {
                    //read only prop, we may still be able to do stuff with it if it is a collection
                    if (Collection.class.isAssignableFrom(i.getPropertyType())) {
                        //special case, a collection with only a read method
                        //we assume we can just add to the connection
                        handledProperties.add(i.getName());

                        Collection propertyValue = (Collection) introspection.getProperty(param, i.getName());
                        if (!propertyValue.isEmpty()) {

                            List<DeferredParameter> params = new ArrayList<>();
                            for (Object c : propertyValue) {
                                DeferredParameter toAdd = loadObjectInstance(c, existing, Object.class);
                                params.add(toAdd);

                            }
                            setupSteps.add(new SerialzationStep() {
                                @Override
                                public void handle(MethodContext context, MethodCreator method,
                                        DeferredArrayStoreParameter out) {
                                    //get the collection
                                    ResultHandle prop = method.invokeVirtualMethod(
                                            MethodDescriptor.ofMethod(i.getReadMethod()),
                                            context.loadDeferred(out));
                                    for (DeferredParameter i : params) {
                                        //add the parameter
                                        //TODO: this is not guareded against large collections, probably not an issue in practice
                                        method.invokeInterfaceMethod(COLLECTION_ADD, prop, context.loadDeferred(i));
                                    }
                                }

                                @Override
                                public void prepare(MethodContext context) {
                                    for (DeferredParameter i : params) {
                                        i.prepare(context);
                                    }
                                }
                            });
                        }

                    } else if (Map.class.isAssignableFrom(i.getPropertyType())) {
                        //special case, a map with only a read method
                        //we assume we can just add to the map
                        //similar to how collection works above

                        handledProperties.add(i.getName());
                        Map<Object, Object> propertyValue = (Map<Object, Object>) introspection.getProperty(param,
                                i.getName());
                        if (!propertyValue.isEmpty()) {
                            Map<DeferredParameter, DeferredParameter> def = new LinkedHashMap<>();
                            for (Map.Entry<Object, Object> entry : propertyValue.entrySet()) {
                                DeferredParameter key = loadObjectInstance(entry.getKey(), existing,
                                        Object.class);
                                DeferredParameter val = loadObjectInstance(entry.getValue(), existing, Object.class);
                                def.put(key, val);
                            }
                            setupSteps.add(new SerialzationStep() {
                                @Override
                                public void handle(MethodContext context, MethodCreator method,
                                        DeferredArrayStoreParameter out) {

                                    ResultHandle prop = method.invokeVirtualMethod(
                                            MethodDescriptor.ofMethod(i.getReadMethod()),
                                            context.loadDeferred(out));
                                    for (Map.Entry<DeferredParameter, DeferredParameter> e : def.entrySet()) {
                                        method.invokeInterfaceMethod(MAP_PUT, prop, context.loadDeferred(e.getKey()),
                                                context.loadDeferred(e.getValue()));
                                    }
                                }

                                @Override
                                public void prepare(MethodContext context) {
                                    for (Map.Entry<DeferredParameter, DeferredParameter> e : def.entrySet()) {
                                        e.getKey().prepare(context);
                                        e.getValue().prepare(context);
                                    }
                                }
                            });
                        }
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (i.getReadMethod() != null && i.getWriteMethod() != null) {
                //normal javabean property
                try {
                    handledProperties.add(i.getName());
                    Object propertyValue = introspection.getProperty(param, i.getName());
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
                                if (m.getParameterTypes().length > 0
                                        && m.getParameterTypes()[0].isAssignableFrom(param.getClass())) {
                                    propertyType = m.getParameterTypes()[0];
                                    break;
                                }
                            }
                        }
                    }
                    DeferredParameter val = loadObjectInstance(propertyValue, existing,
                            i.getPropertyType());
                    Class finalPropertyType = propertyType;
                    setupSteps.add(new SerialzationStep() {
                        @Override
                        public void handle(MethodContext context, MethodCreator method, DeferredArrayStoreParameter out) {
                            method.invokeVirtualMethod(
                                    ofMethod(param.getClass(), i.getWriteMethod().getName(), void.class,
                                            finalPropertyType),
                                    context.loadDeferred(out),
                                    context.loadDeferred(val));
                        }

                        @Override
                        public void prepare(MethodContext context) {
                            val.prepare(context);
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        //now handle accessible fields
        for (Field field : param.getClass().getFields()) {
            if (!Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())
                    && !handledProperties.contains(field.getName())) {

                try {
                    DeferredParameter val = loadObjectInstance(field.get(param), existing, field.getType());
                    setupSteps.add(new SerialzationStep() {
                        @Override
                        public void handle(MethodContext context, MethodCreator method, DeferredArrayStoreParameter out) {
                            method.writeInstanceField(
                                    FieldDescriptor.of(param.getClass(), field.getName(), field.getType()),
                                    context.loadDeferred(out),
                                    context.loadDeferred(val));
                        }

                        @Override
                        public void prepare(MethodContext context) {
                            val.prepare(context);
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        //check how the object is constructed
        NonDefaultConstructorHolder nonDefaultConstructorHolder = null;
        List<DeferredParameter> nonDefaultConstructorHandles = new ArrayList<>();
        if (nonDefaultConstructors.containsKey(param.getClass())) {
            nonDefaultConstructorHolder = nonDefaultConstructors.get(param.getClass());
            List<Object> params = nonDefaultConstructorHolder.paramGenerator.apply(param);
            if (params.size() != nonDefaultConstructorHolder.constructor.getParameterCount()) {
                throw new RuntimeException("Unable to serialize " + param
                        + " as the wrong number of parameters were generated for "
                        + nonDefaultConstructorHolder.constructor);
            }
            int count = 0;
            for (Object i : params) {
                nonDefaultConstructorHandles.add(
                        loadObjectInstance(i, existing,
                                nonDefaultConstructorHolder.constructor.getParameterTypes()[count++]));
            }
        }

        NonDefaultConstructorHolder finalNonDefaultConstructorHolder = nonDefaultConstructorHolder;

        //create a deferred value to represet the object itself. This allows the creation to be split
        //over multiple methods, which is important if this is a large object
        DeferredArrayStoreParameter objectValue = new DeferredArrayStoreParameter() {
            @Override
            ResultHandle createValue(MethodContext context, MethodCreator method, ResultHandle array) {
                ResultHandle out;
                //do the creation
                if (finalNonDefaultConstructorHolder != null) {
                    out = method.newInstance(
                            ofConstructor(finalNonDefaultConstructorHolder.constructor.getDeclaringClass(),
                                    finalNonDefaultConstructorHolder.constructor.getParameterTypes()),
                            nonDefaultConstructorHandles.stream().map(m -> context.loadDeferred(m))
                                    .toArray(ResultHandle[]::new));
                } else {
                    try {
                        param.getClass().getDeclaredConstructor();
                        out = method.newInstance(ofConstructor(param.getClass()));
                    } catch (NoSuchMethodException e) {
                        //fallback for collection types, such as unmodifiableMap
                        if (expectedType == Map.class) {
                            out = method.newInstance(ofConstructor(LinkedHashMap.class));
                        } else if (expectedType == List.class) {
                            out = method.newInstance(ofConstructor(ArrayList.class));
                        } else if (expectedType == Set.class) {
                            out = method.newInstance(ofConstructor(Set.class));
                        } else {
                            throw new RuntimeException("Unable to serialize objects of type " + param.getClass()
                                    + " to bytecode as it has no default constructor");
                        }
                    }
                }
                return out;
            }
        };

        //now return the actual deferred parameter that represents the result of construction
        return new DeferredArrayStoreParameter() {

            @Override
            void doPrepare(MethodContext context) {
                //this is where the object construction happens
                //first create the actial object
                objectValue.prepare(context);
                for (SerialzationStep i : setupSteps) {
                    //then prepare the steps (i.e. creating the values to be placed into this object)
                    i.prepare(context);
                }
                for (SerialzationStep i : setupSteps) {
                    //now actually run the steps (i.e. actually stick the values into the object)
                    context.writeInstruction(new InstructionGroup() {
                        @Override
                        public void write(MethodContext context, MethodCreator method, ResultHandle array) {
                            i.handle(context, method, objectValue);
                        }
                    });
                }
                super.doPrepare(context);
            }

            @Override
            ResultHandle createValue(MethodContext context, MethodCreator method, ResultHandle array) {
                //just return the already created object
                return context.loadDeferred(objectValue);
            }
        };
    }

    private DeferredParameter findLoaded(final Object param) {
        for (ObjectLoader loader : loaders) {
            if (loader.canHandleObject(param, staticInit)) {
                return new DeferredArrayStoreParameter() {
                    @Override
                    ResultHandle createValue(MethodContext creator, MethodCreator method, ResultHandle array) {
                        return loader.load(method, param, staticInit);
                    }
                };
            }
        }
        return null;
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
        final DeferredParameter[] deferredParameters;
        Object returnedProxy;
        String proxyId;

        StoredMethodCall(Class<?> theClass, Method method, Object[] parameters) {
            this.theClass = theClass;
            this.method = method;
            this.parameters = parameters;
            this.deferredParameters = new DeferredParameter[parameters.length];
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

    private static final class ProxyInstance {
        final Object proxy;
        final String key;

        ProxyInstance(Object proxy, String key) {
            this.proxy = proxy;
            this.key = key;
        }

    }

    DeferredParameter loadValue(AnnotationValue value, ClassInfo annotationClass,
            MethodInfo method) {
        //note that this is a special case, in general DeferredParameter should be added to the main parameter list
        //however in this case we know it is a constant
        return new DeferredParameter() {

            @Override
            ResultHandle doLoad(MethodContext context, MethodCreator valueMethod, ResultHandle array) {

                ResultHandle retValue;
                switch (value.kind()) {
                    case BOOLEAN:
                        retValue = valueMethod.load(value.asBoolean());
                        break;
                    case STRING:
                        retValue = valueMethod.load(value.asString());
                        break;
                    case BYTE:
                        retValue = valueMethod.load(value.asByte());
                        break;
                    case SHORT:
                        retValue = valueMethod.load(value.asShort());
                        break;
                    case LONG:
                        retValue = valueMethod.load(value.asLong());
                        break;
                    case INTEGER:
                        retValue = valueMethod.load(value.asInt());
                        break;
                    case FLOAT:
                        retValue = valueMethod.load(value.asFloat());
                        break;
                    case DOUBLE:
                        retValue = valueMethod.load(value.asDouble());
                        break;
                    case CHARACTER:
                        retValue = valueMethod.load(value.asChar());
                        break;
                    case CLASS:
                        retValue = valueMethod.loadClass(value.asClass().toString());
                        break;
                    case ARRAY:
                        retValue = arrayValue(value, valueMethod, method, annotationClass);
                        break;
                    case ENUM:
                        retValue = valueMethod
                                .readStaticField(FieldDescriptor.of(value.asEnumType().toString(), value.asEnum(),
                                        value.asEnumType().toString()));
                        break;
                    case NESTED:
                    default:
                        throw new UnsupportedOperationException("Unsupported value: " + value);
                }
                return retValue;
            }
        };
    }

    static ResultHandle arrayValue(AnnotationValue value, BytecodeCreator valueMethod, MethodInfo method,
            ClassInfo annotationClass) {
        ResultHandle retValue;
        switch (value.componentKind()) {
            case CLASS:
                Type[] classArray = value.asClassArray();
                retValue = valueMethod.newArray(componentType(method), valueMethod.load(classArray.length));
                for (int i = 0; i < classArray.length; i++) {
                    valueMethod.writeArrayValue(retValue, i, valueMethod.loadClass(classArray[i].name().toString()));
                }
                break;
            case STRING:
                String[] stringArray = value.asStringArray();
                retValue = valueMethod.newArray(componentType(method), valueMethod.load(stringArray.length));
                for (int i = 0; i < stringArray.length; i++) {
                    valueMethod.writeArrayValue(retValue, i, valueMethod.load(stringArray[i]));
                }
                break;
            case INTEGER:
                int[] intArray = value.asIntArray();
                retValue = valueMethod.newArray(componentType(method), valueMethod.load(intArray.length));
                for (int i = 0; i < intArray.length; i++) {
                    valueMethod.writeArrayValue(retValue, i, valueMethod.load(intArray[i]));
                }
                break;
            case LONG:
                long[] longArray = value.asLongArray();
                retValue = valueMethod.newArray(componentType(method), valueMethod.load(longArray.length));
                for (int i = 0; i < longArray.length; i++) {
                    valueMethod.writeArrayValue(retValue, i, valueMethod.load(longArray[i]));
                }
                break;
            case BYTE:
                byte[] byteArray = value.asByteArray();
                retValue = valueMethod.newArray(componentType(method), valueMethod.load(byteArray.length));
                for (int i = 0; i < byteArray.length; i++) {
                    valueMethod.writeArrayValue(retValue, i, valueMethod.load(byteArray[i]));
                }
                break;
            case CHARACTER:
                char[] charArray = value.asCharArray();
                retValue = valueMethod.newArray(componentType(method), valueMethod.load(charArray.length));
                for (int i = 0; i < charArray.length; i++) {
                    valueMethod.writeArrayValue(retValue, i, valueMethod.load(charArray[i]));
                }
                break;
            case ENUM:
                String[] enumArray = value.asEnumArray();
                retValue = valueMethod.newArray(componentType(method), valueMethod.load(enumArray.length));
                String enumType = componentType(method);
                for (int i = 0; i < enumArray.length; i++) {
                    valueMethod.writeArrayValue(retValue, i,
                            valueMethod.readStaticField(FieldDescriptor.of(enumType, enumArray[i], enumType)));
                }
                break;
            // TODO: handle other less common types of array components
            default:
                // Return empty array for empty arrays and unsupported types
                // For an empty array the component kind is UNKNOWN
                retValue = valueMethod.newArray(componentType(method), valueMethod.load(0));
        }
        return retValue;
    }

    static String componentType(MethodInfo method) {
        ArrayType arrayType = method.returnType().asArrayType();
        return arrayType.component().name().toString();
    }

    /**
     * A bytecode serialized value. This is an abstraction over ResultHandle, as ResultHandle
     * cannot span methods.
     *
     * Instances of DeferredParameter can be used in different methods
     */
    abstract class DeferredParameter {

        boolean prepared = false;

        /**
         * The function that is called to read the value for use. This may be by reading the value from the Object[]
         * array, or is may be a direct ldc instruction in the case of primitives.
         *
         * Code in this method is run in a single instruction group, so large objects should be serialized in the
         * {@link #doPrepare(MethodContext)} method instead
         * <p>
         * This should not be called directly, but by {@link SplitMethodContext#loadDeferred(DeferredParameter)}
         */
        abstract ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array);

        /**
         * function that prepares the value for use. This is where objects should actually be loaded into the
         * main array
         *
         * @param context The main method context.
         */
        final void prepare(MethodContext context) {
            if (!prepared) {
                prepared = true;
                doPrepare(context);
            }
        }

        void doPrepare(MethodContext context) {
        }
    }

    abstract class DeferredArrayStoreParameter extends DeferredParameter {

        final int arrayIndex;

        /**
         * method that contains the logic to actually create the stored value
         */
        abstract ResultHandle createValue(MethodContext context, MethodCreator method, ResultHandle array);

        @Override
        void doPrepare(MethodContext context) {
            //write out some bytecode to load the object into the array
            //this happens in a new instruction group
            context.writeInstruction(new InstructionGroup() {
                @Override
                public void write(MethodContext context, MethodCreator method, ResultHandle array) {
                    ResultHandle val = createValue(context, method, array);
                    method.writeArrayValue(array, arrayIndex, val);
                }
            });
            prepared = true;
        }

        @Override
        final ResultHandle doLoad(MethodContext context, MethodCreator method, ResultHandle array) {
            return method.readArrayValue(array, arrayIndex);
        }

        DeferredArrayStoreParameter() {
            arrayIndex = deferredParameterCount++;
        }

    }

    /**
     * A step that must be executed to serialize a complex object
     */
    interface SerialzationStep {

        void handle(MethodContext context, MethodCreator method, DeferredArrayStoreParameter out);

        void prepare(MethodContext context);
    }

    /**
     * class responsible for splitting the bytecode into smaller methods, to make sure that even large objects and large
     * numbers of invocations do not put us over the method limit.
     */
    class SplitMethodContext implements Closeable, MethodContext {
        final ResultHandle deferredParameterArray;
        final MethodCreator mainMethod;
        final ClassCreator classCreator;
        List<MethodCreator> allMethods = new ArrayList<>();

        int methodCount;
        int currentCount;
        MethodCreator currentMethod;
        Map<Integer, ResultHandle> currentMethodCache = new HashMap<>();

        SplitMethodContext(ResultHandle deferredParameterArray, MethodCreator mainMethod, ClassCreator classCreator) {
            this.deferredParameterArray = deferredParameterArray;
            this.mainMethod = mainMethod;
            this.classCreator = classCreator;
        }

        @Override
        public void writeInstruction(InstructionGroup writer) {
            if (currentMethod == null || currentCount++ >= MAX_INSTRUCTION_GROUPS) {
                newMethod();
            }
            FixedMethodContext c = new FixedMethodContext(this);
            c.writeInstruction(writer);
        }

        @Override
        public ResultHandle loadDeferred(DeferredParameter parameter) {
            if (currentMethod == null || currentCount++ >= MAX_INSTRUCTION_GROUPS) {
                newMethod();
            }
            FixedMethodContext c = new FixedMethodContext(this);
            return c.loadDeferred(parameter);
        }

        void newMethod() {
            currentCount = 0;
            currentMethod = classCreator.getMethodCreator(mainMethod.getMethodDescriptor().getName() + "_" + (methodCount++),
                    mainMethod.getMethodDescriptor().getReturnType(), StartupContext.class, Object[].class);
            mainMethod.invokeVirtualMethod(currentMethod.getMethodDescriptor(), mainMethod.getThis(),
                    mainMethod.getMethodParam(0), deferredParameterArray);
            currentMethodCache = new HashMap<>();
            allMethods.add(currentMethod);
        }

        @Override
        public void close() {
            for (MethodCreator i : allMethods) {
                i.returnValue(null);
            }
        }
    }

    final class FixedMethodContext implements MethodContext {
        final SplitMethodContext parent;
        final MethodCreator currentMethod;
        final Map<Integer, ResultHandle> currentMethodCache;

        FixedMethodContext(SplitMethodContext parent) {
            this.parent = parent;
            this.currentMethod = parent.currentMethod;
            this.currentMethodCache = parent.currentMethodCache;
        }

        @Override
        public void writeInstruction(InstructionGroup writer) {
            writer.write(this, currentMethod, currentMethod.getMethodParam(1));
        }

        @Override
        public ResultHandle loadDeferred(DeferredParameter parameter) {
            if (parameter instanceof DeferredArrayStoreParameter) {
                //we don't want to have to go back to the array every time
                //so we cache the result handles within the scope of the current method
                int arrayIndex = ((DeferredArrayStoreParameter) parameter).arrayIndex;
                if (currentMethodCache.containsKey(arrayIndex)) {
                    return currentMethodCache.get(arrayIndex);
                }
                ResultHandle loaded = parameter.doLoad(this, currentMethod, currentMethod.getMethodParam(1));
                if (parent.currentMethod == currentMethod) {
                    currentMethodCache.put(arrayIndex, loaded);
                    return loaded;
                } else {
                    ResultHandle ret = currentMethod.readArrayValue(currentMethod.getMethodParam(1), arrayIndex);
                    currentMethodCache.put(arrayIndex, ret);
                    return ret;
                }
            } else {
                return parameter.doLoad(this, currentMethod, currentMethod.getMethodParam(1));
            }
        }
    }

    /**
     * A group of instructions that will always be exectued in the same method
     */
    interface InstructionGroup {
        void write(MethodContext context, MethodCreator creator, ResultHandle array);
    }

    public interface MethodContext {

        void writeInstruction(InstructionGroup writer);

        ResultHandle loadDeferred(DeferredParameter parameter);
    }

}
