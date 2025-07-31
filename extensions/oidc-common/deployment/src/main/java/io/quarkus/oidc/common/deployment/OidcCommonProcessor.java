package io.quarkus.oidc.common.deployment;

import java.lang.constant.ClassDesc;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Singleton;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmo2Adaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.GenericType;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.TypeArgument;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.quarkus.oidc.common.runtime.OidcFilterStorage;
import io.quarkus.oidc.common.runtime.OidcFilterStorage.OidcRequestContextPredicate;
import io.quarkus.oidc.common.runtime.OidcFilterStorage.OidcResponseContextPredicate;

public class OidcCommonProcessor {

    private static final DotName OIDC_ENDPOINT_NAME = DotName.createSimple(OidcEndpoint.class.getName());

    /**
     * Generates {@link io.quarkus.oidc.common.runtime.OidcFilterStorage} bean with metadata like matching
     * {@link OidcEndpoint} for each filter. Additional conditions that define when the filter is applied are collected
     * based on the filter annotations (like TenantFeature). Example:
     *
     * <pre>
     * {@code
     * &#64;Singleton
     * &#64;Unremovable
     * public final class OidcFilterStorageImpl extends OidcFilterStorage {
     *     public OidcFilterStorageImpl(@Any InjectableInstance<OidcRequestFilter> allRequestFilters,
     *             @Any InjectableInstance<OidcResponseFilter> allResponseFilters) {
     *         super(allRequestFilters, allResponseFilters);
     *     }
     *
     *     protected List<OidcEndpoint.Type> getResponseFilterEndpointTypes(Class<?> responseFilterClass) {
     *         // for each response filter class
     *         if (FirstCustomResponseFilter.class == responseFilterClass) {
     *             return List.of(OidcEndpoint.Type.DISCOVERY);
     *         }
     *         return List.of(OidcEndpoint.Type.ALL);
     *     }
     *
     *     protected List<OidcEndpoint.Type> getRequestFilterEndpointTypes(Class<?> requestFilterClass) {
     *         // for each request filter class
     *         if (FirstCustomRequestFilter.class == requestFilterClass) {
     *             return List.of(OidcEndpoint.Type.TOKEN);
     *         }
     *         return List.of(OidcEndpoint.Type.ALL);
     *     }
     *
     *     protected List<OidcRequestContextPredicate> getRequestFilterPredicates(Class<?> requestFilterClass) {
     *         // for each request filter class
     *         if (FirstCustomRequestFilter.class == requestFilterClass) {
     *             // some condition, like
     *             return List.of(new AuthorizationCodeFlowPredicate());
     *         }
     *         return null;
     *     }
     *
     *     protected List<OidcResponseContextPredicate> getResponseFilterPredicates(Class<?> responseFilterClass) {
     *         // for each response filter class
     *         if (FirstCustomResponseFilter.class == responseFilterClass) {
     *             // some condition, like
     *             return List.of(new AuthorizationCodeFlowPredicate());
     *         }
     *         return null;
     *     }
     * }
     * }
     * </pre>
     */
    @BuildStep
    void generateOidcFilterStorageBean(BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            CombinedIndexBuildItem combinedIndexBuildItem, List<OidcFilterPredicateBuildItem> filterPredicates,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeanProducer) {
        var requestFilterPredicates = OidcFilterPredicateBuildItem.getRequestFilters(filterPredicates);
        var responseFilterPredicates = OidcFilterPredicateBuildItem.getResponseFilters(filterPredicates);
        var requestFilters = getFilterClasses(combinedIndexBuildItem.getIndex(), OidcRequestFilter.class);
        var responseFilters = getFilterClasses(combinedIndexBuildItem.getIndex(), OidcResponseFilter.class);
        ClassOutput classOutput = new GeneratedBeanGizmo2Adaptor(generatedBeans);
        Gizmo g = Gizmo.create(classOutput);
        final String generatedClassName = "io.quarkus.oidc.common.codegen.OidcFilterStorageImpl";
        g.class_(generatedClassName, cc -> {
            cc.extends_(OidcFilterStorage.class);
            cc.addAnnotation(Singleton.class);
            cc.final_();

            // public OidcFilterStorageImpl(@Any InjectableInstance<OidcRequestFilter> allRequestFilters,
            //                              @Any InjectableInstance<OidcResponseFilter> allResponseFilters)
            cc.constructor(ctor -> {
                var allRequestFilters = ctor.parameter("allRequestFilters", pc -> {
                    pc.setType(GenericType.of(InjectableInstance.class, List.of(TypeArgument.of(OidcRequestFilter.class))));
                    pc.addAnnotation(Any.class); // this is required for @TenantFeature is a qualifier
                });
                var allResponseFilters = ctor.parameter("allResponseFilters", pc -> {
                    pc.setType(GenericType.of(InjectableInstance.class, List.of(TypeArgument.of(OidcResponseFilter.class))));
                    pc.addAnnotation(Any.class); // this is required for @TenantFeature is a qualifier
                });
                ctor.body((bc) -> {
                    var superCtor = ConstructorDesc.of(OidcFilterStorage.class, ctor.desc().type());
                    bc.invokeSpecial(superCtor, cc.this_(), allRequestFilters, allResponseFilters);
                    bc.return_();
                });
            });

            // protected List<OidcEndpoint.Type> getResponseFilterEndpointTypes(Class<?> filterClass)
            getFilterEndpointTypes(cc, responseFilters, "getResponseFilterEndpointTypes");

            // protected List<OidcEndpoint.Type> getRequestFilterEndpointTypes(Class<?> requestFilterClass)
            getFilterEndpointTypes(cc, requestFilters, "getRequestFilterEndpointTypes");

            // protected List<OidcRequestContextPredicate> getRequestFilterPredicates(Class<?> requestFilterClass)
            getFilterPredicates(cc, "getRequestFilterPredicates", requestFilters, requestFilterPredicates,
                    OidcRequestContextPredicate.class);

            // protected List<OidcResponseContextPredicate> getResponseFilterPredicates(Class<?> responseFilterClass)
            getFilterPredicates(cc, "getResponseFilterPredicates", responseFilters, responseFilterPredicates,
                    OidcResponseContextPredicate.class);
        });
        unremovableBeanProducer.produce(UnremovableBeanBuildItem.beanClassNames(generatedClassName));
    }

