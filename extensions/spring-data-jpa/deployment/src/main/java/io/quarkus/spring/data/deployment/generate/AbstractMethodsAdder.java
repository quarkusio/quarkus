package io.quarkus.spring.data.deployment.generate;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
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

import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.spring.data.deployment.DotNames;
import io.quarkus.spring.data.runtime.RepositorySupport;
import io.quarkus.spring.data.runtime.TypesConverter;

public abstract class AbstractMethodsAdder {

    protected void handleLongReturnValue(BlockCreator bc, Expr resultHandle, DotName returnType) {
        if (DotNames.LONG.equals(returnType)) { // handle object Long return type
            resultHandle = bc.invokeStatic(
                    MethodDesc.of(Long.class, "valueOf", Long.class, long.class),
                    resultHandle);
        }
        bc.return_(resultHandle);
    }

    protected void handleIntegerReturnValue(BlockCreator bc, Expr resultHandle, DotName returnType) {
        if (DotNames.INTEGER.equals(returnType)) { // handle object Integer return type
            resultHandle = bc.invokeStatic(
                    MethodDesc.of(Integer.class, "valueOf", Integer.class, int.class),
                    resultHandle);
        }
        bc.return_(resultHandle);
    }

    protected void handleBooleanReturnValue(BlockCreator bc, Expr resultHandle, DotName returnType) {
        if (DotNames.BOOLEAN.equals(returnType)) { // handle object Boolean return type
            resultHandle = bc.invokeStatic(
                    MethodDesc.of(Boolean.class, "valueOf", Boolean.class, boolean.class),
                    resultHandle);
        }
        bc.return_(resultHandle);
    }

