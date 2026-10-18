package io.quarkus.amazon.lambda.deployment;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;

import com.amazonaws.services.lambda.runtime.RequestHandler;

public class RequestHandlerJandexUtil {

    private static final DotName REQUEST_HANDLER = DotName.createSimple(RequestHandler.class);
    private static final DotName OBJECT = DotName.createSimple("java.lang.Object");
    private static final DotName COLLECTION = DotName.createSimple(Collection.class);

    private RequestHandlerJandexUtil() {
    }

    public static RequestHandlerJandexDefinition discoverHandlerMethod(String handlerClassName, IndexView index) {
        ClassInfo handlerClass = index.getClassByName(handlerClassName);
        if (handlerClass == null) {
            throw new IllegalArgumentException("RequestHandler class not found in the index: " + handlerClassName);
        }

        MethodInfo concreteHandleRequestMethod = findConcreteHandleRequestMethod(handlerClass, index);
        if (concreteHandleRequestMethod == null) {
            throw new IllegalStateException(
                    "Unable to find a concrete handleRequest method on handler class " + handlerClass.name());
        }

        Map<String, Type> typeMap = new HashMap<>();
        InputOutputTypes inputOutputTypes = resolveInputOutputTypes(handlerClass.name(), index, typeMap);
        if (isUnresolved(inputOutputTypes)) {
            throw new IllegalStateException(
                    "Unable to resolve input and output types for handler class " + handlerClass.name());
        }

        return new RequestHandlerJandexDefinition(handlerClass, concreteHandleRequestMethod, inputOutputTypes);
    }

    private static MethodInfo findConcreteHandleRequestMethod(ClassInfo handlerClass, IndexView index) {
        ClassInfo currentClass = handlerClass;

        // Look for implementations of the method in the class hierarchy
        while (currentClass != null && !OBJECT.equals(currentClass.name())) {
            for (MethodInfo method : currentClass.methods()) {
                if (isHandleRequestMethod(method) && !method.isSynthetic() && !method.isAbstract()) {
                    return method;
                }
            }

            Type superType = currentClass.superClassType();
            if (superType != null) {
                currentClass = index.getClassByName(superType.name());
            } else {
                currentClass = null;
            }
        }

        // If not found, look for default methods in interfaces
        currentClass = handlerClass;
        while (currentClass != null && !OBJECT.equals(currentClass.name())) {
            for (Type ifaceType : currentClass.interfaceTypes()) {
                MethodInfo defaultMethod = findDefaultInterfaceMethod(ifaceType.name(), index);
                if (defaultMethod != null) {
                    return defaultMethod;
                }
            }

            // Move to superclass
            Type superType = currentClass.superClassType();
            if (superType != null) {
                currentClass = index.getClassByName(superType.name());
            } else {
                currentClass = null;
            }
        }

        return null;
    }

    private static MethodInfo findDefaultInterfaceMethod(DotName ifaceName, IndexView index) {
        ClassInfo iface = index.getClassByName(ifaceName);
        if (iface == null) {
            return null;
        }

        for (MethodInfo method : iface.methods()) {
            if (isHandleRequestMethod(method) && method.isDefault()) {
                return method;
            }
        }

        for (Type parentIfaceType : iface.interfaceTypes()) {
            MethodInfo defaultMethod = findDefaultInterfaceMethod(parentIfaceType.name(), index);
            if (defaultMethod != null) {
                return defaultMethod;
            }
        }

        return null;
    }

    private static boolean isHandleRequestMethod(MethodInfo method) {
        return method.name().equals("handleRequest") && method.parametersCount() == 2;
    }

    private static boolean isCollectionType(DotName typeName, IndexView index) {
        if (COLLECTION.equals(typeName)) {
            return true;
        }

        ClassInfo classInfo = index.getClassByName(typeName);
        if (classInfo == null) {
            return false;
        }

        for (Type interfaceType : classInfo.interfaceTypes()) {
            if (isCollectionType(interfaceType.name(), index)) {
                return true;
            }
        }

        Type superType = classInfo.superClassType();
        if (superType != null) {
            if (isCollectionType(superType.name(), index)) {
                return true;
            }
        }

        return false;
    }

