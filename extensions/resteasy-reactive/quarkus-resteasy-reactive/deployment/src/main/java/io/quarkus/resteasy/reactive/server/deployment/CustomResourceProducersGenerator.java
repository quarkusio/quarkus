package io.quarkus.resteasy.reactive.server.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.parameters.CookieParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.HeaderParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.MatrixParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.PathParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.QueryParamExtractor;

import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;

final class CustomResourceProducersGenerator {

    private static final DotName DOTNAME_REQUEST_SCOPED = DotName.createSimple(RequestScoped.class.getSimpleName());

    private CustomResourceProducersGenerator() {
    }

    /**
     * We generate a class that contains as many CDI producer methods as there are JAX-RS Resources that use JAX-RS params.
     *
     * If for example there was a single such JAX-RS resource looking like:
     *
     * <code><pre>
     *
     * &#64;Path("/query")
     * public class QueryParamResource {
     *
     * 	 private final String queryParamValue;
     * 	 private final UriInfo uriInfo;
     *
     * 	 public QueryParamResource(@QueryParam("p1") String headerValue, @Context UriInfo uriInfo) {
     * 		this.headerValue = headerValue;
     *   }
     *
     *   &#64;GET
     *   public String get() {
     * 	    // DO something
     *   }
     * }
     *
     *  </pre></code>
     *
     *
     * then the generated producer would look like this:
     *
     * <code><pre>
     *
     *  &#64;Singleton
     * public class ResourcesWithParamProducer {
     *
     *    &#64;Inject
     *    CurrentVertxRequest currentVertxRequest;
     *
     *    &#64;Produces
     *    &#64;RequestScoped
     *    public QueryParamResource producer_QueryParamResource_somehash(UriInfo uriInfo) {
     * 		return new QueryParamResource(getContext().getContext().queryParams().get("p1"), uriInfo);
     *    }
     *
     * 	private QuarkusRestRequestContext getContext() {
     * 		return (QuarkusRestRequestContext) currentVertxRequest.getOtherHttpContextObject();
     *    }
     * }
     *
     *  </pre></code>
     */
    public static void generate(Map<DotName, MethodInfo> resourcesThatNeedCustomProducer,
            Set<String> beanParamsThatNeedCustomProducer,
            BuildProducer<GeneratedBeanBuildItem> generatedBeanBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        GeneratedBeanGizmoAdaptor classOutput = new GeneratedBeanGizmoAdaptor(generatedBeanBuildItemBuildProducer);
        try (ClassCreator c = new ClassCreator(classOutput, "io.quarkus.rest.cdi.ResourceWithJaxRsCtorParamsProducer", null,
                Object.class.getName())) {
            c.addAnnotation(Singleton.class);

            MethodCreator getContextMethodCreator = c.getMethodCreator("getContext", ResteasyReactiveRequestContext.class);
            MethodCreator getHeaderParamMethodCreator = c.getMethodCreator("getHeaderParam", String.class, String.class);
            MethodCreator getQueryParamMethodCreator = c.getMethodCreator("getQueryParam", String.class, String.class);
            MethodCreator getPathParamMethodCreator = c.getMethodCreator("getPathParam", String.class, int.class);
            MethodCreator getMatrixParamMethodCreator = c.getMethodCreator("getMatrixParam", String.class, String.class);
            MethodCreator getCookieParamMethodCreator = c.getMethodCreator("getCookieParam", String.class, String.class);

            try (MethodCreator m = getContextMethodCreator) {
                m.setModifiers(Modifier.PRIVATE);

                ResultHandle otherHttpContextObjectHandle = m.invokeStaticMethod(
                        MethodDescriptor.ofMethod(CurrentRequestManager.class, "get", ResteasyReactiveRequestContext.class));
                ResultHandle result = m.checkCast(otherHttpContextObjectHandle, ResteasyReactiveRequestContext.class);
                m.returnValue(result);
            }

            try (MethodCreator m = getHeaderParamMethodCreator) {
                m.setModifiers(Modifier.PRIVATE);
                ResultHandle quarkusRestContextHandle = m.invokeVirtualMethod(getContextMethodCreator.getMethodDescriptor(),
                        m.getThis());
                ResultHandle extractorHandle = m.newInstance(
                        MethodDescriptor.ofConstructor(HeaderParamExtractor.class, String.class, boolean.class),
                        m.getMethodParam(0), m.load(true));
                ResultHandle resultHandle = m.invokeVirtualMethod(MethodDescriptor.ofMethod(HeaderParamExtractor.class,
                        "extractParameter", Object.class, ResteasyReactiveRequestContext.class), extractorHandle,
                        quarkusRestContextHandle);
                m.returnValue(resultHandle);
            }

            try (MethodCreator m = getQueryParamMethodCreator) {
                m.setModifiers(Modifier.PRIVATE);
                ResultHandle quarkusRestContextHandle = m.invokeVirtualMethod(getContextMethodCreator.getMethodDescriptor(),
                        m.getThis());
                ResultHandle extractorHandle = m.newInstance(
                        MethodDescriptor.ofConstructor(QueryParamExtractor.class, String.class, boolean.class, boolean.class),
                        m.getMethodParam(0), m.load(true), m.load(false));
                ResultHandle resultHandle = m.invokeVirtualMethod(MethodDescriptor.ofMethod(QueryParamExtractor.class,
                        "extractParameter", Object.class, ResteasyReactiveRequestContext.class), extractorHandle,
                        quarkusRestContextHandle);
                m.returnValue(resultHandle);
            }

            try (MethodCreator m = getPathParamMethodCreator) {
                m.setModifiers(Modifier.PRIVATE);
                ResultHandle quarkusRestContextHandle = m.invokeVirtualMethod(getContextMethodCreator.getMethodDescriptor(),
                        m.getThis());
                ResultHandle extractorHandle = m.newInstance(
                        MethodDescriptor.ofConstructor(PathParamExtractor.class, int.class, boolean.class, boolean.class),
                        m.getMethodParam(0), m.load(false), m.load(true));
                ResultHandle resultHandle = m.invokeVirtualMethod(MethodDescriptor.ofMethod(PathParamExtractor.class,
                        "extractParameter", Object.class, ResteasyReactiveRequestContext.class), extractorHandle,
                        quarkusRestContextHandle);
                m.returnValue(resultHandle);
            }

            try (MethodCreator m = getMatrixParamMethodCreator) {
                m.setModifiers(Modifier.PRIVATE);
                ResultHandle quarkusRestContextHandle = m.invokeVirtualMethod(getContextMethodCreator.getMethodDescriptor(),
                        m.getThis());
                ResultHandle extractorHandle = m.newInstance(
                        MethodDescriptor.ofConstructor(MatrixParamExtractor.class, String.class, boolean.class, boolean.class),
                        m.getMethodParam(0), m.load(true), m.load(false));
                ResultHandle resultHandle = m.invokeVirtualMethod(MethodDescriptor.ofMethod(MatrixParamExtractor.class,
                        "extractParameter", Object.class, ResteasyReactiveRequestContext.class), extractorHandle,
                        quarkusRestContextHandle);
                m.returnValue(resultHandle);
            }

            try (MethodCreator m = getCookieParamMethodCreator) {
                m.setModifiers(Modifier.PRIVATE);
                ResultHandle quarkusRestContextHandle = m.invokeVirtualMethod(getContextMethodCreator.getMethodDescriptor(),
                        m.getThis());
                ResultHandle extractorHandle = m.newInstance(
                        MethodDescriptor.ofConstructor(CookieParamExtractor.class, String.class),
                        m.getMethodParam(0));
                ResultHandle resultHandle = m.invokeVirtualMethod(MethodDescriptor.ofMethod(CookieParamExtractor.class,
                        "extractParameter", Object.class, ResteasyReactiveRequestContext.class), extractorHandle,
                        quarkusRestContextHandle);
                m.returnValue(resultHandle);
            }

            for (Map.Entry<DotName, MethodInfo> entry : resourcesThatNeedCustomProducer.entrySet()) {
                DotName resourceDotName = entry.getKey();
                MethodInfo ctor = entry.getValue();

                List<AnnotationInstance> annotations = ctor.annotations();

                Map<Short, List<AnnotationInstance>> paramIndexToAnnotations = new HashMap<>();
                for (AnnotationInstance annotation : annotations) {
                    if (annotation.target().kind() != AnnotationTarget.Kind.METHOD_PARAMETER) {
                        continue;
                    }
                    MethodParameterInfo methodParameterInfo = annotation.target().asMethodParameter();
                    List<AnnotationInstance> annotationsOfParam = paramIndexToAnnotations.get(methodParameterInfo.position());
                    if (annotationsOfParam == null) {
                        annotationsOfParam = new ArrayList<>(1);
                        annotationsOfParam.add(annotation);
                        paramIndexToAnnotations.put(methodParameterInfo.position(), annotationsOfParam);
                    }
                }
                // We first need to determine how each parameter will be handled
                // The JAX-RS parameters will be handled directly
                // The other parameters we will just add to the method signature of the producer method
                // and let CDI populate them for us
                List<CtorParamData> ctorParamData = new ArrayList<>(ctor.parameters().size());
                for (short i = 0; i < ctor.parameters().size(); i++) {
                    Type parameterType = ctor.parameters().get(i);
                    if (!paramIndexToAnnotations.containsKey(i)) {
                        ctorParamData.add(new CtorParamData(CtorParamData.CustomProducerParameterType.OTHER, parameterType));
                    } else {
                        List<AnnotationInstance> paramAnnotations = paramIndexToAnnotations.get(i);
                        List<AnnotationInstance> jaxRSAnnotationsOfParam = new ArrayList<>(paramAnnotations.size());
                        for (AnnotationInstance paramAnnotation : paramAnnotations) {
                            if (ResteasyReactiveDotNames.RESOURCE_CTOR_PARAMS_THAT_NEED_HANDLING
                                    .contains(paramAnnotation.name())) {
                                jaxRSAnnotationsOfParam.add(paramAnnotation);
                            }
                        }
                        if (jaxRSAnnotationsOfParam.isEmpty()) {
                            ctorParamData
                                    .add(new CtorParamData(CtorParamData.CustomProducerParameterType.OTHER, parameterType));
                        } else if (jaxRSAnnotationsOfParam.size() > 1) {
                            throw new IllegalArgumentException("Parameter: " + i + " of the constructor of class '"
                                    + resourceDotName + "' contains multiple JAX-RS annotations, which is not valid");
                        } else {
                            AnnotationInstance jaxRSAnnotationOfParam = jaxRSAnnotationsOfParam.iterator().next();
                            if (!parameterType.name().equals(ResteasyReactiveDotNames.STRING)) {
                                // TODO: do we need to support converters here?
                                throw new IllegalArgumentException("Parameter: " + i + " of the constructor of class '"
                                        + resourceDotName + "' which is annotated with '" + jaxRSAnnotationOfParam.name()
                                        + "' can only be of type String");
                            }
                            CtorParamData.CustomProducerParameterType customProducerParameterType;
                            if (jaxRSAnnotationOfParam.name().equals(ResteasyReactiveDotNames.QUERY_PARAM)) {
                                customProducerParameterType = CtorParamData.CustomProducerParameterType.QUERY;
                            } else if (jaxRSAnnotationOfParam.name().equals(ResteasyReactiveDotNames.HEADER_PARAM)) {
                                customProducerParameterType = CtorParamData.CustomProducerParameterType.HEADER;
                            } else if (jaxRSAnnotationOfParam.name().equals(ResteasyReactiveDotNames.PATH_PARAM)) {
                                customProducerParameterType = CtorParamData.CustomProducerParameterType.PATH;
                            } else if (jaxRSAnnotationOfParam.name().equals(ResteasyReactiveDotNames.MATRIX_PARAM)) {
                                customProducerParameterType = CtorParamData.CustomProducerParameterType.MATRIX;
                            } else if (jaxRSAnnotationOfParam.name().equals(ResteasyReactiveDotNames.COOKIE_PARAM)) {
                                customProducerParameterType = CtorParamData.CustomProducerParameterType.COOKIE;
                            } else {
                                throw new IllegalStateException("Unsupported type '" + jaxRSAnnotationOfParam.name()
                                        + "' used as an annotation in constructor of class '" + resourceDotName + "'");
                            }
                            String name = jaxRSAnnotationOfParam.value().asString(); // all the types we handle have the same annotation method
                            ctorParamData.add(new CtorParamData(customProducerParameterType, parameterType, name));
                        }
                    }
                }
                List<String> producerMethodParameterTypes = new ArrayList<>(ctor.parameters().size());
                for (CtorParamData ctorParamDatum : ctorParamData) {
                    if (ctorParamDatum.getCustomProducerParameterType() == CtorParamData.CustomProducerParameterType.OTHER) {
                        producerMethodParameterTypes.add(ctorParamDatum.getParameterType().name().toString());
                    }
                }

                String methodName = "producer_" + resourceDotName.withoutPackagePrefix()
                        + HashUtil.sha1(resourceDotName.toString());
                try (MethodCreator m = c.getMethodCreator(methodName, resourceDotName.toString(),
                        producerMethodParameterTypes.toArray(new String[0]))) {
                    m.addAnnotation(Produces.class);
                    m.addAnnotation(RequestScoped.class);
                    // we need this to be unremovable because the Resource itself does not get referenced in any injection point
                    m.addAnnotation(Unremovable.class);

                    List<ResultHandle> ctorParamHandles = new ArrayList<>(ctorParamData.size());
                    int otherParamIndex = 0;
                    for (CtorParamData ctorParamDatum : ctorParamData) {
                        CtorParamData.CustomProducerParameterType type = ctorParamDatum.getCustomProducerParameterType();
                        ResultHandle resultHandle;
                        if (type == CtorParamData.CustomProducerParameterType.OTHER) {
                            resultHandle = m.getMethodParam(otherParamIndex);
                            otherParamIndex++;
                        } else if (type == CtorParamData.CustomProducerParameterType.HEADER) {
                            resultHandle = m.invokeVirtualMethod(getHeaderParamMethodCreator.getMethodDescriptor(), m.getThis(),
                                    m.load(ctorParamDatum.getAnnotationValue()));
                        } else if (type == CtorParamData.CustomProducerParameterType.QUERY) {
                            resultHandle = m.invokeVirtualMethod(getQueryParamMethodCreator.getMethodDescriptor(), m.getThis(),
                                    m.load(ctorParamDatum.getAnnotationValue()));
                        } else if (type == CtorParamData.CustomProducerParameterType.PATH) {
                            resultHandle = m.invokeVirtualMethod(getPathParamMethodCreator.getMethodDescriptor(), m.getThis(),
                                    m.load(0)); // TODO: this is not correct, we really need to look up the index
                        } else if (type == CtorParamData.CustomProducerParameterType.MATRIX) {
                            resultHandle = m.invokeVirtualMethod(getMatrixParamMethodCreator.getMethodDescriptor(), m.getThis(),
                                    m.load(ctorParamDatum.getAnnotationValue()));
                        } else if (type == CtorParamData.CustomProducerParameterType.COOKIE) {
                            resultHandle = m.invokeVirtualMethod(getCookieParamMethodCreator.getMethodDescriptor(), m.getThis(),
                                    m.load(ctorParamDatum.getAnnotationValue()));
                        } else {
                            throw new IllegalStateException("Unknown type '" + type
                                    + "' used as an annotation in constructor of class '" + resourceDotName + "'");
                        }
                        ctorParamHandles.add(resultHandle);
                    }

                    // FIXME: this doesn't actually support injection in the resource instance
                    m.returnValue(m.newInstance(ctor, ctorParamHandles.toArray(new ResultHandle[0])));
                }
            }
            // FIXME: support constructors for bean params too
            additionalBeanBuildItemBuildProducer
                    .produce(AdditionalBeanBuildItem.builder().addBeanClasses(beanParamsThatNeedCustomProducer)
                            // FIXME: we should add this, but for that we also need to make the resource class request-scoped
                            //                                                         .setDefaultScope(DOTNAME_REQUEST_SCOPED)
                            //                                                         .setUnremovable()
                            .build());
        }
    }

    private static class CtorParamData {
        private final CustomProducerParameterType customProducerParameterType;
        private final Type parameterType;
        private final String annotationValue; // represents the name obtained from the JAX-RS annotation

        public CtorParamData(CustomProducerParameterType customProducerParameterType, Type parameterType) {
            this(customProducerParameterType, parameterType, null);
        }

        public CtorParamData(CustomProducerParameterType customProducerParameterType, Type parameterType,
                String annotationValue) {
            this.customProducerParameterType = customProducerParameterType;
            this.parameterType = parameterType;
            this.annotationValue = annotationValue;
        }

        public CustomProducerParameterType getCustomProducerParameterType() {
            return customProducerParameterType;
        }

        public Type getParameterType() {
            return parameterType;
        }

        public String getAnnotationValue() {
            return annotationValue;
        }

        private enum CustomProducerParameterType {
            QUERY,
            HEADER,
            PATH,
            MATRIX,
            COOKIE,
            OTHER
        }
    }
}
