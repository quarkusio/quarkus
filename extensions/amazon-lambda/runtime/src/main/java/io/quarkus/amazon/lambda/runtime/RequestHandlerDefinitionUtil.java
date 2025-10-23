package io.quarkus.amazon.lambda.runtime;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.RequestHandler;

public class RequestHandlerDefinitionUtil {

    private RequestHandlerDefinitionUtil() {
    }

    static RequestHandlerDefinition discoverHandlerMethod(Class<? extends RequestHandler<?, ?>> handlerClass) {
        Method concreteHandleRequestMethod = findConcreteHandleRequestMethod(handlerClass);
        if (concreteHandleRequestMethod == null) {
            throw new IllegalStateException(
                    "Unable to find a concrete handleRequest method on handler class " + handlerClass);
        }

        Map<TypeVariable<?>, Type> typeMap = new HashMap<>();
        InputOutputTypes inputOutputTypes = resolveInputOutputTypes(handlerClass, typeMap);
        if (isUnresolved(inputOutputTypes)) {
            throw new IllegalStateException(
                    "Unable to resolve input and output types for handler class " + handlerClass);
        }

        return new RequestHandlerDefinition(concreteHandleRequestMethod, inputOutputTypes);
    }

    private static Method findConcreteHandleRequestMethod(Class<? extends RequestHandler<?, ?>> handlerClass) {
        Class<?> currentClass = handlerClass;
        while (currentClass != null) {
            Method[] declaredMethods = currentClass.getDeclaredMethods();
            for (Method method : declaredMethods) {
                if (isHandleRequestMethod(method) && !Modifier.isAbstract(method.getModifiers()) && !method.isSynthetic()) {
                    return method;
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        // Second pass: only if no concrete method found, check interfaces for default methods
        currentClass = handlerClass;
        while (currentClass != null) {
            for (Class<?> iface : currentClass.getInterfaces()) {
                Method defaultMethod = findDefaultInterfaceMethod(iface);
                if (defaultMethod != null) {
                    return defaultMethod;
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        return null;
    }

    private static Method findDefaultInterfaceMethod(Class<?> iface) {
        // Check this interface for default methods
        Method[] methods = iface.getMethods();
        for (Method method : methods) {
            if (isHandleRequestMethod(method) && method.isDefault()) {
                return method;
            }
        }

        // Check parent interfaces recursively
        for (Class<?> parentIface : iface.getInterfaces()) {
            Method defaultMethod = findDefaultInterfaceMethod(parentIface);
            if (defaultMethod != null) {
                return defaultMethod;
            }
        }

        return null;
    }

    private static boolean isHandleRequestMethod(Method method) {
        return method.getName().equals("handleRequest") && method.getParameterCount() == 2;
    }

    public record InputOutputTypes(Class<?> inputType, Class<?> outputType) {
        private static final InputOutputTypes UNRESOLVED = new InputOutputTypes(Object.class, Object.class);
    }

    public record RequestHandlerDefinition(Method method, InputOutputTypes inputOutputTypes) {
    }

    private static boolean isUnresolved(InputOutputTypes types) {
        // We consider types unresolved if we can't determine them from the generic information
        // This happens when we have a raw RequestHandler interface
        return types == null || types == InputOutputTypes.UNRESOLVED;
    }

    private static InputOutputTypes resolveInputOutputTypes(Type currentType, Map<TypeVariable<?>, Type> typeMap) {
        if (currentType instanceof ParameterizedType pt) {
            Class<?> raw = (Class<?>) pt.getRawType();

            // If we hit RequestHandler, resolve the type arguments
            if (RequestHandler.class.equals(raw)) {
                Type[] args = pt.getActualTypeArguments();
                Class<?> inputType = resolveTypeArgument(args[0], typeMap);
                Class<?> outputType = resolveTypeArgument(args[1], typeMap);
                return new InputOutputTypes(inputType, outputType);
            }

            // Record bindings for current type variables
            TypeVariable<?>[] vars = raw.getTypeParameters();
            Type[] args = pt.getActualTypeArguments();
            Map<TypeVariable<?>, Type> newTypeMap = new HashMap<>(typeMap);
            for (int i = 0; i < vars.length; i++) {
                newTypeMap.put(vars[i], args[i]);
            }

            Type superclass = raw.getGenericSuperclass();
            if (superclass != null) {
                InputOutputTypes result = resolveInputOutputTypes(superclass, newTypeMap);
                if (result != null) {
                    return result;
                }
            }

            Type[] interfaces = raw.getGenericInterfaces();
            for (Type iface : interfaces) {
                InputOutputTypes result = resolveInputOutputTypes(iface, newTypeMap);
                if (result != null) {
                    return result;
                }
            }
        } else if (currentType instanceof Class<?> c) {
            // Raw RequestHandler interface
            if (RequestHandler.class.equals(c)) {
                return InputOutputTypes.UNRESOLVED;
            }

            Type superclass = c.getGenericSuperclass();
            if (superclass != null) {
                InputOutputTypes result = resolveInputOutputTypes(superclass, typeMap);
                if (result != null) {
                    return result;
                }
            }

            Type[] interfaces = c.getGenericInterfaces();
            for (Type iface : interfaces) {
                InputOutputTypes result = resolveInputOutputTypes(iface, typeMap);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private static Class<?> resolveTypeArgument(Type type, Map<TypeVariable<?>, Type> typeMap) {
        if (type instanceof TypeVariable<?> tv) {
            Type resolved = typeMap.get(tv);
            if (resolved != null) {
                // Recursively resolve in case the resolved type is also a type variable
                return resolveTypeArgument(resolved, typeMap);
            }
            // If we can't resolve the type variable, get its first bound
            return getClassFromType(tv);
        }
        return getClassFromType(type);
    }

    private static Class<?> getClassFromType(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return getClassFromType(((ParameterizedType) type).getRawType());
        } else if (type instanceof TypeVariable) {
            // let's try to get the first bound
            Type[] bounds = ((TypeVariable<?>) type).getBounds();
            if (bounds.length > 0) {
                return getClassFromType(bounds[0]);
            }
        }

        throw new IllegalArgumentException("Unsupported type: " + type);
    }
}