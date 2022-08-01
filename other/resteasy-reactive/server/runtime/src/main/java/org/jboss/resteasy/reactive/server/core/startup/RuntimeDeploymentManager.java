package org.jboss.resteasy.reactive.server.core.startup;

import static org.jboss.resteasy.reactive.common.util.DeploymentUtils.loadClass;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.common.model.ResourceFeature;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptor;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptors;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.DeploymentInfo;
import org.jboss.resteasy.reactive.server.core.ExceptionMapping;
import org.jboss.resteasy.reactive.server.core.RequestContextFactory;
import org.jboss.resteasy.reactive.server.core.RuntimeExceptionMapper;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;
import org.jboss.resteasy.reactive.server.core.serialization.DynamicEntityWriter;
import org.jboss.resteasy.reactive.server.handlers.ClassRoutingHandler;
import org.jboss.resteasy.reactive.server.handlers.ExceptionHandler;
import org.jboss.resteasy.reactive.server.handlers.ResourceLocatorHandler;
import org.jboss.resteasy.reactive.server.handlers.ResourceRequestFilterHandler;
import org.jboss.resteasy.reactive.server.handlers.ResponseHandler;
import org.jboss.resteasy.reactive.server.handlers.ResponseWriterHandler;
import org.jboss.resteasy.reactive.server.handlers.RestInitialHandler;
import org.jboss.resteasy.reactive.server.jaxrs.FeatureContextImpl;
import org.jboss.resteasy.reactive.server.mapping.RequestMapper;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.mapping.URITemplate;
import org.jboss.resteasy.reactive.server.model.Features;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
import org.jboss.resteasy.reactive.server.spi.RuntimeConfigurableServerRestHandler;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.spi.BeanFactory;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

public class RuntimeDeploymentManager {
    public static final ServerRestHandler[] EMPTY_REST_HANDLER_ARRAY = new ServerRestHandler[0];
    private final DeploymentInfo info;
    private final Supplier<Executor> executorSupplier;
    private final Supplier<Executor> virtualExecutorSupplier;
    private final Consumer<Closeable> closeTaskHandler;
    private final RequestContextFactory requestContextFactory;
    private final ThreadSetupAction threadSetupAction;
    private final String rootPath;

    private List<RequestMapper.RequestPath<RestInitialHandler.InitialMatch>> classMappers;

    public RuntimeDeploymentManager(DeploymentInfo info,
            Supplier<Executor> executorSupplier,
            Supplier<Executor> virtualExecutorSupplier,
            Consumer<Closeable> closeTaskHandler,
            RequestContextFactory requestContextFactory, ThreadSetupAction threadSetupAction, String rootPath) {
        this.info = info;
        this.executorSupplier = executorSupplier;
        this.virtualExecutorSupplier = virtualExecutorSupplier;
        this.closeTaskHandler = closeTaskHandler;
        this.requestContextFactory = requestContextFactory;
        this.threadSetupAction = threadSetupAction;
        this.rootPath = rootPath;
    }

