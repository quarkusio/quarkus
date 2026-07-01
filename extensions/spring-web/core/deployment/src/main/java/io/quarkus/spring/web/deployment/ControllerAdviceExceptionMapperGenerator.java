package io.quarkus.spring.web.deployment;

import java.lang.annotation.Annotation;
import java.lang.constant.ClassDesc;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.gizmo2.Jandex2Gizmo;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.spring.web.runtime.common.ResponseEntityConverter;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

class ControllerAdviceExceptionMapperGenerator extends AbstractExceptionMapperGenerator {

    private static final DotName RESPONSE_ENTITY = DotName.createSimple("org.springframework.http.ResponseEntity");

    private static final ClassDesc CD_ANNOTATION_ARRAY = ClassDesc.of(Annotation.class.getName()).arrayType();

    // Preferred content types order for String or primitive type responses
    private static final List<String> TEXT_MEDIA_TYPES = Arrays.asList(
            MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML);
    // Preferred content types order for object type responses
    private static final List<String> OBJECT_MEDIA_TYPES = Arrays.asList(
            MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.TEXT_PLAIN);

    private final MethodInfo controllerAdviceMethod;
    private final TypesUtil typesUtil;
    private final Type returnType;
    private final List<Type> parameterTypes;
    private final String declaringClassName;

    private final Map<Type, FieldDesc> parameterTypeToField = new HashMap<>();

    private FieldDesc httpHeadersField;

    private final boolean isResteasyClassic;

    ControllerAdviceExceptionMapperGenerator(MethodInfo controllerAdviceMethod, DotName exceptionDotName,
            ClassOutput classOutput, TypesUtil typesUtil, boolean isResteasyClassic) {
        super(exceptionDotName, classOutput, isResteasyClassic);

        this.controllerAdviceMethod = controllerAdviceMethod;
        this.typesUtil = typesUtil;

        this.returnType = controllerAdviceMethod.returnType();
        this.parameterTypes = controllerAdviceMethod.parameterTypes();
        this.declaringClassName = controllerAdviceMethod.declaringClass().name().toString();
        this.isResteasyClassic = isResteasyClassic;
    }

    /**
     * We need to go through each parameter of the method of the ControllerAdvice
     * and make sure it's supported
     * The jakarta.ws.rs.ext.ExceptionMapper only has one parameter, the exception, however
     * other parameters can be obtained using @Context and therefore injected into the target method
     */
    @Override
    protected void preGenerateMethodBody(ClassCreator cc) {
        if (!isResteasyClassic) {
            return;
        }

        int notAllowedParameterIndex = -1;
        for (int i = 0; i < parameterTypes.size(); i++) {
            Type parameterType = parameterTypes.get(i);
            DotName parameterTypeDotName = parameterType.name();
            if (typesUtil.isAssignable(Exception.class, parameterTypeDotName)) {
                // do nothing since this will be handled during in generateMethodBody
            } else if (typesUtil.isAssignable(HttpServletRequest.class, parameterTypeDotName)) {
                if (parameterTypeToField.containsKey(parameterType)) {
                    throw new IllegalArgumentException("Parameter type " + parameterTypes.get(notAllowedParameterIndex).name()
                            + " is being used multiple times in method" + controllerAdviceMethod.name() + " of class"
                            + controllerAdviceMethod.declaringClass().name());
                }

                // we need to generate a field that injects the HttpServletRequest into the class
                FieldDesc httpRequestField = cc.field("httpServletRequest", fc -> {
                    fc.setType(HttpServletRequest.class);
                    fc.private_();
                    fc.addAnnotation(Context.class);
                });

                // stash the fieldCreator in a map indexed by the parameter type so we can retrieve it later
                parameterTypeToField.put(parameterType, httpRequestField);
            } else if (typesUtil.isAssignable(HttpServletResponse.class, parameterTypeDotName)) {
                if (parameterTypeToField.containsKey(parameterType)) {
                    throw new IllegalArgumentException("Parameter type " + parameterTypes.get(notAllowedParameterIndex).name()
                            + " is being used multiple times in method" + controllerAdviceMethod.name() + " of class"
                            + controllerAdviceMethod.declaringClass().name());
                }

                // we need to generate a field that injects the HttpServletResponse into the class
                FieldDesc httpResponseField = cc.field("httpServletResponse", fc -> {
                    fc.setType(HttpServletResponse.class);
                    fc.private_();
                    fc.addAnnotation(Context.class);
                });

                // stash the fieldCreator in a map indexed by the parameter type so we can retrieve it later
                parameterTypeToField.put(parameterType, httpResponseField);
            } else {
                notAllowedParameterIndex = i;
            }
        }
        if (notAllowedParameterIndex >= 0) {
            throw new IllegalArgumentException(
                    "Parameter type " + parameterTypes.get(notAllowedParameterIndex).name() + " is not supported for method"
                            + controllerAdviceMethod.name() + " of class" + controllerAdviceMethod.declaringClass().name());
        }

        createHttpHeadersField(cc);
    }

