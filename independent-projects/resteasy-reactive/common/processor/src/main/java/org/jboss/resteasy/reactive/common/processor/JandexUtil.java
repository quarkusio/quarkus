package org.jboss.resteasy.reactive.common.processor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.jandex.TypeVariable;

/**
 * A collection of Jandex utility methods.
 */
public final class JandexUtil {

    public final static DotName DOTNAME_OBJECT = DotName.createSimple(Object.class.getName());
    public final static DotName DOTNAME_RECORD = DotName.createSimple("java.lang.Record");

    private JandexUtil() {
    }

    public static Index createIndex(Path path) {
        Indexer indexer = new Indexer();
        try (Stream<Path> files = Files.walk(path)) {
            files.forEach(new Consumer<Path>() {
                @Override
                public void accept(Path path) {
                    if (path.toString().endsWith(".class")) {
                        try (InputStream in = Files.newInputStream(path)) {
                            indexer.index(in);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return indexer.complete();
    }

    public static IndexView createCalculatingIndex(Path path) {
        Index index = createIndex(path);
        return new CalculatingIndexView(index, Thread.currentThread().getContextClassLoader(), new ConcurrentHashMap<>());
    }

    public static IndexView createCalculatingIndex(IndexView index) {
        return new CalculatingIndexView(index, Thread.currentThread().getContextClassLoader(), new ConcurrentHashMap<>());
    }

    /**
     * Returns the captured generic types of an interface given a class that at some point in the class
     * hierarchy implements the interface.
     *
     * The list contains the types in the same order as they are generic parameters defined on the interface
     *
     * A result is only returned if and only if all the generics where captured. If any of them where not defined by the class
     * an exception is thrown.
     *
     * Also note that all parts of the class/interface hierarchy must be in the supplied index
     *
     * As an example, imagine the following class:
     *
     * <pre>
     *
     * class MyList implements List&lt;String&gt; {
     *     ...
     * }
     *
     * </pre>
     *
     * If we call
     *
     * <pre>
     *
     * JandexUtil.resolveTypeParameters(DotName.createSimple(MyList.class.getName()),
     *         DotName.createSimple(List.class.getName()), index)
     *
     * </pre>
     *
     * then the result will contain a single element of class ClassType whose name() would return a DotName for String
     */
    public static List<Type> resolveTypeParameters(DotName input, DotName target, IndexView index) {
        final ClassInfo inputClassInfo = fetchFromIndex(input, index);

        Type startingType = getType(inputClassInfo, index);
        final List<Type> result = findParametersRecursively(startingType, target,
                new HashSet<>(), index);
        // null means not found
        if (result == null) {
            return Collections.emptyList();
        }

        return result;
    }

    /**
     * Creates a type for a ClassInfo
     */
    private static Type getType(ClassInfo inputClassInfo, IndexView index) {
        List<TypeVariable> typeParameters = inputClassInfo.typeParameters();
        if (typeParameters.isEmpty())
            return ClassType.create(inputClassInfo.name(), Kind.CLASS);
        Type owner = null;
        // ignore owners for non-static classes
        if (inputClassInfo.enclosingClass() != null && !Modifier.isStatic(inputClassInfo.flags())) {
            owner = getType(fetchFromIndex(inputClassInfo.enclosingClass(), index), index);
        }
        return ParameterizedType.create(inputClassInfo.name(), typeParameters.toArray(new Type[0]), owner);
    }

    /**
     * Finds the type arguments passed from the starting type to the given target type, mapping
     * generics when found, on the way down. Returns null if not found.
     */
    private static List<Type> findParametersRecursively(Type type, DotName target,
            Set<DotName> visitedTypes, IndexView index) {
        DotName name = type.name();
        // cache results first
        if (!visitedTypes.add(name)) {
            return null;
        }

        // always end at Object
        if (DOTNAME_OBJECT.equals(name) || DOTNAME_RECORD.equals(name)) {
            return null;
        }

        final ClassInfo inputClassInfo = fetchFromIndex(name, index);

        // look at the current type
        if (target.equals(name)) {
            Type thisType = getType(inputClassInfo, index);
            if (thisType.kind() == Kind.CLASS)
                return Collections.emptyList();
            else
                return thisType.asParameterizedType().arguments();
        }

        // superclasses first
        Type superClassType = inputClassInfo.superClassType();
        List<Type> superResult = findParametersRecursively(superClassType, target, visitedTypes, index);
        if (superResult != null) {
            // map any returned type parameters to our type arguments on the way down
            return mapTypeArguments(superClassType, superResult, index);
        }

        // interfaces second
        for (Type interfaceType : inputClassInfo.interfaceTypes()) {
            List<Type> ret = findParametersRecursively(interfaceType, target, visitedTypes, index);
            if (ret != null) {
                // map any returned type parameters to our type arguments on the way down
                return mapTypeArguments(interfaceType, ret, index);
            }
        }

        // not found
        return null;
    }

    /**
     * Maps any type parameters in typeArgumentsFromSupertype from the type parameters declared in appliedType's declaration
     * to the type arguments we passed in appliedType
     */
    private static List<Type> mapTypeArguments(Type appliedType, List<Type> typeArgumentsFromSupertype, IndexView index) {
        // no type arguments to map
        if (typeArgumentsFromSupertype.isEmpty()) {
            return typeArgumentsFromSupertype;
        }
        // extra easy if all the type args don't contain any type parameters
        if (!containsTypeParameters(typeArgumentsFromSupertype)) {
            return typeArgumentsFromSupertype;
        }

        // this can't fail since we got a result
        ClassInfo superType = fetchFromIndex(appliedType.name(), index);

        // if our supertype has no type parameters, we don't need any mapping
        if (superType.typeParameters().isEmpty()) {
            return typeArgumentsFromSupertype;
        }

        // figure out which arguments we passed to the supertype
        List<Type> appliedArguments;

        // we passed them explicitely
        if (appliedType.kind() == Kind.PARAMETERIZED_TYPE) {
            appliedArguments = appliedType.asParameterizedType().arguments();
        } else {
            // raw supertype: use bounds
            appliedArguments = new ArrayList<>(superType.typeParameters().size());
            for (TypeVariable typeVariable : superType.typeParameters()) {
                if (!typeVariable.bounds().isEmpty()) {
                    appliedArguments.add(typeVariable.bounds().get(0));
                } else {
                    appliedArguments.add(ClassType.create(DOTNAME_OBJECT, Kind.CLASS));
                }
            }
        }

        // it's a problem if we got different arguments to the parameters declared
        if (appliedArguments.size() != superType.typeParameters().size()) {
            throw new IllegalArgumentException("Our supertype instance " + appliedType
                    + " does not match supertype declared arguments: " + superType.typeParameters());
        }
        // build the mapping
        Map<String, Type> mapping = new HashMap<>();
        for (int i = 0; i < superType.typeParameters().size(); i++) {
            TypeVariable typeParameter = superType.typeParameters().get(i);
            mapping.put(typeParameter.identifier(), appliedArguments.get(i));
        }
        // and map
        return mapGenerics(typeArgumentsFromSupertype, mapping);
    }

    private static boolean containsTypeParameters(List<Type> typeArgumentsFromSupertype) {
        for (Type type : typeArgumentsFromSupertype) {
            if (containsTypeParameters(type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsTypeParameters(Type type) {
        switch (type.kind()) {
            case ARRAY:
                return containsTypeParameters(type.asArrayType().constituent());
            case PARAMETERIZED_TYPE:
                ParameterizedType parameterizedType = type.asParameterizedType();
                if (parameterizedType.owner() != null
                        && containsTypeParameters(parameterizedType.owner()))
                    return true;
                return containsTypeParameters(parameterizedType.arguments());
            case TYPE_VARIABLE:
                return true;
            default:
                return false;
        }
    }

    private static List<Type> mapGenerics(List<Type> types, Map<String, Type> mapping) {
        List<Type> ret = new ArrayList<>(types.size());
        for (Type type : types) {
            ret.add(mapGenerics(type, mapping));
        }
        return ret;
    }

    private static Type mapGenerics(Type type, Map<String, Type> mapping) {
        switch (type.kind()) {
            case ARRAY:
                ArrayType arrayType = type.asArrayType();
                return ArrayType.create(mapGenerics(arrayType.constituent(), mapping), arrayType.dimensions());
            case CLASS:
                return type;
            case PARAMETERIZED_TYPE:
                ParameterizedType parameterizedType = type.asParameterizedType();
                Type owner = null;
                if (parameterizedType.owner() != null) {
                    owner = mapGenerics(parameterizedType.owner(), mapping);
                }
                return ParameterizedType.create(parameterizedType.name(),
                        mapGenerics(parameterizedType.arguments(), mapping).toArray(new Type[0]), owner);
            case TYPE_VARIABLE:
                Type ret = mapping.get(type.asTypeVariable().identifier());
                if (ret == null) {
                    throw new IllegalArgumentException("Missing type argument mapping for " + type);
                }
                return ret;
            default:
                throw new IllegalArgumentException("Illegal type in hierarchy: " + type);
        }
    }

    private static ClassInfo fetchFromIndex(DotName dotName, IndexView index) {
        final ClassInfo classInfo = index.getClassByName(dotName);
        if (classInfo == null) {
            throw new IllegalArgumentException("Class " + dotName + " was not found in the index");
        }
        return classInfo;
    }

    /**
     * Returns the enclosing class of the given annotation instance. For field, method or record component annotations,
     * this will return the enclosing class. For parameters, this will return the enclosing class of the enclosing
     * method. For classes, it will return the class itself. For type annotations, it will return the class enclosing
     * the annotated type usage.
     *
     * @param annotationInstance the annotation whose enclosing class to look up
     * @return the enclosing class
     */
    public static ClassInfo getEnclosingClass(AnnotationInstance annotationInstance) {
        return getEnclosingClass(annotationInstance.target());
    }

    private static ClassInfo getEnclosingClass(AnnotationTarget annotationTarget) {
        switch (annotationTarget.kind()) {
            case FIELD:
                return annotationTarget.asField().declaringClass();
            case METHOD:
                return annotationTarget.asMethod().declaringClass();
            case METHOD_PARAMETER:
                return annotationTarget.asMethodParameter().method().declaringClass();
            case RECORD_COMPONENT:
                return annotationTarget.asRecordComponent().declaringClass();
            case CLASS:
                return annotationTarget.asClass();
            case TYPE:
                return getEnclosingClass(annotationTarget.asType().enclosingTarget());
            default:
                throw new RuntimeException(); // this should not occur
        }
    }

    /**
     * Returns true if the given Jandex ClassInfo is a subclass of the given <tt>parentName</tt>. Note that this will
     * not check interfaces.
     *
     * @param index the index to use to look up super classes.
     * @param info the ClassInfo we want to check.
     * @param parentName the name of the superclass we want to find.
     * @return true if the given ClassInfo has <tt>parentName</tt> as a superclass.
     * @throws RuntimeException if one of the superclasses is not indexed.
     */
    public static boolean isSubclassOf(IndexView index, ClassInfo info, DotName parentName) {
        if (info.superName().equals(DOTNAME_OBJECT) || info.superName().equals(DOTNAME_RECORD)) {
            return false;
        }
        if (info.superName().equals(parentName)) {
            return true;
        }

        // climb up the hierarchy of classes
        Type superType = info.superClassType();
        ClassInfo superClass = index.getClassByName(superType.name());
        if (superClass == null) {
            // this can happens if the parent is not inside the Jandex index
            throw new RuntimeException("The class " + superType.name() + " is not inside the Jandex index");
        }
        return isSubclassOf(index, superClass, parentName);
    }

    /**
     * Returns true if the given Jandex ClassInfo is a subclass of or inherits the given <tt>name</tt>.
     *
     * @param index the index to use to look up super classes.
     * @param info the ClassInfo we want to check.
     * @param name the name of the superclass or interface we want to find.
     * @throws RuntimeException if one of the superclasses is not indexed.
     */
    public static boolean isImplementorOf(IndexView index, ClassInfo info, DotName name) {
        return isImplementorOf(index, info, name, Collections.emptySet());
    }

    /**
     * Returns true if the given Jandex ClassInfo is a subclass of or inherits the given <tt>name</tt>.
     *
     * @param index the index to use to look up super classes.
     * @param info the ClassInfo we want to check.
     * @param name the name of the superclass or interface we want to find.
     * @param additionalIgnoredSuperClasses return false if the class has any of these as a superclass.
     * @throws RuntimeException if one of the superclasses is not indexed.
     */
    public static boolean isImplementorOf(IndexView index, ClassInfo info, DotName name,
            Set<DotName> additionalIgnoredSuperClasses) {
        // Check interfaces
        List<DotName> interfaceNames = info.interfaceNames();
        for (DotName interfaceName : interfaceNames) {
            if (interfaceName.equals(name)) {
                return true;
            }
        }

        // Check direct hierarchy
        DotName superDotName = info.superName();
        if (superDotName.equals(DOTNAME_OBJECT) || superDotName.equals(DOTNAME_RECORD)
                || additionalIgnoredSuperClasses.contains(superDotName)) {
            return false;
        }
        if (info.superName().equals(name)) {
            return true;
        }

        // climb up the hierarchy of classes
        Type superType = info.superClassType();
        ClassInfo superClass = index.getClassByName(superType.name());
        if (superClass == null) {
            // this can happen if the parent is not inside the Jandex index
            throw new RuntimeException("The class " + superType.name() + " is not inside the Jandex index");
        }
        return isImplementorOf(index, superClass, name, additionalIgnoredSuperClasses);
    }

    public static boolean isAssignableFrom(DotName superType, DotName subType, IndexView index) {
        // java.lang.Object is assignable from any type
        if (superType.equals(DOTNAME_OBJECT)) {
            return true;
        }
        // type1 is the same as type2
        if (superType.equals(subType)) {
            return true;
        }
        // type1 is a superclass
        return findSupertypes(subType, index).contains(superType);
    }

    private static Set<DotName> findSupertypes(DotName name, IndexView index) {
        Set<DotName> result = new HashSet<>();

        Deque<DotName> workQueue = new ArrayDeque<>();
        workQueue.add(name);
        while (!workQueue.isEmpty()) {
            DotName type = workQueue.poll();
            if (result.contains(type)) {
                continue;
            }
            result.add(type);

            ClassInfo clazz = index.getClassByName(type);
            if (clazz == null) {
                continue;
            }
            if (clazz.superName() != null) {
                workQueue.add(clazz.superName());
            }
            workQueue.addAll(clazz.interfaceNames());
        }

        return result;
    }

}
