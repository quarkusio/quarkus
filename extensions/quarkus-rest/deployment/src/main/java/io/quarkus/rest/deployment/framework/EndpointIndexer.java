package io.quarkus.rest.deployment.framework;

import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.BLOCKING;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.CONSUMES;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.LIST;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.PATH;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.PRODUCES;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.QUERY_PARAM;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.SET;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.SORTED_SET;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.STRING;
import static io.quarkus.rest.deployment.framework.QuarkusRestDotNames.SUSPENDED;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.sse.SseEventSink;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.runtime.QuarkusRestConfig;
import io.quarkus.rest.runtime.QuarkusRestRecorder;
import io.quarkus.rest.runtime.core.parameters.converters.GeneratedParameterConverter;
import io.quarkus.rest.runtime.core.parameters.converters.ListConverter;
import io.quarkus.rest.runtime.core.parameters.converters.ParameterConverter;
import io.quarkus.rest.runtime.core.parameters.converters.SetConverter;
import io.quarkus.rest.runtime.core.parameters.converters.SortedSetConverter;
import io.quarkus.rest.runtime.model.MethodParameter;
import io.quarkus.rest.runtime.model.ParameterType;
import io.quarkus.rest.runtime.model.ResourceClass;
import io.quarkus.rest.runtime.model.ResourceMethod;
import io.quarkus.rest.runtime.spi.EndpointInvoker;
import io.quarkus.runtime.util.HashUtil;

public class EndpointIndexer {

    private static final Map<String, String> primitiveTypes;

    private static final Logger log = Logger.getLogger(EndpointInvoker.class);

    static {
        Map<String, String> prims = new HashMap<>();
        prims.put(byte.class.getName(), Byte.class.getName());
        prims.put(Byte.class.getName(), Byte.class.getName());
        prims.put(boolean.class.getName(), Boolean.class.getName());
        prims.put(Boolean.class.getName(), Boolean.class.getName());
        prims.put(char.class.getName(), Character.class.getName());
        prims.put(Character.class.getName(), Character.class.getName());
        prims.put(short.class.getName(), Short.class.getName());
        prims.put(Short.class.getName(), Short.class.getName());
        prims.put(int.class.getName(), Integer.class.getName());
        prims.put(Integer.class.getName(), Integer.class.getName());
        prims.put(float.class.getName(), Float.class.getName());
        prims.put(Float.class.getName(), Float.class.getName());
        prims.put(double.class.getName(), Double.class.getName());
        prims.put(Double.class.getName(), Double.class.getName());
        prims.put(long.class.getName(), Long.class.getName());
        prims.put(Long.class.getName(), Long.class.getName());
        primitiveTypes = Collections.unmodifiableMap(prims);
    }

    public static ResourceClass createEndpoints(IndexView index, ClassInfo classInfo, BeanContainer beanContainer,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer, QuarkusRestRecorder recorder,
            Map<String, String> existingConverters, Map<DotName, String> scannedResourcePaths, QuarkusRestConfig config) {
        try {
            String path = scannedResourcePaths.get(classInfo.name());
            List<ResourceMethod> methods = createEndpoints(index, classInfo, classInfo, new HashSet<>(),
                    generatedClassBuildItemBuildProducer, recorder, existingConverters, config);
            ResourceClass clazz = new ResourceClass();
            clazz.getMethods().addAll(methods);
            clazz.setClassName(classInfo.name().toString());
            if (path != null) {
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                clazz.setPath(path);
            }
            clazz.setFactory(recorder.factory(clazz.getClassName(), beanContainer));
            return clazz;
        } catch (Exception e) {
            if (Modifier.isInterface(classInfo.flags()) || Modifier.isAbstract(classInfo.flags())) {
                //kinda bogus, but we just ignore failed interfaces for now
                //they can have methods that are not valid until they are actually extended by a concrete type
                log.debug("Ignoring interface " + classInfo.name(), e);
                return null;
            }
            throw new RuntimeException(e);
        }
    }

