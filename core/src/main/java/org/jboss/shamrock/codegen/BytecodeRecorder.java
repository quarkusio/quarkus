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
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.shamrock.core.ClassOutput;
import org.jboss.shamrock.injection.Injection;
import org.jboss.shamrock.injection.InjectionInstance;
import org.jboss.shamrock.reflection.ConstructorHandle;
import org.jboss.shamrock.reflection.FieldHandle;
import org.jboss.shamrock.reflection.MethodHandle;
import org.jboss.shamrock.reflection.ReflectionContext;
import org.jboss.shamrock.reflection.RuntimeReflection;
import org.jboss.shamrock.startup.StartupContext;

public class BytecodeRecorder implements AutoCloseable {

    private static final AtomicInteger COUNT = new AtomicInteger();

    private final String className;
    private final Class<?> serviceType;
    private final ClassOutput classOutput;
    private final MethodRecorder methodRecorder;
    private final Method method;
    private final boolean runtime;

    public BytecodeRecorder(String className, Class<?> serviceType, ClassOutput classOutput, boolean runtime) {
        this.className = className;
        this.serviceType = serviceType;
        this.classOutput = classOutput;
        this.runtime = runtime;
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

    public Class<?> getServiceType() {
        return serviceType;
    }


    public void executeRuntime(StartupContext startupContext) {
        for (BytecodeInstruction instructionSet : methodRecorder.storedMethodCalls) {
            if(instructionSet instanceof StoredMethodCall) {
                StoredMethodCall m = (StoredMethodCall) instructionSet;
                Object[] params = new Object[m.method.getParameterTypes().length];
                for (int i = 0; i < params.length; ++i) {
                    Class<?> type = m.method.getParameterTypes()[i];
                    String contextName = findContextName(m.method.getParameterAnnotations()[i]);
                    if (type == StartupContext.class) {
                        params[i] = startupContext;
                    } else if (contextName != null) {
                        params[i] = startupContext.getValue(contextName);
                    } else {
                        params[i] = m.parameters[i];
                    }
                }
                try {
                    Object instance = m.method.getDeclaringClass().newInstance();
                    Object result = m.method.invoke(instance, params);
                    ContextObject co = m.method.getAnnotation(ContextObject.class);
                    if (co != null) {
                        startupContext.putValue(co.value(), result);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if(instructionSet instanceof NewInstance) {
                throw new RuntimeException("NYI");
            } else {
                throw new RuntimeException("unknown instruction " + instructionSet);
            }
        }
    }

    private String findContextName(Annotation[] annotations) {
        for (Annotation a : annotations) {
            if (a.annotationType() == ContextObject.class) {
                return ((ContextObject) a).value();
            }
        }
        return null;
    }

    public InjectionInstance<?> newInstanceFactory(String className) {
        String injectionFactory = Injection.getInstanceFactoryName(className, classOutput);
        NewInstance instance = new NewInstance(injectionFactory);
        methodRecorder.storedMethodCalls.add(instance);
        return instance;
    }

    public <T> T getRecordingProxy(Class<T> theClass) {
        return methodRecorder.getRecordingProxy(theClass);
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
            if(type.isAssignableFrom(NewInstance.class)) {
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
        if (runtime) {
            //runtime we don't do this stuff
            return;
        }
        ClassFile file = new ClassFile(className, AccessFlag.PUBLIC, Object.class.getName(), getClass().getClassLoader(), serviceType.getName());
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
        for (BytecodeInstruction set : this.methodRecorder.storedMethodCalls) {
            if(set instanceof StoredMethodCall) {
                StoredMethodCall call = (StoredMethodCall) set;
                if (classInstanceVariables.containsKey(call.theClass)) {
                    continue;
                }
                ca.newInstruction(call.theClass);
                ca.dup();
                ca.invokespecial(call.theClass.getName(), "<init>", "()V");
                ca.astore(localVarCounter);
                classInstanceVariables.put(call.theClass, localVarCounter++);
            } else if(set instanceof NewInstance) {
                ((NewInstance) set).varPos = localVarCounter++;
            }
        }
        //now we invoke the actual method call
        for (BytecodeInstruction set : methodRecorder.storedMethodCalls) {
            if(set instanceof StoredMethodCall) {
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
                            ca.ldc((String) param);
                        } else if (param instanceof Boolean) {
                            ca.ldc((boolean) param ? 1 : 0);
                        } else if(param instanceof NewInstance) {
                            ca.aload(((NewInstance) param).varPos);
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
            } else if(set instanceof NewInstance) {
                NewInstance ni = (NewInstance) set;
                ca.newInstruction(ni.className);
                ca.dup();
                ca.invokespecial(ni.className, "<init>", "()V");
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

    interface BytecodeInstruction {

    }

    static final class StoredMethodCall implements BytecodeInstruction {
        final Class<?> theClass;
        final Method method;
        final Object[] parameters;

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