    protected void generateFindQueryResultHandling(BlockCreator bc, Expr panacheQueryExpr,
            Integer pageableParameterIndex, Expr[] methodParams,
            ClassInfo repositoryClassInfo, ClassInfo entityClassInfo,
            DotName returnType, Integer limit, String methodName, DotName customResultType, String originalResultType) {

        // Store panacheQuery in a LocalVar so it can be used across nested blocks (try_, ifElse, etc.)
        Expr panacheQuery = bc.localVar("panacheQuery", panacheQueryExpr);

        Expr page = null;
        if (limit != null) {
            // create a custom page object that will limit the results by the limit size
            page = bc.new_(ClassDesc.of(Page.class.getName()), Const.of(limit));
        } else if (pageableParameterIndex != null) {
            page = bc.invokeStatic(
                    MethodDesc.of(TypesConverter.class, "toPanachePage", Page.class, Pageable.class),
                    methodParams[pageableParameterIndex]);
        }

        if (page != null) {
            panacheQuery = bc.localVar("pagedPanacheQuery",
                    bc.invokeInterface(
                            MethodDesc.of(PanacheQuery.class, "page", PanacheQuery.class, Page.class),
                            panacheQuery, page));
        }

        // Need a final copy for use in lambdas
        final Expr finalPanacheQuery = panacheQuery;

        if (returnType.equals(entityClassInfo.name())) {
            String panacheQueryMethodToUse = (limit != null) ? "firstResult" : "singleResult";

            bc.try_(tc -> {
                tc.body(tb -> {
                    Expr singleResult = tb.invokeInterface(
                            MethodDesc.of(PanacheQuery.class, panacheQueryMethodToUse, Object.class),
                            finalPanacheQuery);

                    Expr casted = tb.cast(singleResult, ClassDesc.of(entityClassInfo.name().toString()));
                    tb.return_(casted);
                });
                tc.catch_(NoResultException.class, "e", (cb, e) -> {
                    cb.return_(Const.ofNull(ClassDesc.of(entityClassInfo.name().toString())));
                });
            });

        } else if (DotNames.OPTIONAL.equals(returnType)) {
            String panacheQueryMethodToUse = (limit != null) ? "firstResult" : "singleResult";

            bc.try_(tc -> {
                tc.body(tb -> {
                    Expr singleResult = tb.invokeInterface(
                            MethodDesc.of(PanacheQuery.class, panacheQueryMethodToUse, Object.class),
                            finalPanacheQuery);

                    if (customResultType == null) {
                        Expr casted = tb.cast(singleResult, ClassDesc.of(entityClassInfo.name().toString()));
                        Expr optional = tb.invokeStatic(
                                MethodDesc.of(Optional.class, "ofNullable", Optional.class, Object.class),
                                casted);
                        tb.return_(optional);
                    } else {
                        Expr customResult = tb.invokeStatic(
                                ClassMethodDesc.of(ClassDesc.of(customResultType.toString()), "convert_" + methodName,
                                        GenerationUtil.toMethodTypeDesc(customResultType.toString(), originalResultType)),
                                singleResult);
                        Expr optional = tb.invokeStatic(
                                MethodDesc.of(Optional.class, "ofNullable", Optional.class, Object.class),
                                customResult);
                        tb.return_(optional);
                    }
                });
                tc.catch_(NoResultException.class, "e", (cb, e) -> {
                    Expr emptyOptional = cb.invokeStatic(
                            MethodDesc.of(Optional.class, "empty", Optional.class));
                    cb.return_(emptyOptional);
                });
            });
        } else if (DotNames.LIST.equals(returnType) || DotNames.COLLECTION.equals(returnType)
                || DotNames.SET.equals(returnType) || DotNames.ITERATOR.equals(returnType)
                || DotNames.SPRING_DATA_PAGE.equals(returnType) || DotNames.SPRING_DATA_SLICE.equals(returnType)) {
            Expr list;

            if (customResultType == null) {
                list = bc.invokeInterface(
                        MethodDesc.of(PanacheQuery.class, "list", List.class),
                        finalPanacheQuery);
            } else {
                Expr stream = bc.invokeInterface(
                        MethodDesc.of(PanacheQuery.class, "stream", Stream.class),
                        finalPanacheQuery);

                // Function to convert originResultType to the custom type
                Expr mappingFunction = bc.lambda(Function.class, lc -> {
                    var param = lc.parameter("p", 0);
                    lc.body(lb -> {
                        Expr obj = lb.invokeStatic(
                                ClassMethodDesc.of(ClassDesc.of(customResultType.toString()), "convert_" + methodName,
                                        GenerationUtil.toMethodTypeDesc(customResultType.toString(), originalResultType)),
                                param);
                        lb.return_(obj);
                    });
                });

                stream = bc.invokeInterface(
                        MethodDesc.of(Stream.class, "map", Stream.class, Function.class),
                        stream, mappingFunction);

                // Re-collect the stream into a list
                Expr collector = bc.invokeStatic(
                        MethodDesc.of(Collectors.class, "toList", Collector.class));
                Expr collected = bc.invokeInterface(
                        MethodDesc.of(Stream.class, "collect", Object.class, Collector.class),
                        stream, collector);
                list = bc.cast(collected, ConstantDescs.CD_List);
            }

            if (DotNames.ITERATOR.equals(returnType)) {
                Expr iterator = bc.invokeInterface(
                        MethodDesc.of(Iterable.class, "iterator", Iterator.class),
                        list);
                bc.return_(iterator);
            } else if (DotNames.SET.equals(returnType)) {
                Expr listAsCollection = bc.cast(list, ConstantDescs.CD_Collection);
                Expr set = bc.new_(ClassDesc.of(LinkedHashSet.class.getName()), listAsCollection);
                bc.return_(set);
            } else if (DotNames.SPRING_DATA_PAGE.equals(returnType)) {
                if (pageableParameterIndex != null) {
                    Expr count = bc.invokeInterface(
                            MethodDesc.of(PanacheQuery.class, "count", long.class),
                            finalPanacheQuery);
                    Expr pageResult = bc.new_(ClassDesc.of(PageImpl.class.getName()),
                            list, methodParams[pageableParameterIndex], count);
                    bc.return_(pageResult);
                } else {
                    Expr pageResult = bc.new_(ClassDesc.of(PageImpl.class.getName()), list);
                    bc.return_(pageResult);
                }
            } else if (DotNames.SPRING_DATA_SLICE.equals(returnType)) {
                if (pageableParameterIndex != null) {
                    Expr hasNextPage = bc.invokeInterface(
                            MethodDesc.of(PanacheQuery.class, "hasNextPage", boolean.class),
                            finalPanacheQuery);
                    Expr sliceResult = bc.new_(ClassDesc.of(SliceImpl.class.getName()),
                            list, methodParams[pageableParameterIndex], hasNextPage);
                    bc.return_(sliceResult);
                } else {
                    Expr sliceResult = bc.new_(ClassDesc.of(SliceImpl.class.getName()), list);
                    bc.return_(sliceResult);
                }
            } else {
                bc.return_(list);
            }

        } else if (DotNames.STREAM.equals(returnType)) {
            Expr stream = bc.invokeInterface(
                    MethodDesc.of(PanacheQuery.class, "stream", Stream.class),
                    finalPanacheQuery);
            bc.return_(stream);

        } else if (isHibernateSupportedReturnType(returnType)) {
            Expr singleResult = bc.invokeInterface(
                    MethodDesc.of(PanacheQuery.class, "singleResult", Object.class),
                    finalPanacheQuery);
            bc.return_(singleResult);
        } else if (customResultType != null) {
            String panacheQueryMethodToUse = (limit != null) ? "firstResult" : "singleResult";

            bc.try_(tc -> {
                tc.body(tb -> {
                    Expr singleResult = tb.invokeInterface(
                            MethodDesc.of(PanacheQuery.class, panacheQueryMethodToUse, Object.class),
                            finalPanacheQuery);

                    Expr customResult = tb.invokeStatic(
                            ClassMethodDesc.of(ClassDesc.of(customResultType.toString()), "convert_" + methodName,
                                    GenerationUtil.toMethodTypeDesc(customResultType.toString(), originalResultType)),
                            singleResult);

                    tb.return_(customResult);
                });
                tc.catch_(NoResultException.class, "e", (cb, e) -> {
                    cb.return_(Const.ofNull(ConstantDescs.CD_Object));
                });
            });
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
    protected void handleFlushAutomatically(AnnotationInstance modifyingAnnotation, BlockCreator bc,
            Expr entityClassExpr) {
        final AnnotationValue flushAutomatically = modifyingAnnotation != null ? modifyingAnnotation.value("flushAutomatically")
                : null;
        if (flushAutomatically != null && flushAutomatically.asBoolean()) {
            bc.invokeStatic(
                    MethodDesc.of(RepositorySupport.class, "flush", void.class, Class.class),
                    entityClassExpr);
        }
    }

    /**
     * Clear the underlying persistence context after executing the modifying query if enabled by {@link Modifying}
     * annotation.
     */
    protected void handleClearAutomatically(AnnotationInstance modifyingAnnotation, BlockCreator bc,
            Expr entityClassExpr) {
        final AnnotationValue clearAutomatically = modifyingAnnotation != null ? modifyingAnnotation.value("clearAutomatically")
                : null;
        if (clearAutomatically != null && clearAutomatically.asBoolean()) {
            bc.invokeStatic(
                    MethodDesc.of(RepositorySupport.class, "clear", void.class, Class.class),
                    entityClassExpr);
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
            return verifyQueryResultType(t.asArrayType().constituent(), index);
        } else if (t.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            final List<Type> types = t.asParameterizedType().arguments();
            if (types.size() == 1 && isKnownContainerType(t.name())) {
                return verifyQueryResultType(types.get(0), index);
            } else {
                for (Type type : types) {
                    verifyQueryResultType(type, index);
                }
            }
        } else {
            final ClassInfo typeClass = index.getClassByName(t.name());
            if (typeClass == null) {
                throw new IllegalStateException(
                        t.name() + " is not in the Quarkus Jandex index and cannot be used as a query return type. " +
                                "Consider using a simpler type or ensure this class is properly indexed. ");
            }
        }
        return t;
    }

    private static boolean isKnownContainerType(DotName name) {
        return DotNames.LIST.equals(name)
                || DotNames.COLLECTION.equals(name)
                || DotNames.SET.equals(name)
                || DotNames.OPTIONAL.equals(name)
                || DotNames.STREAM.equals(name)
                || DotNames.ITERATOR.equals(name)
                || DotNames.SPRING_DATA_PAGE.equals(name)
                || DotNames.SPRING_DATA_SLICE.equals(name);
    }

    protected DotName createSimpleInterfaceImpl(DotName ifaceName, DotName entityName) {
        String fullName = ifaceName.toString();

        // package name: must be in the same package as the interface
        final int index = fullName.lastIndexOf('.');
        String packageName = "";
        if (index > 0 && index < fullName.length() - 1) {
            packageName = fullName.substring(0, index) + ".";
        }

        return DotName.createSimple(packageName
                + (ifaceName.isInner() ? ifaceName.local() : ifaceName.withoutPackagePrefix()) + "_"
                + HashUtil.sha1(ifaceName.toString()) + "_" + HashUtil.sha1(entityName.toString()));
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