    private static List<ResourceMethod> createEndpoints(IndexView index, ClassInfo currentClassInfo,
            ClassInfo actualEndpointInfo, Set<String> seenMethods,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer, QuarkusRestRecorder recorder,
            Map<String, String> existingConverters, QuarkusRestConfig config) {
        List<ResourceMethod> ret = new ArrayList<>();
        String[] classProduces = readStringArrayValue(currentClassInfo.classAnnotation(QuarkusRestDotNames.PRODUCES));
        String[] classConsumes = readStringArrayValue(currentClassInfo.classAnnotation(QuarkusRestDotNames.CONSUMES));

        for (DotName httpMethod : QuarkusRestDotNames.JAXRS_METHOD_ANNOTATIONS) {
            List<AnnotationInstance> foundMethods = currentClassInfo.annotations().get(httpMethod);
            if (foundMethods != null) {
                for (AnnotationInstance annotation : foundMethods) {
                    MethodInfo info = annotation.target().asMethod();
                    String descriptor = methodDescriptor(info);
                    if (seenMethods.contains(descriptor)) {
                        continue;
                    }
                    seenMethods.add(descriptor);
                    String methodPath = readStringValue(info.annotation(PATH));
                    if (methodPath != null) {
                        if (!methodPath.startsWith("/")) {
                            methodPath = "/" + methodPath;
                        }
                    } else {
                        methodPath = "/";
                    }
                    ResourceMethod method = createResourceMethod(currentClassInfo, actualEndpointInfo,
                            generatedClassBuildItemBuildProducer,
                            recorder, classProduces, classConsumes, httpMethod, info, methodPath, index, existingConverters,
                            config);

                    ret.add(method);
                }
            }
        }
        //now resource locator methods
        List<AnnotationInstance> foundMethods = currentClassInfo.annotations().get(PATH);
        if (foundMethods != null) {
            for (AnnotationInstance annotation : foundMethods) {
                if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                    MethodInfo info = annotation.target().asMethod();
                    String descriptor = methodDescriptor(info);
                    if (seenMethods.contains(descriptor)) {
                        continue;
                    }
                    seenMethods.add(descriptor);
                    String methodPath = readStringValue(annotation);
                    if (methodPath != null) {
                        if (!methodPath.startsWith("/")) {
                            methodPath = "/" + methodPath;
                        }
                    }
                    ResourceMethod method = createResourceMethod(currentClassInfo, actualEndpointInfo,
                            generatedClassBuildItemBuildProducer,
                            recorder, classProduces, classConsumes, null, info, methodPath, index, existingConverters, config);
                    ret.add(method);
                }
            }
        }