    private void createHttpHeadersField(ClassCreator classCreator) {
        httpHeadersField = classCreator.field("httpHeaders", fc -> {
            fc.setType(HttpHeaders.class);
            fc.private_();
            fc.addAnnotation(Context.class);
        });
    }

    @Override
    void generateMethodBody(BlockCreator bc, Expr thisRef, Expr exceptionParam) {
        if (isVoidType(returnType)) {
            generateVoidExceptionHandler(bc, thisRef, exceptionParam);
        } else if (isEntityType(returnType)) {
            generateResponseEntityExceptionHandler(bc, thisRef, exceptionParam);
        } else {
            generateGenericResponseExceptionHandler(bc, thisRef, exceptionParam);
        }
    }

    private void generateVoidExceptionHandler(BlockCreator bc, Expr thisRef, Expr exceptionParam) {
        invokeExceptionHandlerMethod(bc, thisRef, exceptionParam);
        int status = getAnnotationStatusOrDefault(Response.Status.NO_CONTENT.getStatusCode());
        Expr result = new ResponseBuilder(bc, status)
                .withType(getResponseContentType(bc, thisRef, TEXT_MEDIA_TYPES))
                .build();
        bc.return_(result);
    }

    private void generateResponseEntityExceptionHandler(BlockCreator bc, Expr thisRef, Expr exceptionParam) {
        Expr result = bc.invokeStatic(
                ClassMethodDesc.of(ClassDesc.of(ResponseEntityConverter.class.getName()), "toResponse",
                        ClassDesc.of(Response.class.getName()),
                        ClassDesc.of(RESPONSE_ENTITY.toString()),
                        ClassDesc.of(MediaType.class.getName())),
                invokeExceptionHandlerMethod(bc, thisRef, exceptionParam),
                getResponseContentType(bc, thisRef, getSupportedMediaTypesForType(getResponseEntityType())));

        bc.return_(result);
    }

    private Type getResponseEntityType() {
        if (isParameterizedType(returnType) && returnType.asParameterizedType().arguments().size() == 1) {
            return returnType.asParameterizedType().arguments().get(0);
        }
        return returnType;
    }

    private void generateGenericResponseExceptionHandler(BlockCreator bc, Expr thisRef, Expr exceptionParam) {
        int status = getAnnotationStatusOrDefault(Response.Status.OK.getStatusCode());
        Expr result = new ResponseBuilder(bc, status)
                .withEntity(invokeExceptionHandlerMethod(bc, thisRef, exceptionParam))
                .withType(getResponseContentType(bc, thisRef, getSupportedMediaTypesForType(returnType)))
                .build();

        bc.return_(result);
    }

    private List<String> getSupportedMediaTypesForType(Type type) {
        if (isStringType(type) || isPrimitiveType(type)) {
            return TEXT_MEDIA_TYPES;
        }

        return OBJECT_MEDIA_TYPES;
    }

