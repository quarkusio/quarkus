package org.jboss.resteasy.reactive.server.core.startup;

import static org.jboss.resteasy.reactive.common.util.DeploymentUtils.loadClass;
import static org.jboss.resteasy.reactive.common.util.types.Types.getEffectiveReturnType;
import static org.jboss.resteasy.reactive.common.util.types.Types.getRawType;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyWriter;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.ResteasyReactiveConfig;
import org.jboss.resteasy.reactive.common.model.MethodParameter;
import org.jboss.resteasy.reactive.common.model.ParameterType;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.common.types.AllWriteableMarker;
import org.jboss.resteasy.reactive.common.util.MediaTypeHelper;
import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;
import org.jboss.resteasy.reactive.common.util.ServerMediaType;
import org.jboss.resteasy.reactive.common.util.types.TypeSignatureParser;
import org.jboss.resteasy.reactive.common.util.types.Types;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.server.core.DeploymentInfo;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;
import org.jboss.resteasy.reactive.server.core.parameters.AsyncResponseExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.BodyParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.ContextParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.CookieParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.FormParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.HeaderParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.InjectParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.LocatableResourcePathParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.MatrixParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.MultipartDataInputExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.MultipartFormParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.NullParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.PathParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.QueryParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.RuntimeResolvedConverter;
import org.jboss.resteasy.reactive.server.core.serialization.DynamicEntityWriter;
import org.jboss.resteasy.reactive.server.core.serialization.FixedEntityWriter;
import org.jboss.resteasy.reactive.server.core.serialization.FixedEntityWriterArray;
import org.jboss.resteasy.reactive.server.handlers.AbortChainHandler;
import org.jboss.resteasy.reactive.server.handlers.BlockingHandler;
import org.jboss.resteasy.reactive.server.handlers.ExceptionHandler;
import org.jboss.resteasy.reactive.server.handlers.FixedProducesHandler;
import org.jboss.resteasy.reactive.server.handlers.FormBodyHandler;
import org.jboss.resteasy.reactive.server.handlers.InputHandler;
import org.jboss.resteasy.reactive.server.handlers.InstanceHandler;
import org.jboss.resteasy.reactive.server.handlers.InvocationHandler;
import org.jboss.resteasy.reactive.server.handlers.NonBlockingHandler;
import org.jboss.resteasy.reactive.server.handlers.ParameterHandler;
import org.jboss.resteasy.reactive.server.handlers.PerRequestInstanceHandler;
import org.jboss.resteasy.reactive.server.handlers.PublisherResponseHandler;
import org.jboss.resteasy.reactive.server.handlers.RequestDeserializeHandler;
import org.jboss.resteasy.reactive.server.handlers.ResourceLocatorHandler;
import org.jboss.resteasy.reactive.server.handlers.ResourceRequestFilterHandler;
import org.jboss.resteasy.reactive.server.handlers.ResponseHandler;
import org.jboss.resteasy.reactive.server.handlers.ResponseWriterHandler;
import org.jboss.resteasy.reactive.server.handlers.SseResponseWriterHandler;
import org.jboss.resteasy.reactive.server.handlers.VariableProducesHandler;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.mapping.URITemplate;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;
import org.jboss.resteasy.reactive.server.model.ServerMethodParameter;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.server.util.ScoreSystem;
import org.jboss.resteasy.reactive.spi.BeanFactory;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class RuntimeResourceDeployment {

    private static final ServerRestHandler[] EMPTY_REST_HANDLER_ARRAY = new ServerRestHandler[0];
    @SuppressWarnings("rawtypes")
    private static final MessageBodyWriter[] EMPTY_MESSAGE_BODY_WRITERS = new MessageBodyWriter[0];

    private static final int HANDLERS_CAPACITY = 10;

    private static final Logger log = Logger.getLogger(RuntimeResourceDeployment.class);

    private final DeploymentInfo info;
    private final ServerSerialisers serialisers;
    private final ResteasyReactiveConfig resteasyReactiveConfig;
    private final Supplier<Executor> executorSupplier;
    private final Supplier<Executor> virtualExecutorSupplier;
    private final RuntimeInterceptorDeployment runtimeInterceptorDeployment;
    private final DynamicEntityWriter dynamicEntityWriter;
    private final ResourceLocatorHandler resourceLocatorHandler;
    /**
     * If the runtime will always default to blocking (e.g. Servlet)
     */
    private final boolean defaultBlocking;
    private final BlockingHandler blockingHandler;
    private final BlockingHandler blockingHandlerVirtualThread;
    private final ResponseWriterHandler responseWriterHandler;

    public RuntimeResourceDeployment(DeploymentInfo info, Supplier<Executor> executorSupplier,
            Supplier<Executor> virtualExecutorSupplier,
            RuntimeInterceptorDeployment runtimeInterceptorDeployment, DynamicEntityWriter dynamicEntityWriter,
            ResourceLocatorHandler resourceLocatorHandler, boolean defaultBlocking) {
        this.info = info;
        this.serialisers = info.getSerialisers();
        this.resteasyReactiveConfig = info.getResteasyReactiveConfig();
        this.executorSupplier = executorSupplier;
        this.virtualExecutorSupplier = virtualExecutorSupplier;
        this.runtimeInterceptorDeployment = runtimeInterceptorDeployment;
        this.dynamicEntityWriter = dynamicEntityWriter;
        this.resourceLocatorHandler = resourceLocatorHandler;
        this.defaultBlocking = defaultBlocking;
        this.blockingHandler = new BlockingHandler(executorSupplier);
        this.blockingHandlerVirtualThread = new BlockingHandler(virtualExecutorSupplier);
        this.responseWriterHandler = new ResponseWriterHandler(dynamicEntityWriter);
    }

    public RuntimeResource buildResourceMethod(ResourceClass clazz,
            ServerResourceMethod method, boolean locatableResource, URITemplate classPathTemplate, DeploymentInfo info) {
        URITemplate methodPathTemplate = new URITemplate(method.getPath(), method.isResourceLocator());
        MultivaluedMap<ScoreSystem.Category, ScoreSystem.Diagnostic> score = new QuarkusMultivaluedHashMap<>();

        Map<String, Integer> pathParameterIndexes = buildParamIndexMap(classPathTemplate, methodPathTemplate);
        MediaType streamElementType = null;
        if (method.getStreamElementType() != null) {
            streamElementType = MediaType.valueOf(method.getStreamElementType());
        }
        List<MediaType> consumesMediaTypes;
        if (method.getConsumes() == null) {
            consumesMediaTypes = Collections.emptyList();
        } else {
            consumesMediaTypes = new ArrayList<>(method.getConsumes().length);
            for (String s : method.getConsumes()) {
                consumesMediaTypes.add(MediaType.valueOf(s));
            }
        }

        Class<Object> resourceClass = loadClass(clazz.getClassName());
        Class<?>[] parameterDeclaredTypes = new Class[method.getParameters().length];
        Class<?>[] parameterDeclaredUnresolvedTypes = new Class[method.getParameters().length];
        for (int i = 0; i < method.getParameters().length; ++i) {
            MethodParameter parameter = method.getParameters()[i];
            String declaredType = parameter.declaredType;
            String declaredUnresolvedType = parameter.declaredUnresolvedType;
            parameterDeclaredTypes[i] = loadClass(declaredType);
            parameterDeclaredUnresolvedTypes[i] = parameterDeclaredTypes[i];
            if (!declaredType.equals(declaredUnresolvedType)) {
                parameterDeclaredUnresolvedTypes[i] = loadClass(declaredUnresolvedType);
            }
        }

        Annotation[] resourceClassAnnotations = resourceClass.getAnnotations();
        Set<String> classAnnotationNames;
        if (resourceClassAnnotations.length == 0) {
            classAnnotationNames = Collections.emptySet();
        } else {
            classAnnotationNames = new HashSet<>(resourceClassAnnotations.length);
            for (Annotation annotation : resourceClassAnnotations) {
                classAnnotationNames.add(annotation.annotationType().getName());
            }
        }

        ResteasyReactiveResourceInfo lazyMethod = new ResteasyReactiveResourceInfo(method.getName(), resourceClass,
                parameterDeclaredUnresolvedTypes, classAnnotationNames, method.getMethodAnnotationNames(),
                !defaultBlocking && !method.isBlocking());

        RuntimeInterceptorDeployment.MethodInterceptorContext interceptorDeployment = runtimeInterceptorDeployment
                .forMethod(method, lazyMethod);

        //setup reader and writer interceptors first
        ServerRestHandler interceptorHandler = interceptorDeployment.setupInterceptorHandler();
        //we want interceptors in the abort handler chain
        List<ServerRestHandler> abortHandlingChain = new ArrayList<>(
                3 + (interceptorHandler != null ? 1 : 0) + (info.getPreExceptionMapperHandler() != null ? 1 : 0));

        List<ServerRestHandler> handlers = new ArrayList<>(HANDLERS_CAPACITY);
        // we add null as the first item to make sure that subsequent items are added in the proper positions
        // and that the items don't need to shifted when at the end of the method we set the
        // first item
        handlers.add(null);
        addHandlers(handlers, clazz, method, info, HandlerChainCustomizer.Phase.AFTER_MATCH);
        if (interceptorHandler != null) {
            handlers.add(interceptorHandler);
        }

        // when a method is blocking, we also want all the request filters to run on the worker thread
        // because they can potentially set thread local variables
        // we don't need to run this for Servlet and other runtimes that default to blocking
        Optional<Integer> blockingHandlerIndex = Optional.empty();
        if (!defaultBlocking) {
            if (method.isBlocking()) {
                if (method.isRunOnVirtualThread()) {
                    handlers.add(blockingHandlerVirtualThread);
                    score.add(ScoreSystem.Category.Execution, ScoreSystem.Diagnostic.ExecutionVirtualThread);
                } else {
                    handlers.add(blockingHandler);
                    score.add(ScoreSystem.Category.Execution, ScoreSystem.Diagnostic.ExecutionBlocking);
                }
                blockingHandlerIndex = Optional.of(handlers.size() - 1);
            } else {
                if (method.isRunOnVirtualThread()) {
                    //should not happen
                    log.error("a method was both non-blocking and @RunOnVirtualThread, it is now considered " +
                            "@RunOnVirtual and blocking");
                    handlers.add(blockingHandlerVirtualThread);
                    score.add(ScoreSystem.Category.Execution, ScoreSystem.Diagnostic.ExecutionVirtualThread);
                } else {
                    handlers.add(NonBlockingHandler.INSTANCE);
                    score.add(ScoreSystem.Category.Execution, ScoreSystem.Diagnostic.ExecutionNonBlocking);
                }
            }
        }

        // special case for AsyncFile which can't do async IO and handle interceptors
        if (method.getReturnType().equals("Lio/vertx/core/file/AsyncFile;")
                && interceptorDeployment.hasWriterInterceptors()) {
            throw new RuntimeException(
                    "Endpoints that return an AsyncFile cannot have any WriterInterceptor set");
        }

        //spec doesn't seem to test this, but RESTEasy does not run request filters for both root and sub resources (which makes sense)
        //so only run request filters for methods that are leaf resources - i.e. have a HTTP method annotation so we ensure only one will run
        boolean hasWithFormReadRequestFilters = false;
        if (method.getHttpMethod() != null) {
            List<ResourceRequestFilterHandler> containerRequestFilterHandlers = interceptorDeployment
                    .setupRequestFilterHandler();
            if (blockingHandlerIndex.isPresent()) {
                int initialIndex = blockingHandlerIndex.get();
                for (int i = 0; i < containerRequestFilterHandlers.size(); i++) {
                    ResourceRequestFilterHandler handler = containerRequestFilterHandlers.get(i);
                    if (handler.isNonBlockingRequired()) {
                        // the non-blocking handlers are added in the order we have already determined, but they need to
                        // be added before the blocking handler
                        handlers.add(initialIndex + i, handler);
                    } else {
                        handlers.add(handler);
                    }
                }
            } else {
                handlers.addAll(containerRequestFilterHandlers);
            }
            for (ResourceRequestFilterHandler handler : containerRequestFilterHandlers) {
                if (handler.isWithFormRead()) {
                    hasWithFormReadRequestFilters = true;
                    break;
                }
            }
        }

        // some parameters need the body to be read
        MethodParameter[] parameters = method.getParameters();
        // body can only be in a parameter
        MethodParameter bodyParameter = null;
        int bodyParameterIndex = -1;
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter param = parameters[i];
            if (param.parameterType == ParameterType.BODY) {
                bodyParameter = param;
                bodyParameterIndex = i;
                break;
            }
        }
        // form params can be everywhere (field, beanparam, param)
        boolean checkWithFormReadRequestFilters = false;
        boolean inputHandlerEngaged = false;
        if (method.isFormParamRequired() || hasWithFormReadRequestFilters) {
            // read the body as multipart in one go
            handlers.add(new FormBodyHandler(bodyParameter != null, executorSupplier, method.getFileFormNames()));
            checkWithFormReadRequestFilters = true;
        }
        if (bodyParameter != null) {
            if (!defaultBlocking) {
                if (!method.isBlocking()) {
                    // allow the body to be read by chunks
                    handlers.add(new InputHandler(resteasyReactiveConfig.getInputBufferSize(), executorSupplier));
                    checkWithFormReadRequestFilters = true;
                    inputHandlerEngaged = true;
                }
            }
        }
        if (checkWithFormReadRequestFilters && hasWithFormReadRequestFilters) {
            // we need to remove the corresponding filters from the handlers list and add them to its end in the same order
            List<ServerRestHandler> readBodyRequestFilters = new ArrayList<>(1);
            for (int i = handlers.size() - 2; i >= 0; i--) {
                var serverRestHandler = handlers.get(i);
                if (serverRestHandler instanceof ResourceRequestFilterHandler) {
                    ResourceRequestFilterHandler resourceRequestFilterHandler = (ResourceRequestFilterHandler) serverRestHandler;
                    if (resourceRequestFilterHandler.isWithFormRead()) {
                        readBodyRequestFilters.add(handlers.remove(i));
                    }
                }
            }
            handlers.addAll(readBodyRequestFilters);
        }

        // if we need the body, let's deserialize it
        if (bodyParameter != null) {
            Class<Object> typeClass = loadClass(bodyParameter.declaredType);
            Type genericType = typeClass;
            if (!bodyParameter.type.equals(bodyParameter.declaredType)) {
                // we only need to parse the signature and create generic type when the declared type differs from the type
                genericType = TypeSignatureParser.parse(bodyParameter.signature);
            }
            handlers.add(new RequestDeserializeHandler(typeClass, genericType, consumesMediaTypes, serialisers,
                    bodyParameterIndex));
            if (inputHandlerEngaged) {
                handlers.add(NonBlockingHandler.INSTANCE);
            }

        }

        // given that we may inject form params in the endpoint we need to make sure we read the body before
        // we create/inject our endpoint
        ServerRestHandler instanceHandler = null;
        if (!locatableResource) {
            if (clazz.isPerRequestResource()) {
                instanceHandler = new PerRequestInstanceHandler(clazz.getFactory(), info.getClientProxyUnwrapper());
                score.add(ScoreSystem.Category.Resource, ScoreSystem.Diagnostic.ResourcePerRequest);
            } else {
                instanceHandler = new InstanceHandler(clazz.getFactory());
                score.add(ScoreSystem.Category.Resource, ScoreSystem.Diagnostic.ResourceSingleton);
            }
            handlers.add(instanceHandler);
        }

        addHandlers(handlers, clazz, method, info, HandlerChainCustomizer.Phase.RESOLVE_METHOD_PARAMETERS);
        for (int i = 0; i < parameters.length; i++) {
            ServerMethodParameter param = (ServerMethodParameter) parameters[i];
            ParameterExtractor extractor = parameterExtractor(pathParameterIndexes, locatableResource, param);
            ParameterConverter converter = null;
            ParamConverterProviders paramConverterProviders = info.getParamConverterProviders();
            boolean userProviderConvertersExist = !paramConverterProviders.getParamConverterProviders().isEmpty();
            if (param.converter != null) {
                converter = param.converter.get();
                if (userProviderConvertersExist) {
                    Method javaMethod = lazyMethod.getMethod();
                    // Workaround our lack of support for generic params by not doing this init if there are not runtime
                    // param converter providers
                    Class<?>[] parameterTypes = javaMethod.getParameterTypes();
                    Type[] genericParameterTypes = javaMethod.getGenericParameterTypes();
                    Annotation[][] parameterAnnotations = javaMethod.getParameterAnnotations();
                    smartInitParameterConverter(i, converter, paramConverterProviders, parameterTypes, genericParameterTypes,
                            parameterAnnotations);

                    // make sure we give the user provided resolvers the chance to convert
                    converter = new RuntimeResolvedConverter(converter);
                    converter.init(paramConverterProviders, parameterTypes[i], genericParameterTypes[i],
                            parameterAnnotations[i]);
                }
            }

            handlers.add(new ParameterHandler(i, param.getDefaultValue(), extractor,
                    converter, param.parameterType,
                    param.isObtainedAsCollection(), param.isOptional()));
        }
        addHandlers(handlers, clazz, method, info, HandlerChainCustomizer.Phase.BEFORE_METHOD_INVOKE);
        EndpointInvoker invoker = method.getInvoker().get();
        ServerRestHandler alternate = alternateInvoker(method, invoker);
        if (alternate != null) {
            handlers.add(alternate);
        } else {
            handlers.add(new InvocationHandler(invoker));
        }
        boolean afterMethodInvokeHandlersAdded = addHandlers(handlers, clazz, method, info,
                HandlerChainCustomizer.Phase.AFTER_METHOD_INVOKE);
        boolean afterMethodInvokeHandlersSecondRoundAdded = addHandlers(handlers, clazz, method, info,
                HandlerChainCustomizer.Phase.AFTER_METHOD_INVOKE_SECOND_ROUND);
        if (afterMethodInvokeHandlersAdded || afterMethodInvokeHandlersSecondRoundAdded) {
            addStreamingResponseCustomizers(method, handlers);
        }

        Type returnType = TypeSignatureParser.parse(method.getReturnType());
        Type effectiveReturnType = getEffectiveReturnType(returnType);
        Class<?> rawEffectiveReturnType = getRawType(effectiveReturnType);

        ServerMediaType serverMediaType = null;
        if (method.getProduces() != null && method.getProduces().length > 0) {
            // when negotiating a media type, we want to use the proper subtype to locate a ResourceWriter,
            // hence the 'true' for 'useSuffix'
            serverMediaType = new ServerMediaType(ServerMediaType.mediaTypesFromArray(method.getProduces()),
                    StandardCharsets.UTF_8.name(), false);
        }
        if (method.getHttpMethod() == null) {
            //this is a resource locator method
            handlers.add(resourceLocatorHandler);
        } else if (!Response.class.isAssignableFrom(rawEffectiveReturnType)) {
            //try and statically determine the media type and response writer
            //we can't do this for all cases, but we can do it for the most common ones
            //in practice this should work for the majority of endpoints
            if (method.getProduces() != null && method.getProduces().length > 0) {
                //the method can only produce a single content type, which is the most common case
                if (method.getProduces().length == 1) {
                    MediaType mediaType = MediaType.valueOf(method.getProduces()[0]);
                    //its a wildcard type, makes it hard to determine statically
                    if (mediaType.isWildcardType() || mediaType.isWildcardSubtype()) {
                        handlers.add(new VariableProducesHandler(serverMediaType, serialisers));
                        score.add(ScoreSystem.Category.Writer, ScoreSystem.Diagnostic.WriterRunTime);
                    } else if (isNotVoid(rawEffectiveReturnType)) {
                        List<MessageBodyWriter<?>> buildTimeWriters = serialisers.findBuildTimeWriters(rawEffectiveReturnType,
                                RuntimeType.SERVER, MediaTypeHelper.toListOfMediaType(method.getProduces()));
                        if (buildTimeWriters == null) {
                            //if this is null this means that the type cannot be resolved at build time
                            //this happens when the method returns a generic type (e.g. Object), so there
                            //are more specific mappers that could be invoked depending on the actual return value
                            handlers.add(new FixedProducesHandler(mediaType, dynamicEntityWriter));
                            score.add(ScoreSystem.Category.Writer, ScoreSystem.Diagnostic.WriterRunTime);
                        } else if (buildTimeWriters.isEmpty()) {
                            //we could not find any writers that can write a response to this endpoint
                            log.warn("Cannot find any combination of response writers for the method " + clazz.getClassName()
                                    + "#" + method.getName() + "(" + Arrays.toString(method.getParameters()) + ")");
                            handlers.add(new VariableProducesHandler(serverMediaType, serialisers));
                            score.add(ScoreSystem.Category.Writer, ScoreSystem.Diagnostic.WriterRunTime);
                        } else if (isSingleEffectiveWriter(buildTimeWriters)) {
                            MessageBodyWriter<?> writer = buildTimeWriters.get(0);
                            handlers.add(new FixedProducesHandler(mediaType, new FixedEntityWriter(
                                    writer, serialisers)));
                            if (writer instanceof ServerMessageBodyWriter)
                                score.add(ScoreSystem.Category.Writer,
                                        ScoreSystem.Diagnostic.WriterBuildTimeDirect(writer));
                            else
                                score.add(ScoreSystem.Category.Writer,
                                        ScoreSystem.Diagnostic.WriterBuildTime(writer));
                        } else {
                            //multiple writers, we try them in the proper order which had already been created
                            handlers.add(new FixedProducesHandler(mediaType,
                                    new FixedEntityWriterArray(buildTimeWriters.toArray(EMPTY_MESSAGE_BODY_WRITERS),
                                            serialisers)));
                            score.add(ScoreSystem.Category.Writer,
                                    ScoreSystem.Diagnostic.WriterBuildTimeMultiple(buildTimeWriters));
                        }
                    } else {
                        score.add(ScoreSystem.Category.Writer, ScoreSystem.Diagnostic.WriterNotRequired);
                    }
                } else {
                    //there are multiple possibilities
                    //we could optimise this more in future
                    handlers.add(new VariableProducesHandler(serverMediaType, serialisers));
                    score.add(ScoreSystem.Category.Writer, ScoreSystem.Diagnostic.WriterRunTime);
                }
            } else {
                score.add(ScoreSystem.Category.Writer, isNotVoid(rawEffectiveReturnType) ? ScoreSystem.Diagnostic.WriterRunTime
                        : ScoreSystem.Diagnostic.WriterNotRequired);
            }
        } else {
            score.add(ScoreSystem.Category.Writer, ScoreSystem.Diagnostic.WriterRunTime);
        }

        //the response filter handlers, they need to be added to both the abort and
        //normal chains. At the moment this only has one handler added to it but
        //in future there will be one per filter
        List<ServerRestHandler> responseFilterHandlers;
        if (method.isSse()) {
            handlers.add(SseResponseWriterHandler.INSTANCE);
            responseFilterHandlers = Collections.emptyList();
        } else {
            addResponseHandler(method, handlers);
            addHandlers(handlers, clazz, method, info, HandlerChainCustomizer.Phase.AFTER_RESPONSE_CREATED);
            responseFilterHandlers = new ArrayList<>(interceptorDeployment.setupResponseFilterHandler());
            handlers.addAll(responseFilterHandlers);
            handlers.add(responseWriterHandler);
        }
        if (!clazz.resourceExceptionMapper().isEmpty() && (instanceHandler != null)) {
            // when class level exception mapper are used, we need to make sure that an instance of resource class exists
            // so we can invoke it
            abortHandlingChain.add(instanceHandler);
        }
        if (info.getPreExceptionMapperHandler() != null) {
            abortHandlingChain.add(info.getPreExceptionMapperHandler());
        }
        abortHandlingChain.add(ExceptionHandler.INSTANCE);
        abortHandlingChain.add(ResponseHandler.NO_CUSTOMIZER_INSTANCE);
        abortHandlingChain.addAll(responseFilterHandlers);
        abortHandlingChain.add(responseWriterHandler);

        handlers.set(0, new AbortChainHandler(abortHandlingChain.toArray(EMPTY_REST_HANDLER_ARRAY)));

        return new RuntimeResource(method.getHttpMethod(), methodPathTemplate,
                classPathTemplate,
                method.getProduces() == null ? null : serverMediaType,
                consumesMediaTypes, invoker,
                clazz.getFactory(), handlers.toArray(EMPTY_REST_HANDLER_ARRAY), method.getName(), parameterDeclaredTypes,
                effectiveReturnType, method.isBlocking(), method.isRunOnVirtualThread(), resourceClass,
                lazyMethod,
                pathParameterIndexes, info.isDevelopmentMode() ? score : null, streamElementType,
                clazz.resourceExceptionMapper());
    }

    /**
     * This method takes into account the case where a parameter is for example List<UUID>
     * and we want to allow users to be able to use their implementation of
     * ParamConverter<UUID>.
     */
    private static void smartInitParameterConverter(int i, ParameterConverter quarkusConverter,
            ParamConverterProviders paramConverterProviders,
            Class<?>[] parameterTypes, Type[] genericParameterTypes,
            Annotation[][] parameterAnnotations) {
        if (quarkusConverter.isForSingleObjectContainer()) {

            if (genericParameterTypes[i] instanceof ParameterizedType) {
                Type[] genericArguments = ((ParameterizedType) genericParameterTypes[i]).getActualTypeArguments();
                if (genericArguments.length == 1) {
                    String genericTypeClassName = null;
                    Type genericType = genericArguments[0];
                    if (genericType instanceof Class) {
                        genericTypeClassName = ((Class<?>) genericType).getName();
                    } else if (genericType instanceof ParameterizedType) {
                        genericTypeClassName = ((ParameterizedType) genericType).getRawType().getTypeName();
                    } else if (genericType instanceof WildcardType) {
                        WildcardType genericTypeWildcardType = (WildcardType) genericType;
                        Type[] upperBounds = genericTypeWildcardType.getUpperBounds();
                        Type[] lowerBounds = genericTypeWildcardType.getLowerBounds();
                        if ((lowerBounds.length == 0) && (upperBounds.length == 1)) {
                            Type genericTypeUpperBoundType = upperBounds[0];
                            if (genericTypeUpperBoundType instanceof Class) {
                                genericTypeClassName = ((Class<?>) genericTypeUpperBoundType).getName();
                            }
                        }
                    }
                    //TODO: are there any other cases we can support?
                    if (genericTypeClassName == null) {
                        throw new IllegalArgumentException(
                                "Unable to support parameter converter with type: '" + genericType.getTypeName() + "'");
                    }
                    quarkusConverter.init(paramConverterProviders, loadClass(genericTypeClassName),
                            genericArguments[0],
                            parameterAnnotations[i]);
                    return;
                }
            }
        }

        // TODO: this is almost certainly wrong when genericParameterTypes[i] is a ParameterizedType not handle above,
        // but there is no obvious way to handle it...
        quarkusConverter.init(paramConverterProviders, parameterTypes[i], genericParameterTypes[i],
                parameterAnnotations[i]);
    }

    private static boolean isNotVoid(Class<?> rawEffectiveReturnType) {
        return rawEffectiveReturnType != Void.class
                && rawEffectiveReturnType != void.class;
    }

    private void addResponseHandler(ServerResourceMethod method, List<ServerRestHandler> handlers) {
        if (method.getHandlerChainCustomizers().isEmpty()) {
            handlers.add(ResponseHandler.NO_CUSTOMIZER_INSTANCE);
        } else {
            List<ResponseHandler.ResponseBuilderCustomizer> customizers = new ArrayList<>(
                    method.getHandlerChainCustomizers().size());
            for (int i = 0; i < method.getHandlerChainCustomizers().size(); i++) {
                ResponseHandler.ResponseBuilderCustomizer customizer = method.getHandlerChainCustomizers().get(i)
                        .successfulInvocationResponseBuilderCustomizer(method);
                if (customizer != null) {
                    customizers.add(customizer);
                }
            }
            handlers.add(new ResponseHandler(customizers));
        }
    }

    private void addStreamingResponseCustomizers(ServerResourceMethod method, List<ServerRestHandler> handlers) {
        List<PublisherResponseHandler.StreamingResponseCustomizer> customizers = new ArrayList<>(
                method.getHandlerChainCustomizers().size());
        for (int i = 0; i < method.getHandlerChainCustomizers().size(); i++) {
            PublisherResponseHandler.StreamingResponseCustomizer streamingResponseCustomizer = method
                    .getHandlerChainCustomizers().get(i)
                    .streamingResponseCustomizer(method);
            if (streamingResponseCustomizer != null) {
                customizers.add(streamingResponseCustomizer);
            }
        }
        if (!customizers.isEmpty()) {
            for (int i = 0; i < handlers.size(); i++) {
                ServerRestHandler serverRestHandler = handlers.get(i);
                if (serverRestHandler instanceof PublisherResponseHandler) {
                    ((PublisherResponseHandler) serverRestHandler).setStreamingResponseCustomizers(customizers);
                    return;
                }
            }
        }
    }

    private boolean isSingleEffectiveWriter(List<MessageBodyWriter<?>> buildTimeWriters) {
        if (buildTimeWriters.size() == 1) { // common case of single writer
            return true;
        }

        // in the case where the first Writer is an instance of AllWriteableMessageBodyWriter,
        // it doesn't matter that we have multiple writers as the first one will always be used to serialize
        return buildTimeWriters.get(0) instanceof AllWriteableMarker;
    }

    private boolean addHandlers(List<ServerRestHandler> handlers, ResourceClass clazz, ServerResourceMethod method,
            DeploymentInfo info,
            HandlerChainCustomizer.Phase phase) {
        int originalHandlersSize = handlers.size();
        for (int i = 0; i < info.getGlobalHandlerCustomizers().size(); i++) {
            handlers.addAll(info.getGlobalHandlerCustomizers().get(i).handlers(phase, clazz, method));
        }
        for (int i = 0; i < method.getHandlerChainCustomizers().size(); i++) {
            handlers.addAll(method.getHandlerChainCustomizers().get(i).handlers(phase, clazz, method));
        }
        return originalHandlersSize != handlers.size();
    }

    private ServerRestHandler alternateInvoker(ServerResourceMethod method, EndpointInvoker invoker) {
        for (int i = 0; i < method.getHandlerChainCustomizers().size(); i++) {
            ServerRestHandler ret = method.getHandlerChainCustomizers().get(i).alternateInvocationHandler(invoker);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public ParameterExtractor parameterExtractor(Map<String, Integer> pathParameterIndexes, boolean locatableResource,
            ServerMethodParameter param) {
        ParameterExtractor extractor;
        switch (param.parameterType) {
            case HEADER:
                return new HeaderParamExtractor(param.name, param.isSingle());
            case COOKIE:
                return new CookieParamExtractor(param.name, param.type);
            case FORM:
                MultipartFormParamExtractor.Type multiPartType = null;
                Class<Object> typeClass = null;
                Type genericType = null;
                if (param.type.equals(FileUpload.class.getName())) {
                    multiPartType = MultipartFormParamExtractor.Type.FileUpload;
                } else if (param.type.equals(File.class.getName())) {
                    multiPartType = MultipartFormParamExtractor.Type.File;
                } else if (param.type.equals(Path.class.getName())) {
                    multiPartType = MultipartFormParamExtractor.Type.Path;
                } else if (param.type.equals(String.class.getName())) {
                    multiPartType = MultipartFormParamExtractor.Type.String;
                } else if (param.type.equals(InputStream.class.getName())) {
                    multiPartType = MultipartFormParamExtractor.Type.InputStream;
                } else if (param.type.equals(byte[].class.getName())) {
                    multiPartType = MultipartFormParamExtractor.Type.ByteArray;
                } else if (param.mimeType != null && !param.mimeType.equals(MediaType.TEXT_PLAIN)) {
                    multiPartType = MultipartFormParamExtractor.Type.PartType;
                    // TODO: special primitive handling?
                    // FIXME: by using the element type, we're also getting converters for parameter collection types such as List/Array/Set
                    // but also others we may not want?
                    typeClass = loadClass(param.type);
                    genericType = TypeSignatureParser.parse(param.signature);
                    // strip the element type for the message body readers
                    genericType = Types.getMultipartElementType(genericType);
                }
                if (multiPartType != null) {
                    return new MultipartFormParamExtractor(param.name, param.isSingle(), multiPartType, typeClass, genericType,
                            param.mimeType, param.encoded);
                }
                // regular form
                return new FormParamExtractor(param.name, param.isSingle(), param.encoded);
            case PATH:
                Integer index = pathParameterIndexes.get(param.name);
                if (index == null) {
                    if (locatableResource) {
                        extractor = new LocatableResourcePathParamExtractor(param.name);
                    } else {
                        extractor = new NullParamExtractor();
                    }
                } else {
                    extractor = new PathParamExtractor(index, param.encoded, param.isSingle());
                }
                return extractor;
            case CONTEXT:
                return new ContextParamExtractor(param.type);
            case ASYNC_RESPONSE:
                return AsyncResponseExtractor.INSTANCE;
            case QUERY:
                extractor = new QueryParamExtractor(param.name, param.isSingle(), param.encoded, param.separator);
                return extractor;
            case BODY:
                return BodyParamExtractor.INSTANCE;
            case MATRIX:
                extractor = new MatrixParamExtractor(param.name, param.isSingle(), param.encoded);
                return extractor;
            case BEAN:
            case MULTI_PART_FORM:
                return new InjectParamExtractor((BeanFactory<Object>) info.getFactoryCreator().apply(loadClass(param.type)));
            case MULTI_PART_DATA_INPUT:
                return MultipartDataInputExtractor.INSTANCE;
            case CUSTOM:
                return param.customParameterExtractor;
            default:
                throw new RuntimeException("Unknown param type: " + param.parameterType);
        }
    }

    public Map<String, Integer> buildParamIndexMap(URITemplate classPathTemplate, URITemplate methodPathTemplate) {
        Map<String, Integer> pathParameterIndexes = new HashMap<>();
        int pathCount = 0;
        if (classPathTemplate != null) {
            for (URITemplate.TemplateComponent i : classPathTemplate.components) {
                if (i.name != null) {
                    pathParameterIndexes.put(i.name, pathCount++);
                } else if (i.names != null) {
                    for (String nm : i.names) {
                        pathParameterIndexes.put(nm, pathCount++);
                    }
                }
            }
        }
        for (URITemplate.TemplateComponent i : methodPathTemplate.components) {
            if (i.name != null) {
                pathParameterIndexes.put(i.name, pathCount++);
            } else if (i.names != null) {
                for (String nm : i.names) {
                    pathParameterIndexes.put(nm, pathCount++);
                }
            }
        }
        return pathParameterIndexes;
    }

}