    public Deployment deploy() {
        ResourceInterceptors interceptors = info.getInterceptors();
        ServerSerialisers serialisers = info.getSerialisers();
        Features features = info.getFeatures();
        ExceptionMapping exceptionMapping = info.getExceptionMapping();
        List<ResourceClass> resourceClasses = info.getResourceClasses();
        List<ResourceClass> locatableResourceClasses = info.getLocatableResourceClasses();
        ParamConverterProviders paramConverterProviders = info.getParamConverterProviders();
        Supplier<Application> applicationSupplier = info.getApplicationSupplier();
        String applicationPath = info.getApplicationPath();

        DynamicEntityWriter dynamicEntityWriter = new DynamicEntityWriter(serialisers);

        RuntimeExceptionMapper exceptionMapper = new RuntimeExceptionMapper(exceptionMapping,
                Thread.currentThread().getContextClassLoader());
        ConfigurationImpl configurationImpl = configureFeatures(features, interceptors, exceptionMapper);

        RuntimeInterceptorDeployment interceptorDeployment = new RuntimeInterceptorDeployment(info, configurationImpl,
                closeTaskHandler);
        ResourceLocatorHandler resourceLocatorHandler = new ResourceLocatorHandler(
                new Function<>() {
                    @Override
                    public BeanFactory.BeanInstance<?> apply(Class<?> aClass) {
                        return info.getFactoryCreator().apply(aClass).createInstance();
                    }
                });
        List<RuntimeConfigurableServerRestHandler> runtimeConfigurableServerRestHandlers = new ArrayList<>();
        RuntimeResourceDeployment runtimeResourceDeployment = new RuntimeResourceDeployment(info, executorSupplier,
                virtualExecutorSupplier,
                interceptorDeployment, dynamicEntityWriter, resourceLocatorHandler, requestContextFactory.isDefaultBlocking());
        List<ResourceClass> possibleSubResource = new ArrayList<>(locatableResourceClasses);
        possibleSubResource.addAll(resourceClasses); //the TCK uses normal resources also as sub resources
        for (int i = 0; i < possibleSubResource.size(); i++) {
            ResourceClass clazz = possibleSubResource.get(i);
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> templates = new HashMap<>();
            URITemplate classPathTemplate = clazz.getPath() == null ? null : new URITemplate(clazz.getPath(), true);
            for (int j = 0; j < clazz.getMethods().size(); j++) {
                ResourceMethod method = clazz.getMethods().get(j);
                RuntimeResource runtimeResource = runtimeResourceDeployment.buildResourceMethod(
                        clazz, (ServerResourceMethod) method, true, classPathTemplate, info);
                addRuntimeConfigurableHandlers(runtimeResource, runtimeConfigurableServerRestHandlers);

                RuntimeMappingDeployment.buildMethodMapper(templates, method, runtimeResource);
            }
            Map<String, RequestMapper<RuntimeResource>> mappersByMethod = new RuntimeMappingDeployment(templates)
                    .buildClassMapper();
            resourceLocatorHandler.addResource(loadClass(clazz.getClassName()), mappersByMethod);
        }

        //it is possible that multiple resource classes use the same path
        //we use this map to merge them
        Map<URITemplate, Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>>> mappers = new TreeMap<>();

        for (int i = 0; i < resourceClasses.size(); i++) {
            ResourceClass clazz = resourceClasses.get(i);
            URITemplate classTemplate = new URITemplate(clazz.getPath(), true);
            var perClassMappers = mappers.get(classTemplate);
            if (perClassMappers == null) {
                mappers.put(classTemplate, perClassMappers = new HashMap<>());
            }
            for (int j = 0; j < clazz.getMethods().size(); j++) {
                ResourceMethod method = clazz.getMethods().get(j);
                RuntimeResource runtimeResource = runtimeResourceDeployment.buildResourceMethod(
                        clazz, (ServerResourceMethod) method, false, classTemplate, info);
                addRuntimeConfigurableHandlers(runtimeResource, runtimeConfigurableServerRestHandlers);

                RuntimeMappingDeployment.buildMethodMapper(perClassMappers, method, runtimeResource);
            }

        }
        classMappers = new ArrayList<>(mappers.size());
        mappers.forEach(this::forEachMapperEntry);

        List<ServerRestHandler> abortHandlingChain = new ArrayList<>(3);

        if (interceptorDeployment.getGlobalInterceptorHandler() != null) {
            abortHandlingChain.add(interceptorDeployment.getGlobalInterceptorHandler());
        }
        abortHandlingChain.add(new ExceptionHandler());
        abortHandlingChain.add(ResponseHandler.NO_CUSTOMIZER_INSTANCE);
        if (!interceptors.getContainerResponseFilters().getGlobalResourceInterceptors().isEmpty()) {
            abortHandlingChain.addAll(interceptorDeployment.getGlobalResponseInterceptorHandlers());
        }
        abortHandlingChain.add(new ResponseWriterHandler(dynamicEntityWriter));
        // sanitise the prefix for our usage to make it either an empty string, or something which starts with a / and does not
        // end with one
        String prefix = rootPath;
        if (prefix != null) {
            prefix = sanitizePathPrefix(prefix);
        } else {
            prefix = "";
        }
        if ((applicationPath != null) && !applicationPath.isEmpty()) {
            prefix = prefix + sanitizePathPrefix(applicationPath);
        }

        //pre matching interceptors are run first
        List<ServerRestHandler> preMatchHandlers = new ArrayList<>();
        for (int i = 0; i < info.getGlobalHandlerCustomizers().size(); i++) {
            preMatchHandlers
                    .addAll(info.getGlobalHandlerCustomizers().get(i).handlers(HandlerChainCustomizer.Phase.BEFORE_PRE_MATCH,
                            null, null));
        }
        if (!interceptors.getContainerRequestFilters().getPreMatchInterceptors().isEmpty()) {
            preMatchHandlers = new ArrayList<>(interceptorDeployment.getPreMatchContainerRequestFilters().size());
            for (Map.Entry<ResourceInterceptor<ContainerRequestFilter>, ContainerRequestFilter> entry : interceptorDeployment
                    .getPreMatchContainerRequestFilters()
                    .entrySet()) {
                preMatchHandlers
                        .add(new ResourceRequestFilterHandler(entry.getValue(), true, entry.getKey().isNonBlockingRequired(),
                                entry.getKey().isReadBody()));
            }
        }
        for (int i = 0; i < info.getGlobalHandlerCustomizers().size(); i++) {
            preMatchHandlers
                    .addAll(info.getGlobalHandlerCustomizers().get(i).handlers(HandlerChainCustomizer.Phase.AFTER_PRE_MATCH,
                            null, null));
        }
        return new Deployment(exceptionMapping, info.getCtxResolvers(), serialisers,
                abortHandlingChain.toArray(EMPTY_REST_HANDLER_ARRAY), dynamicEntityWriter,
                prefix, paramConverterProviders, configurationImpl, applicationSupplier,
                threadSetupAction, requestContextFactory, preMatchHandlers, classMappers,
                runtimeConfigurableServerRestHandlers, exceptionMapper, info.isResumeOn404(), info.getResteasyReactiveConfig());
    }