    private Expr getResponseContentType(BlockCreator bc, Expr thisRef, List<String> supportedMediaTypeStrings) {
        Expr[] supportedMediaTypes = supportedMediaTypeStrings.stream()
                .map(Const::of)
                .toArray(Expr[]::new);

        String responseContentTypeResolverClassName = isResteasyClassic
                ? "io.quarkus.spring.web.resteasy.classic.runtime.ResteasyClassicResponseContentTypeResolver"
                : "io.quarkus.spring.web.resteasy.reactive.runtime.ResteasyReactiveResponseContentTypeResolver";
        ClassDesc resolverClassDesc = ClassDesc.of(responseContentTypeResolverClassName);
        LocalVar contentTypeResolver = bc.localVar("contentTypeResolver",
                bc.new_(ConstructorDesc.of(resolverClassDesc)));

        ClassMethodDesc resolveMethod = ClassMethodDesc.of(resolverClassDesc, "resolve",
                MediaType.class, HttpHeaders.class, String[].class);

        if (isResteasyClassic) {
            return bc.invokeVirtual(
                    resolveMethod,
                    contentTypeResolver,
                    bc.get(thisRef.field(httpHeadersField)),
                    bc.newArray(String.class, supportedMediaTypes));
        }
        return bc.invokeVirtual(
                resolveMethod,
                contentTypeResolver,
                getBeanFromArc(bc, HttpHeaders.class.getName()),
                bc.newArray(String.class, supportedMediaTypes));
    }

    private Expr invokeExceptionHandlerMethod(BlockCreator bc, Expr thisRef, Expr exceptionParam) {
        String returnTypeClassName = isVoidType(returnType) ? void.class.getName() : returnType.name().toString();
        ClassDesc returnTypeClassDesc = isVoidType(returnType)
                ? ClassDesc.ofDescriptor("V")
                : ClassDesc.of(returnTypeClassName);
        ClassDesc declaringClassDesc = ClassDesc.of(declaringClassName);

        if (parameterTypes.isEmpty()) {
            return bc.invokeVirtual(
                    ClassMethodDesc.of(declaringClassDesc, controllerAdviceMethod.name(), returnTypeClassDesc),
                    controllerAdviceInstance(bc));
        }

        ClassDesc[] parameterTypeClassDescs = new ClassDesc[parameterTypes.size()];
        LocalVar[] parameterTypeHandles = new LocalVar[parameterTypes.size()];
        if (isResteasyClassic) {
            for (int i = 0; i < parameterTypes.size(); i++) {
                Type parameterType = parameterTypes.get(i);
                parameterTypeClassDescs[i] = Jandex2Gizmo.classDescOf(parameterType);
                if (typesUtil.isAssignable(Exception.class, parameterType.name())) {
                    parameterTypeHandles[i] = bc.localVar("param" + i, exceptionParam);
                } else {
                    parameterTypeHandles[i] = bc.localVar("param" + i,
                            bc.get(thisRef.field(parameterTypeToField.get(parameterType))));
                }
            }
        } else {
            for (int i = 0; i < parameterTypes.size(); i++) {
                Type parameterType = parameterTypes.get(i);
                parameterTypeClassDescs[i] = Jandex2Gizmo.classDescOf(parameterType);
                if (typesUtil.isAssignable(Exception.class, parameterType.name())) {
                    parameterTypeHandles[i] = bc.localVar("param" + i, exceptionParam);
                } else if (typesUtil.isAssignable(UriInfo.class, parameterType.name())) {
                    parameterTypeHandles[i] = bc.localVar("param" + i, getBeanFromArc(bc, UriInfo.class.getName()));
                } else if (typesUtil.isAssignable(Request.class, parameterType.name())) {
                    parameterTypeHandles[i] = bc.localVar("param" + i, getBeanFromArc(bc, Request.class.getName()));
                } else if (typesUtil.isAssignable(HttpServerRequest.class, parameterType.name())) {
                    parameterTypeHandles[i] = bc.localVar("param" + i,
                            getBeanFromArc(bc, HttpServerRequest.class.getName()));
                } else if (typesUtil.isAssignable(HttpServerResponse.class, parameterType.name())) {
                    LocalVar requestHandle = bc.localVar("request",
                            getBeanFromArc(bc, HttpServerRequest.class.getName()));
                    parameterTypeHandles[i] = bc.localVar("param" + i, bc.invokeInterface(
                            MethodDesc.of(HttpServerRequest.class,
                                    "response",
                                    HttpServerResponse.class),
                            requestHandle));
                } else {
                    throw new IllegalArgumentException(
                            "Parameter type '" + parameterType.name() + "' is not supported for method '"
                                    + controllerAdviceMethod.name() + "' of class '"
                                    + controllerAdviceMethod.declaringClass().name()
                                    + "'");
                }
            }
        }

        return bc.invokeVirtual(
                ClassMethodDesc.of(declaringClassDesc, controllerAdviceMethod.name(), returnTypeClassDesc,
                        parameterTypeClassDescs),
                controllerAdviceInstance(bc), parameterTypeHandles);
    }