    private static void getFilterPredicates(ClassCreator cc, String methodName, Collection<ClassInfo> filters,
            Collection<OidcFilterPredicateBuildItem> filterPredicates, Class<?> predicateClass) {
        cc.method(methodName, mc -> {
            mc.final_();
            mc.protected_();
            mc.returning(GenericType.of(ClassDesc.of(List.class.getName()), List.of(TypeArgument.of(predicateClass))));
            var filterClass = mc.parameter("filterClass", Class.class);
            mc.body(bc -> {
                for (ClassInfo filter : filters) {
                    final String[] filterPredicateClasses = getFilterPredicateClasses(filter, filterPredicates);
                    if (filterPredicateClasses.length > 0) {
                        // if (filterClass == MyCustomFilter.class)
                        var classesAreEqual = bc.eq(filterClass, getClassConstant(filter));
                        bc.if_(classesAreEqual, ifBc -> {
                            final Expr[] expressions = new Expr[filterPredicateClasses.length];
                            for (int i = 0; i < filterPredicateClasses.length; i++) {
                                expressions[i] = ifBc.new_(ClassDesc.of(filterPredicateClasses[i]));
                            }
                            // something like:
                            // return List.of(new AuthorizationCodeFlowPredicate());
                            ifBc.return_(ifBc.listOf(expressions));
                        });
                    }
                }
                // return null; // no predicates detected for this class
                bc.returnNull();
            });
        });
    }

    private static void getFilterEndpointTypes(ClassCreator cc, Collection<ClassInfo> filters, String methodName) {
        cc.method(methodName, mc -> {
            mc.final_();
            mc.protected_();
            mc.returning(GenericType.of(ClassDesc.of(List.class.getName()), List.of(TypeArgument.of(OidcEndpoint.Type.class))));
            var filterClass = mc.parameter("filterClass", Class.class);
            mc.body(bc -> {
                for (ClassInfo filter : filters) {
                    OidcEndpoint.Type[] endpointTypes = getEndpointTypes(filter);
                    if (endpointTypes != null && endpointTypes.length > 0) {
                        // if (filterClass == MyCustomFilter.class)
                        var classesAreEqual = bc.eq(filterClass, getClassConstant(filter));
                        bc.if_(classesAreEqual, ifBc -> ifBc.return_(getOidcEndpointTypes(endpointTypes)));
                        // return List.of(OidcEndpoint.Type.TOKEN);
                    }
                }
                // return List.of(OidcEndpoint.Type.ALL); // default if no other endpoint type is detected
                bc.return_(getOidcEndpointTypes(OidcEndpoint.Type.ALL));
            });
        });
    }

    private static Const getOidcEndpointTypes(OidcEndpoint.Type... oidcEndpointType) {
        return Const.of(Arrays.stream(oidcEndpointType).map(OidcCommonProcessor::getOidcEndpointType).toList());
    }

    private static Const getOidcEndpointType(OidcEndpoint.Type oidcEndpointType) {
        return Const.of(Enum.EnumDesc.of(ClassDesc.of(OidcEndpoint.Type.class.getName()), oidcEndpointType.name()));
    }

    private static Const getClassConstant(ClassInfo className) {
        return Const.of(ClassDesc.of(className.name().toString()));
    }

    private static OidcEndpoint.Type[] getEndpointTypes(ClassInfo filterClassInfo) {
        var oidcEndpointAnnotation = filterClassInfo.annotation(OIDC_ENDPOINT_NAME);
        if (oidcEndpointAnnotation != null && oidcEndpointAnnotation.value() != null) {
            var enumArray = oidcEndpointAnnotation.value().asEnumArray();
            if (enumArray != null && enumArray.length > 0) {
                return Arrays.stream(enumArray).map(OidcEndpoint.Type::valueOf).toArray(OidcEndpoint.Type[]::new);
            }
        }
        return null;
    }

    private static String[] getFilterPredicateClasses(ClassInfo filterClassInfo,
            Collection<OidcFilterPredicateBuildItem> filterPredicates) {
        Set<String> predicateClasses = new HashSet<>();
        for (OidcFilterPredicateBuildItem filterPredicate : filterPredicates) {
            if (filterPredicate.appliesTo(filterClassInfo)) {
                predicateClasses.add(filterPredicate.predicateClass);
            }
        }
        return predicateClasses.toArray(new String[0]);
    }

    private static Collection<ClassInfo> getFilterClasses(IndexView index, Class<?> filterClass) {
        return index.getAllKnownImplementations(filterClass).stream().filter(ci -> !ci.isAbstract() && !ci.isInterface())
                .toList();
    }
}
