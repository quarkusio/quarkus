package io.quarkus.spring.data.deployment.generate;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.NoResultException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.repository.Modifying;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.spring.data.deployment.DotNames;
import io.quarkus.spring.data.runtime.RepositorySupport;
import io.quarkus.spring.data.runtime.TypesConverter;

public abstract class AbstractMethodsAdder {

    protected void handleLongReturnValue(BytecodeCreator methodCreator, ResultHandle resultHandle, DotName returnType) {
        if (DotNames.LONG.equals(returnType)) { // handle object Long return type
            resultHandle = methodCreator.invokeStaticMethod(
                    MethodDescriptor.ofMethod(Long.class, "valueOf", Long.class, long.class),
                    resultHandle);
        }
        methodCreator.returnValue(resultHandle);
    }

    protected void handleIntegerReturnValue(BytecodeCreator methodCreator, ResultHandle resultHandle, DotName returnType) {
        if (DotNames.INTEGER.equals(returnType)) { // handle object Integer return type
            resultHandle = methodCreator.invokeStaticMethod(
                    MethodDescriptor.ofMethod(Integer.class, "valueOf", Integer.class, int.class),
                    resultHandle);
        }
        methodCreator.returnValue(resultHandle);
    }

    protected void handleBooleanReturnValue(BytecodeCreator methodCreator, ResultHandle resultHandle, DotName returnType) {
        if (DotNames.BOOLEAN.equals(returnType)) { // handle object Long return type
            resultHandle = methodCreator.invokeStaticMethod(
                    MethodDescriptor.ofMethod(Boolean.class, "valueOf", Boolean.class, boolean.class),
                    resultHandle);
        }
        methodCreator.returnValue(resultHandle);
    }

    protected void generateFindQueryResultHandling(MethodCreator methodCreator, ResultHandle panacheQuery,
            Integer pageableParameterIndex, ClassInfo repositoryClassInfo, ClassInfo entityClassInfo,
            DotName returnType, Integer limit, String methodName, DotName customResultType, String originalResultType) {

        ResultHandle page = null;
        if (limit != null) {
            // create a custom page object that will limit the results by the limit size
            page = methodCreator.newInstance(MethodDescriptor.ofConstructor(Page.class, int.class), methodCreator.load(limit));
        } else if (pageableParameterIndex != null) {
            page = methodCreator.invokeStaticMethod(
                    MethodDescriptor.ofMethod(TypesConverter.class, "toPanachePage", Page.class, Pageable.class),
                    methodCreator.getMethodParam(pageableParameterIndex));
        }

        if (page != null) {
            panacheQuery = methodCreator.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(PanacheQuery.class, "page", PanacheQuery.class, Page.class),
                    panacheQuery, page);
        }

        if (returnType.equals(entityClassInfo.name())) {
            // implement by issuing PanacheQuery.singleResult
            // if there is one result return
            // if there are no results (known due to NoResultException) return null
            // if there are multiple results just let the relevant exception be thrown

            // when limit is specified we don't want to fail when there are multiple results, we just want to return the first one
            String panacheQueryMethodToUse = (limit != null) ? "firstResult" : "singleResult";

            TryBlock tryBlock = methodCreator.tryBlock();
            ResultHandle singleResult = tryBlock.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(PanacheQuery.class, panacheQueryMethodToUse, Object.class),
                    panacheQuery);

            ResultHandle casted = tryBlock.checkCast(singleResult, entityClassInfo.name().toString());
            tryBlock.returnValue(casted);

