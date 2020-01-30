package io.quarkus.spring.data.deployment.generate;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.NoResultException;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.spring.data.deployment.DotNames;
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
            DotName returnType, Integer limit, String methodName, DotName customResultType) {

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
            ResultHandle casted = tryBlock.checkCast(singleResult, entityClassInfo.name().toString());
            ResultHandle optional = tryBlock.invokeStaticMethod(
                    MethodDescriptor.ofMethod(Optional.class, "of", Optional.class, Object.class),
                    casted);
            tryBlock.returnValue(optional);
            CatchBlockCreator catchBlock = tryBlock.addCatch(NoResultException.class);
            ResultHandle emptyOptional = catchBlock.invokeStaticMethod(
                    MethodDescriptor.ofMethod(Optional.class, "empty", Optional.class));
            catchBlock.returnValue(emptyOptional);
        } else if (DotNames.LIST.equals(returnType) || DotNames.COLLECTION.equals(returnType)
                || DotNames.ITERATOR.equals(returnType)) {
            ResultHandle list;

            if (customResultType == null) {
                list = methodCreator.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(PanacheQuery.class, "list", List.class),
                        panacheQuery);
            } else {
                ResultHandle stream = methodCreator.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(PanacheQuery.class, "stream", Stream.class),
                        panacheQuery);

                // Function to convert Object[] to the custom type (using the generated static convert method)
                FunctionCreator function = methodCreator.createFunction(Function.class);
                BytecodeCreator funcBytecode = function.getBytecode();
                ResultHandle obj = funcBytecode.invokeStaticMethod(
                        MethodDescriptor.ofMethod(customResultType.toString(), "convert_" + methodName,
                                customResultType.toString(),
                                Object[].class.getName()),
                        funcBytecode.getMethodParam(0));
                funcBytecode.returnValue(obj);

                stream = methodCreator.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Stream.class, "map", Stream.class, Function.class),
                        stream, function.getInstance());

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
            }
            methodCreator.returnValue(list);

        } else if (DotNames.STREAM.equals(returnType)) {
            ResultHandle stream = methodCreator.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(PanacheQuery.class, "stream", Stream.class),
                    panacheQuery);
            methodCreator.returnValue(stream);

        } else if (DotNames.SPRING_DATA_PAGE.equals(returnType)) {
            ResultHandle list = methodCreator.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(PanacheQuery.class, "list", List.class),
                    panacheQuery);
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
            ResultHandle list = methodCreator.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(PanacheQuery.class, "list", List.class),
                    panacheQuery);
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

        } else {
            throw new IllegalArgumentException(
                    "Return type of method " + methodName + " of Repository " + repositoryClassInfo
                            + " does not match find query type");
        }
    }
}