    public record InputOutputTypes(Type inputType, boolean isCollection, Type elementType, Type outputType) {
        private static final InputOutputTypes UNRESOLVED = new InputOutputTypes(
                Type.create(OBJECT, Type.Kind.CLASS), false, null,
                Type.create(OBJECT, Type.Kind.CLASS));
    }

    public record RequestHandlerJandexDefinition(ClassInfo handlerClass, MethodInfo method, InputOutputTypes inputOutputTypes) {
    }

    private static boolean isUnresolved(InputOutputTypes types) {
        return types == null || types == InputOutputTypes.UNRESOLVED;
    }

    private static InputOutputTypes resolveInputOutputTypes(DotName className, IndexView index,
            Map<String, Type> typeMap) {
        ClassInfo classInfo = index.getClassByName(className);
        if (classInfo == null) {
            return null;
        }

        for (Type interfaceType : classInfo.interfaceTypes()) {
            InputOutputTypes result = resolveInputOutputTypesFromType(interfaceType, index, typeMap);
            if (result != null) {
                return result;
            }
        }

        Type superType = classInfo.superClassType();
        if (superType != null) {
            InputOutputTypes result = resolveInputOutputTypesFromType(superType, index, typeMap);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private static InputOutputTypes resolveInputOutputTypesFromType(Type currentType, IndexView index,
            Map<String, Type> typeMap) {
        if (currentType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType pt = currentType.asParameterizedType();
            DotName rawName = pt.name();

            // If we hit RequestHandler, resolve the type arguments
            if (REQUEST_HANDLER.equals(rawName)) {
                List<Type> args = pt.arguments();
                Type inputType = resolveTypeArgument(args.get(0), typeMap);
                Type outputType = resolveTypeArgument(args.get(1), typeMap);

                // Check if input type is a collection and extract element type
                boolean isCollection = false;
                Type elementType = null;
                Type rawInputType = args.get(0);

                if (rawInputType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                    ParameterizedType inputPt = rawInputType.asParameterizedType();
                    if (isCollectionType(inputPt.name(), index)) {
                        isCollection = true;
                        if (!inputPt.arguments().isEmpty()) {
                            elementType = resolveTypeArgument(inputPt.arguments().get(0), typeMap);
                        }
                    }
                } else if (rawInputType.kind() == Type.Kind.CLASS) {
                    if (isCollectionType(rawInputType.name(), index)) {
                        isCollection = true;
                        elementType = Type.create(OBJECT, Type.Kind.CLASS);
                    }
                }

                return new InputOutputTypes(inputType, isCollection, elementType, outputType);
            }

            // Record bindings for current type variables
            ClassInfo rawClass = index.getClassByName(rawName);
            if (rawClass != null) {
                List<TypeVariable> vars = rawClass.typeParameters();
                List<Type> args = pt.arguments();
                Map<String, Type> newTypeMap = new HashMap<>(typeMap);
                for (int i = 0; i < vars.size() && i < args.size(); i++) {
                    newTypeMap.put(vars.get(i).identifier(), args.get(i));
                }

                // Recursively check this type's hierarchy
                InputOutputTypes result = resolveInputOutputTypes(rawName, index, newTypeMap);
                if (result != null) {
                    return result;
                }
            }
        } else if (currentType.kind() == Type.Kind.CLASS) {
            DotName className = currentType.name();

            if (REQUEST_HANDLER.equals(className)) {
                return InputOutputTypes.UNRESOLVED;
            }

            return resolveInputOutputTypes(className, index, typeMap);
        }

        return null;
    }

    private static Type resolveTypeArgument(Type type, Map<String, Type> typeMap) {
        if (type.kind() == Type.Kind.TYPE_VARIABLE) {
            TypeVariable tv = type.asTypeVariable();
            Type resolved = typeMap.get(tv.identifier());
            if (resolved != null) {
                // Recursively resolve in case the resolved type is also a type variable
                return resolveTypeArgument(resolved, typeMap);
            }
            // If we can't resolve the type variable, get its first bound
            return getTypeFromBounds(tv);
        }
        return type;
    }

    private static Type getTypeFromBounds(TypeVariable tv) {
        List<Type> bounds = tv.bounds();
        if (!bounds.isEmpty()) {
            Type firstBound = bounds.get(0);
            if (firstBound.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                return Type.create(firstBound.name(), Type.Kind.CLASS);
            }
            return firstBound;
        }
        return Type.create(OBJECT, Type.Kind.CLASS);
    }
}