    private LocalVar controllerAdviceInstance(BlockCreator bc) {
        ClassDesc declaringClassDesc = ClassDesc.of(declaringClassName);
        if (isResteasyClassic) {
            LocalVar controllerAdviceClass = bc.localVar("controllerAdviceClass",
                    bc.classForName(Const.of(declaringClassName)));
            LocalVar container = bc.localVar("container",
                    bc.invokeStatic(MethodDesc.of(Arc.class, "container", ArcContainer.class)));
            LocalVar instance = bc.localVar("instance", bc.invokeInterface(
                    MethodDesc.of(ArcContainer.class, "instance", InstanceHandle.class, Class.class,
                            Annotation[].class),
                    container, controllerAdviceClass, Const.ofNull(CD_ANNOTATION_ARRAY)));
            Expr bean = bc.invokeInterface(
                    MethodDesc.of(InstanceHandle.class, "get", Object.class),
                    instance);
            return bc.localVar("controllerAdvice", bc.cast(bean, declaringClassDesc));
        } else {
            return bc.localVar("controllerAdvice", bc.cast(getBeanFromArc(bc, declaringClassName), declaringClassDesc));
        }
    }

    private Expr getBeanFromArc(BlockCreator bc, String beanClassName) {
        LocalVar container = bc.localVar("container",
                bc.invokeStatic(MethodDesc.of(Arc.class, "container", ArcContainer.class)));
        LocalVar instance = bc.localVar("instance", bc.invokeInterface(
                MethodDesc.of(ArcContainer.class, "instance", InstanceHandle.class, Class.class,
                        Annotation[].class),
                container, bc.classForName(Const.of(beanClassName)), Const.ofNull(CD_ANNOTATION_ARRAY)));
        return bc.invokeInterface(
                MethodDesc.of(InstanceHandle.class, "get", Object.class),
                instance);
    }

    private int getAnnotationStatusOrDefault(int defaultValue) {
        AnnotationInstance annotation = controllerAdviceMethod.annotation(RESPONSE_STATUS);
        if (annotation == null) {
            return defaultValue;
        }

        return getHttpStatusFromAnnotation(annotation);
    }

    private boolean isVoidType(Type type) {
        return Type.Kind.VOID.equals(type.kind());
    }

    private boolean isPrimitiveType(Type type) {
        return Type.Kind.PRIMITIVE.equals(type.kind());
    }

    private boolean isStringType(Type type) {
        return DotName.createSimple(String.class.getName()).equals(type.name());
    }

    private boolean isEntityType(Type type) {
        return RESPONSE_ENTITY.equals(type.name());
    }

    private boolean isParameterizedType(Type type) {
        return Type.Kind.PARAMETERIZED_TYPE.equals(type.kind());
    }
}