            CatchBlockCreator catchBlock = tryBlock.addCatch(NoResultException.class);
            catchBlock.returnValue(catchBlock.loadNull());

        } else if (DotNames.OPTIONAL.equals(returnType)) {
            // implement by issuing PanacheQuery.singleResult
            // if there is one result return an Optional containing it
            // if there are no results (known due to NoResultException) return empty Optional
            // if there are multiple results just let the relevant exception be thrown

            // when limit is specified we don't want to fail when there are multiple results, we just want to return the first one
            String panacheQueryMethodToUse = (limit != null) ? "firstResult" : "singleResult";

            TryBlock tryBlock = methodCreator.tryBlock();
            ResultHandle singleResult = tryBlock.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(PanacheQuery.class, panacheQueryMethodToUse, Object.class),
                    panacheQuery);

            if (customResultType == null) {
                ResultHandle casted = tryBlock.checkCast(singleResult, entityClassInfo.name().toString());
                ResultHandle optional = tryBlock.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Optional.class, "of", Optional.class, Object.class),
                        casted);
                tryBlock.returnValue(optional);
            } else {
                ResultHandle customResult = tryBlock.invokeStaticMethod(
                        MethodDescriptor.ofMethod(customResultType.toString(), "convert_" + methodName,
                                customResultType.toString(),
                                originalResultType),
                        singleResult);
                ResultHandle optional = tryBlock.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Optional.class, "of", Optional.class, Object.class),
                        customResult);
                tryBlock.returnValue(optional);
            }
            CatchBlockCreator catchBlock = tryBlock.addCatch(NoResultException.class);
            ResultHandle emptyOptional = catchBlock.invokeStaticMethod(
                    MethodDescriptor.ofMethod(Optional.class, "empty", Optional.class));
            catchBlock.returnValue(emptyOptional);
        } else if (DotNames.LIST.equals(returnType) || DotNames.COLLECTION.equals(returnType)
                || DotNames.SET.equals(returnType) || DotNames.ITERATOR.equals(returnType)
                || DotNames.SPRING_DATA_PAGE.equals(returnType) || DotNames.SPRING_DATA_SLICE.equals(returnType)) {
            ResultHandle list;

            if (customResultType == null) {
                list = methodCreator.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(PanacheQuery.class, "list", List.class),
                        panacheQuery);
            } else {

                ResultHandle stream = methodCreator.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(PanacheQuery.class, "stream", Stream.class),
                        panacheQuery);

                // Function to convert `originResultType` (Object[] or entity class)
                // to the custom type (using the generated static convert method)
                FunctionCreator customResultMappingFunction = methodCreator.createFunction(Function.class);
                BytecodeCreator funcBytecode = customResultMappingFunction.getBytecode();
                ResultHandle obj = funcBytecode.invokeStaticMethod(
                        MethodDescriptor.ofMethod(customResultType.toString(), "convert_" + methodName,
                                customResultType.toString(),
                                originalResultType),
                        funcBytecode.getMethodParam(0));
                funcBytecode.returnValue(obj);

                stream = methodCreator.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Stream.class, "map", Stream.class, Function.class),
                        stream, customResultMappingFunction.getInstance());

                // Re-collect the stream into a list
                ResultHandle collector = methodCreator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Collectors.class, "toList", Collector.class));
                list = methodCreator.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Stream.class, "collect", Object.class, Collector.class),
                        stream, collector);
            }

            if (DotNames.ITERATOR.equals(returnType)) {
                ResultHandle iterator = methodCreator.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Iterable.class, "iterator", Iterator.class),
                        list);
                methodCreator.returnValue(iterator);
            } else if (DotNames.SET.equals(returnType)) {
                ResultHandle set = methodCreator.newInstance(
                        MethodDescriptor.ofConstructor(LinkedHashSet.class, Collection.class), list);
                methodCreator.returnValue(set);
            } else if (DotNames.SPRING_DATA_PAGE.equals(returnType)) {
                ResultHandle pageResult;
                if (pageableParameterIndex != null) {
                    ResultHandle count = methodCreator.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(PanacheQuery.class, "count", long.class),
                            panacheQuery);
                    pageResult = methodCreator.newInstance(
                            MethodDescriptor.ofConstructor(PageImpl.class, List.class, Pageable.class, long.class),
                            list, methodCreator.getMethodParam(pageableParameterIndex), count);
                } else {
                    pageResult = methodCreator.newInstance(MethodDescriptor.ofConstructor(PageImpl.class, List.class), list);
                }

                methodCreator.returnValue(pageResult);
            } else if (DotNames.SPRING_DATA_SLICE.equals(returnType)) {
                ResultHandle sliceResult;
                if (pageableParameterIndex != null) {
                    ResultHandle hasNextPage = methodCreator.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(PanacheQuery.class, "hasNextPage", boolean.class),
                            panacheQuery);
                    sliceResult = methodCreator.newInstance(
                            MethodDescriptor.ofConstructor(SliceImpl.class, List.class, Pageable.class, boolean.class),
                            list, methodCreator.getMethodParam(pageableParameterIndex), hasNextPage);
                } else {
                    sliceResult = methodCreator.newInstance(MethodDescriptor.ofConstructor(SliceImpl.class, List.class), list);
                }

                methodCreator.returnValue(sliceResult);
            }
            methodCreator.returnValue(list);

        } else if (DotNames.STREAM.equals(returnType)) {
            ResultHandle stream = methodCreator.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(PanacheQuery.class, "stream", Stream.class),
                    panacheQuery);
            methodCreator.returnValue(stream);

        } else if (isHibernateSupportedReturnType(returnType)) {
            ResultHandle singleResult = methodCreator.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(PanacheQuery.class, "singleResult", Object.class),
                    panacheQuery);
            methodCreator.returnValue(singleResult);
        } else if (customResultType != null) {
            // when limit is specified we don't want to fail when there are multiple results, we just want to return the first one
            String panacheQueryMethodToUse = (limit != null) ? "firstResult" : "singleResult";

            TryBlock tryBlock = methodCreator.tryBlock();
            ResultHandle singleResult = tryBlock.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(PanacheQuery.class, panacheQueryMethodToUse, Object.class),
                    panacheQuery);

            ResultHandle customResult = tryBlock.invokeStaticMethod(
                    MethodDescriptor.ofMethod(customResultType.toString(), "convert_" + methodName,
                            customResultType.toString(),
                            originalResultType),
                    singleResult);

            tryBlock.returnValue(customResult);

            CatchBlockCreator catchBlock = tryBlock.addCatch(NoResultException.class);
            catchBlock.returnValue(catchBlock.loadNull());

            tryBlock.returnValue(customResult);
        } else {
            throw new IllegalArgumentException(
                    "Return type of method " + methodName + " of Repository " + repositoryClassInfo
                            + " does not match find query type");
        }
    }

    /**
     * Flush the underlying persistence context before executing the modifying query if enabled by {@link Modifying}
     * annotation.
     */
    protected void handleFlushAutomatically(AnnotationInstance modifyingAnnotation, MethodCreator methodCreator,
            FieldDescriptor entityClassFieldDescriptor) {
        final AnnotationValue flushAutomatically = modifyingAnnotation != null ? modifyingAnnotation.value("flushAutomatically")
                : null;
        if (flushAutomatically != null && flushAutomatically.asBoolean()) {
            methodCreator.invokeStaticMethod(
                    MethodDescriptor.ofMethod(RepositorySupport.class, "flush", void.class, Class.class),
                    methodCreator.readInstanceField(entityClassFieldDescriptor, methodCreator.getThis()));
        }
    }

    /**
     * Clear the underlying persistence context after executing the modifying query if enabled by {@link Modifying}
     * annotation.
     */
    protected void handleClearAutomatically(AnnotationInstance modifyingAnnotation, MethodCreator methodCreator,
            FieldDescriptor entityClassFieldDescriptor) {
        final AnnotationValue clearAutomatically = modifyingAnnotation != null ? modifyingAnnotation.value("clearAutomatically")
                : null;
        if (clearAutomatically != null && clearAutomatically.asBoolean()) {
            methodCreator.invokeStaticMethod(
                    MethodDescriptor.ofMethod(RepositorySupport.class, "clear", void.class, Class.class),
                    methodCreator.readInstanceField(entityClassFieldDescriptor, methodCreator.getThis()));
        }
    }

    protected boolean isHibernateSupportedReturnType(DotName dotName) {
        return dotName.equals(DotNames.OBJECT) || DotNames.HIBERNATE_PROVIDED_BASIC_TYPES.contains(dotName);
    }

    protected Type verifyQueryResultType(Type t, IndexView index) {
        if (isHibernateSupportedReturnType(t.name())) {
            return t;
        }
        if (t.kind() == Type.Kind.ARRAY) {
            return verifyQueryResultType(t.asArrayType().component(), index);
        } else if (t.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            final List<Type> types = t.asParameterizedType().arguments();
            if (types.size() == 1) {
                return verifyQueryResultType(types.get(0), index);
            } else {
                for (Type type : types) {
                    verifyQueryResultType(type, index);
                }
            }
        } else {
            final ClassInfo typeClass = index.getClassByName(t.name());
            if (typeClass == null) {
                throw new IllegalStateException(t.name() + " was not part of the Quarkus index");
            }
        }
        return t;
    }

    protected DotName createSimpleInterfaceImpl(DotName ifaceName) {
        String fullName = ifaceName.toString();

        // package name: must be in the same package as the interface
        final int index = fullName.lastIndexOf('.');
        String packageName = "";
        if (index > 0 && index < fullName.length() - 1) {
            packageName = fullName.substring(0, index) + ".";
        }

        return DotName.createSimple(packageName
                + (ifaceName.isInner() ? ifaceName.local() : ifaceName.withoutPackagePrefix()) + "_"
                + HashUtil.sha1(ifaceName.toString()));
    }

    protected DotName getPrimitiveTypeName(DotName returnTypeName) {
        if (DotNames.LONG.equals(returnTypeName)) {
            return DotNames.PRIMITIVE_LONG;
        }
        if (DotNames.INTEGER.equals(returnTypeName)) {
            return DotNames.PRIMITIVE_INTEGER;
        }
        if (DotNames.BOOLEAN.equals(returnTypeName)) {
            return DotNames.PRIMITIVE_BOOLEAN;
        }
        return returnTypeName;
    }
}