    private void forEachMapperEntry(URITemplate path,
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> classTemplates) {
        int classTemplateNameCount = path.countPathParamNames();
        RuntimeMappingDeployment runtimeMappingDeployment = new RuntimeMappingDeployment(classTemplates);
        ClassRoutingHandler classRoutingHandler = new ClassRoutingHandler(runtimeMappingDeployment.buildClassMapper(),
                classTemplateNameCount,
                info.isResumeOn404());
        classMappers.add(new RequestMapper.RequestPath<>(true, path,
                new RestInitialHandler.InitialMatch(new ServerRestHandler[] { classRoutingHandler },
                        runtimeMappingDeployment.getMaxMethodTemplateNameCount() + classTemplateNameCount)));
    }

    private void addRuntimeConfigurableHandlers(RuntimeResource runtimeResource,
            List<RuntimeConfigurableServerRestHandler> runtimeConfigurableServerRestHandlers) {
        for (ServerRestHandler serverRestHandler : runtimeResource.getHandlerChain()) {
            if (serverRestHandler instanceof RuntimeConfigurableServerRestHandler) {
                runtimeConfigurableServerRestHandlers.add((RuntimeConfigurableServerRestHandler) serverRestHandler);
            }
        }
    }

    //TODO: this needs plenty more work to support all possible types and provide all information the FeatureContext allows
    private ConfigurationImpl configureFeatures(Features features, ResourceInterceptors interceptors,
            RuntimeExceptionMapper exceptionMapping) {

        ConfigurationImpl configuration = new ConfigurationImpl(RuntimeType.SERVER);
        if (features.getResourceFeatures().isEmpty()) {
            return configuration;
        }

        FeatureContextImpl featureContext = new FeatureContextImpl(interceptors, exceptionMapping,
                configuration, info.getFactoryCreator());
        List<ResourceFeature> resourceFeatures = features.getResourceFeatures();
        for (ResourceFeature resourceFeature : resourceFeatures) {
            Feature feature = resourceFeature.getFactory().createInstance().getInstance();
            boolean enabled = feature.configure(featureContext);
            if (enabled) {
                configuration.addEnabledFeature(feature);
            }
        }
        if (featureContext.isFiltersNeedSorting()) {
            interceptors.sort();
        }
        return configuration;
    }

    private String sanitizePathPrefix(String prefix) {
        prefix = prefix.trim();
        if (prefix.equals("/"))
            prefix = "";
        // add leading slash
        if (!prefix.startsWith("/"))
            prefix = "/" + prefix;
        // remove trailing slash
        if (prefix.endsWith("/"))
            prefix = prefix.substring(0, prefix.length() - 1);
        return prefix;
    }

}