        DotName superClassName = currentClassInfo.superName();
        if (superClassName != null && !superClassName.equals(DotNames.OBJECT)) {
            ClassInfo superClass = index.getClassByName(superClassName);
            if (superClass != null) {
                ret.addAll(createEndpoints(index, superClass, actualEndpointInfo, seenMethods,
                        generatedClassBuildItemBuildProducer, recorder, existingConverters, config));
            }
        }
        List<DotName> interfaces = currentClassInfo.interfaceNames();
        for (DotName i : interfaces) {
            ClassInfo superClass = index.getClassByName(i);
            if (superClass != null) {
                ret.addAll(createEndpoints(index, superClass, actualEndpointInfo, seenMethods,
                        generatedClassBuildItemBuildProducer, recorder, existingConverters, config));
            }
        }
        return ret;
    }

    private static ResourceMethod createResourceMethod(ClassInfo currentClassInfo, ClassInfo actualEndpointInfo,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer, QuarkusRestRecorder recorder,
            String[] classProduces, String[] classConsumes, DotName httpMethod, MethodInfo info, String methodPath,
            IndexView indexView, Map<String, String> existingEndpoints, QuarkusRestConfig config) {
        try {
            Map<DotName, AnnotationInstance>[] parameterAnnotations = new Map[info.parameters().size()];
            MethodParameter[] methodParameters = new MethodParameter[info.parameters()
                    .size()];
            for (int paramPos = 0; paramPos < info.parameters().size(); ++paramPos) {
                parameterAnnotations[paramPos] = new HashMap<>();
            }
            for (AnnotationInstance i : info.annotations()) {
                if (i.target().kind() == AnnotationTarget.Kind.METHOD_PARAMETER) {
                    parameterAnnotations[i.target().asMethodParameter().position()].put(i.name(), i);
                }
            }
            boolean suspended = false;
            boolean sse = false;
            for (int i = 0; i < methodParameters.length; ++i) {
                Map<DotName, AnnotationInstance> anns = parameterAnnotations[i];

                String name = null;
                AnnotationInstance pathParam = anns.get(QuarkusRestDotNames.PATH_PARAM);
                AnnotationInstance queryParam = anns.get(QUERY_PARAM);
                AnnotationInstance headerParam = anns.get(QuarkusRestDotNames.HEADER_PARAM);
                AnnotationInstance formParam = anns.get(QuarkusRestDotNames.FORM_PARAM);
                AnnotationInstance contextParam = anns.get(QuarkusRestDotNames.CONTEXT);
                AnnotationInstance defaultValueAnnotation = anns.get(QuarkusRestDotNames.DEFAULT_VALUE);
                AnnotationInstance suspendedAnnotation = anns.get(SUSPENDED);
                String defaultValue = null;
                if (defaultValueAnnotation != null) {
                    defaultValue = defaultValueAnnotation.value().asString();
                }
                ParameterType type;
                if (moreThanOne(pathParam, queryParam, headerParam, formParam, contextParam)) {
                    throw new RuntimeException(
                            "Cannot have more than one of @PathParam, @QueryParam, @HeaderParam, @FormParam, @Context on "
                                    + info);
                } else if (pathParam != null) {
                    name = pathParam.value().asString();
                    type = ParameterType.PATH;
                } else if (queryParam != null) {
                    name = queryParam.value().asString();
                    type = ParameterType.QUERY;
                } else if (headerParam != null) {
                    name = headerParam.value().asString();
                    type = ParameterType.HEADER;
                } else if (formParam != null) {
                    name = formParam.value().asString();
                    type = ParameterType.FORM;
                } else if (contextParam != null) {
                    // no name required
                    type = ParameterType.CONTEXT;
                } else if (suspendedAnnotation != null) {
                    // no name required
                    type = ParameterType.ASYNC_RESPONSE;
                    suspended = true;
                } else {
                    type = ParameterType.BODY;
                }
                String elementType;
                boolean single = true;
                Supplier<ParameterConverter> converter = null;
                Type paramType = info.parameters().get(i);
                if (paramType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                    ParameterizedType pt = paramType.asParameterizedType();
                    if (pt.name().equals(LIST)) {
                        single = false;
                        elementType = toClassName(pt.arguments().get(0), currentClassInfo, actualEndpointInfo, indexView);
                        converter = extractConverter(elementType, indexView, generatedClassBuildItemBuildProducer,
                                existingEndpoints, info);
                        converter = new ListConverter.ListSupplier(converter);
                    } else if (pt.name().equals(SET)) {
                        single = false;
                        elementType = toClassName(pt.arguments().get(0), currentClassInfo, actualEndpointInfo, indexView);
                        converter = extractConverter(elementType, indexView, generatedClassBuildItemBuildProducer,
                                existingEndpoints, info);
                        converter = new SetConverter.SetSupplier(converter);
                    } else if (pt.name().equals(SORTED_SET)) {
                        single = false;
                        elementType = toClassName(pt.arguments().get(0), currentClassInfo, actualEndpointInfo, indexView);
                        converter = extractConverter(elementType, indexView, generatedClassBuildItemBuildProducer,
                                existingEndpoints, info);
                        converter = new SortedSetConverter.SortedSetSupplier(converter);
                    } else {
                        throw new RuntimeException("Invalid parameter type " + pt);
                    }
                } else {
                    elementType = toClassName(paramType, currentClassInfo, actualEndpointInfo, indexView);
                    if (type != ParameterType.CONTEXT && type != ParameterType.BODY && type != ParameterType.ASYNC_RESPONSE) {
                        converter = extractConverter(elementType, indexView, generatedClassBuildItemBuildProducer,
                                existingEndpoints, info);
                    }
                    if (type == ParameterType.CONTEXT && elementType.equals(SseEventSink.class.getName())) {
                        sse = true;
                    }
                }
                if (suspendedAnnotation != null && !elementType.equals(AsyncResponse.class.getName())) {
                    throw new RuntimeException("Can only inject AsyncResponse on methods marked @Suspended");
                }
                methodParameters[i] = new MethodParameter(name,
                        elementType, type, single, converter, defaultValue);
            }

            String[] produces = readStringArrayValue(info.annotation(PRODUCES), classProduces);
            String[] consumes = readStringArrayValue(info.annotation(CONSUMES), classConsumes);
            boolean blocking = config.blocking;
            AnnotationInstance blockingAnnotation = info.annotation(BLOCKING);
            if (blockingAnnotation == null) {
                blockingAnnotation = info.declaringClass().classAnnotation(BLOCKING);
            }
            if (blockingAnnotation != null) {
                AnnotationValue value = blockingAnnotation.value();
                if (value != null) {
                    blocking = value.asBoolean();
                } else {
                    blocking = true;
                }
            }
            ResourceMethod method = new ResourceMethod()
                    .setHttpMethod(annotationToMethod(httpMethod))
                    .setPath(methodPath)
                    .setConsumes(consumes)
                    .setName(info.name())
                    .setBlocking(blocking)
                    .setSuspended(suspended)
                    .setSse(sse)
                    .setParameters(methodParameters)
                    // FIXME: resolved arguments ?
                    .setReturnType(AsmUtil.getSignature(info.returnType(), new Function<String, String>() {
                        @Override
                        public String apply(String v) {
                            //we attempt to resolve type variables
                            ClassInfo declarer = info.declaringClass();
                            int pos = -1;
                            for (;;) {
                                if (declarer == null) {
                                    return null;
                                }
                                List<TypeVariable> typeParameters = declarer.typeParameters();
                                for (int i = 0; i < typeParameters.size(); i++) {
                                    TypeVariable tv = typeParameters.get(i);
                                    if (tv.identifier().equals(v)) {
                                        pos = i;
                                    }
                                }
                                if (pos != -1) {
                                    break;
                                }
                                declarer = indexView.getClassByName(declarer.superName());
                            }
                            Type type = JandexUtil
                                    .resolveTypeParameters(info.declaringClass().name(), declarer.name(), indexView)
                                    .get(pos);
                            if (type.kind() == Type.Kind.TYPE_VARIABLE && type.asTypeVariable().identifier().equals(v)) {
                                List<Type> bounds = type.asTypeVariable().bounds();
                                if (bounds.isEmpty()) {
                                    return "Ljava/lang/Object;";
                                }
                                return AsmUtil.getSignature(bounds.get(0), this);
                            } else {
                                return AsmUtil.getSignature(type, this);
                            }
                        }
                    }))
                    .setProduces(produces);

            StringBuilder sigBuilder = new StringBuilder();
            sigBuilder.append(method.getName())
                    .append(method.getReturnType());
            for (MethodParameter t : method.getParameters()) {
                sigBuilder.append(t);
            }
            String baseName = currentClassInfo.name() + "$quarkusrestinvoker$" + method.getName() + "_"
                    + HashUtil.sha1(sigBuilder.toString());
            try (ClassCreator classCreator = new ClassCreator(
                    new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true), baseName, null,
                    Object.class.getName(), EndpointInvoker.class.getName())) {
                MethodCreator mc = classCreator.getMethodCreator("invoke", Object.class, Object.class, Object[].class);
                ResultHandle[] args = new ResultHandle[method.getParameters().length];
                ResultHandle array = mc.getMethodParam(1);
                for (int i = 0; i < method.getParameters().length; ++i) {
                    args[i] = mc.readArrayValue(array, i);
                }
                ResultHandle res;
                if (Modifier.isInterface(currentClassInfo.flags())) {
                    res = mc.invokeInterfaceMethod(info, mc.getMethodParam(0), args);
                } else {
                    res = mc.invokeVirtualMethod(info, mc.getMethodParam(0), args);
                }
                if (info.returnType().kind() == Type.Kind.VOID) {
                    mc.returnValue(mc.loadNull());
                } else {
                    mc.returnValue(res);
                }
            }
            method.setInvoker(recorder.invoker(baseName));
            return method;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process method " + info.declaringClass().name() + "#" + info.toString(), e);
        }
    }

    private static Supplier<ParameterConverter> extractConverter(String elementType, IndexView indexView,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            Map<String, String> existingConverters, MethodInfo methodInfo) {
        if (elementType.equals(String.class.getName())) {
            return null;
        } else if (existingConverters.containsKey(elementType)) {
            return new GeneratedParameterConverter().setClassName(existingConverters.get(elementType));
        }
        MethodDescriptor fromString = null;
        MethodDescriptor valueOf = null;
        MethodInfo stringCtor = null;
        String prim = primitiveTypes.get(elementType);
        String prefix = "";
        if (prim != null) {
            elementType = prim;
            valueOf = MethodDescriptor.ofMethod(elementType, "valueOf", elementType, String.class);
            prefix = "io.quarkus.generated.";
        } else {
            ClassInfo type = indexView.getClassByName(DotName.createSimple(elementType));
            if (type == null) {
                //todo: should we fall back to reflection here?
                throw new RuntimeException("Unknown parameter type " + elementType);
            }
            for (MethodInfo i : type.methods()) {
                if (i.parameters().size() == 1) {
                    if (i.parameters().get(0).name().equals(STRING)) {
                        if (i.name().equals("<init>")) {
                            stringCtor = i;
                        } else if (i.name().equals("valueOf")) {
                            valueOf = MethodDescriptor.of(i);
                        } else if (i.name().equals("fromString")) {
                            fromString = MethodDescriptor.of(i);
                        }
                    }
                }
            }
            if (type.isEnum()) {
                //spec weirdness, enums order is different
                if (fromString != null) {
                    valueOf = null;
                }
            }
        }

        String baseName = prefix + elementType + "$quarkusrestparamConverter$";
        try (ClassCreator classCreator = new ClassCreator(
                new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true), baseName, null,
                Object.class.getName(), ParameterConverter.class.getName())) {
            MethodCreator mc = classCreator.getMethodCreator("convert", Object.class, Object.class);
            if (stringCtor != null) {
                ResultHandle ret = mc.newInstance(stringCtor, mc.getMethodParam(0));
                mc.returnValue(ret);
            } else if (valueOf != null) {
                ResultHandle ret = mc.invokeStaticMethod(valueOf, mc.getMethodParam(0));
                mc.returnValue(ret);
            } else if (fromString != null) {
                ResultHandle ret = mc.invokeStaticMethod(valueOf, mc.getMethodParam(0));
                mc.returnValue(ret);
            } else {
                throw new RuntimeException("Unknown parameter type " + elementType + " on method " + methodInfo + " on class "
                        + methodInfo.declaringClass());
            }
        }
        existingConverters.put(elementType, baseName);
        return new GeneratedParameterConverter().setClassName(baseName);
    }

    private static String methodDescriptor(MethodInfo info) {
        return info.name() + ":" + AsmUtil.getDescriptor(info, s -> null);
    }

    private static boolean moreThanOne(AnnotationInstance... annotations) {
        boolean oneNonNull = false;
        for (AnnotationInstance annotation : annotations) {
            if (annotation != null) {
                if (oneNonNull)
                    return true;
                oneNonNull = true;
            }
        }
        return false;
    }

    private static String[] readStringArrayValue(AnnotationInstance annotation, String[] defaultValue) {
        String[] read = readStringArrayValue(annotation);
        if (read == null) {
            return defaultValue;
        }
        return read;
    }

    private static String[] readStringArrayValue(AnnotationInstance annotation) {
        if (annotation == null) {
            return null;
        }
        return annotation.value().asStringArray();
    }

    private static String annotationToMethod(DotName httpMethod) {
        if (httpMethod == null) {
            return null; //resource locators
        }
        if (httpMethod.equals(QuarkusRestDotNames.GET)) {
            return "GET";
        } else if (httpMethod.equals(QuarkusRestDotNames.POST)) {
            return "POST";
        } else if (httpMethod.equals(QuarkusRestDotNames.HEAD)) {
            return "HEAD";
        } else if (httpMethod.equals(QuarkusRestDotNames.PUT)) {
            return "PUT";
        } else if (httpMethod.equals(QuarkusRestDotNames.DELETE)) {
            return "DELETE";
        } else if (httpMethod.equals(QuarkusRestDotNames.PATCH)) {
            return "PATCH";
        } else if (httpMethod.equals(QuarkusRestDotNames.OPTIONS)) {
            return "OPTIONS";
        }
        throw new IllegalStateException("Unknown HTTP method annotation " + httpMethod);
    }

    public static String readStringValue(AnnotationInstance annotation, String defaultValue) {
        String val = readStringValue(annotation);
        return val == null ? defaultValue : val;
    }

    public static String readStringValue(AnnotationInstance annotationInstance) {
        String classProduces = null;
        if (annotationInstance != null) {
            classProduces = annotationInstance.value().asString();
        }
        return classProduces;
    }

    private static String toClassName(Type indexType, ClassInfo currentClass, ClassInfo actualEndpointClass,
            IndexView indexView) {
        switch (indexType.kind()) {
            case CLASS:
                return indexType.asClassType().name().toString();
            case PRIMITIVE:
                return indexType.asPrimitiveType().primitive().name().toLowerCase(Locale.ENGLISH);
            case PARAMETERIZED_TYPE:
                return indexType.asParameterizedType().name().toString();
            case ARRAY:
                ArrayType arrayType = indexType.asArrayType();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < arrayType.dimensions(); ++i) {
                    sb.append("[");
                }
                sb.append(toClassName(arrayType.component(), currentClass, actualEndpointClass, indexView));
                return sb.toString();
            case TYPE_VARIABLE:
                TypeVariable typeVariable = indexType.asTypeVariable();
                if (typeVariable.bounds().isEmpty()) {
                    return Object.class.getName();
                }
                int pos = -1;
                for (int i = 0; i < currentClass.typeParameters().size(); ++i) {
                    if (currentClass.typeParameters().get(i).identifier().equals(typeVariable.identifier())) {
                        pos = i;
                        break;
                    }
                }
                if (pos != -1) {
                    List<Type> params = JandexUtil.resolveTypeParameters(actualEndpointClass.name(), currentClass.name(),
                            indexView);

                    Type resolved = params.get(pos);
                    if (resolved.kind() != Type.Kind.TYPE_VARIABLE
                            || !resolved.asTypeVariable().identifier().equals(typeVariable.identifier())) {
                        return toClassName(resolved, currentClass, actualEndpointClass, indexView);
                    }
                }
                return toClassName(typeVariable.bounds().get(0), currentClass, actualEndpointClass, indexView);
            default:
                throw new RuntimeException("Unknown parameter type " + indexType);
        }
    }

    private static String appendPath(String prefix, String suffix) {
        if (prefix == null) {
            return suffix;
        } else if (suffix == null) {
            return prefix;
        }
        if ((prefix.endsWith("/") && !suffix.startsWith("/")) ||
                (!prefix.endsWith("/") && suffix.startsWith("/"))) {
            return prefix + suffix;
        } else if (prefix.endsWith("/")) {
            return prefix.substring(0, prefix.length() - 1) + suffix;
        } else {
            return prefix + "/" + suffix;
        }
    }

    static class MethodDesc {
        AnnotationInstance httpMethod;
        AnnotationInstance path;
        boolean blocking;

    }

}
