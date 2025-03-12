package org.jboss.resteasy.reactive.server.core.startup;

import static org.jboss.resteasy.reactive.common.util.DeploymentUtils.loadClass;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Feature;

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
import org.jboss.resteasy.reactive.server.spi.GenericRuntimeConfigurableServerRestHandler;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.spi.BeanFactory;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class RuntimeDeploymentManager {
    public static final ServerRestHandler[] EMPTY_REST_HANDLER_ARRAY = new ServerRestHandler[0];
    private final DeploymentInfo info;
    private final Supplier<Executor> executorSupplier;
    private final Supplier<Executor> virtualExecutorSupplier;
    private final Consumer<Closeable> closeTaskHandler;
    private final RequestContextFactory requestContextFactory;
    private final ThreadSetupAction threadSetupAction;
    private final String rootPath;

    private ArrayList<RequestMapper.RequestPath<RestInitialHandler.InitialMatch>> classMappers;

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
                }, info.getClientProxyUnwrapper());

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
        // to use it inside lambdas
        String finalPrefix = prefix;

        List<GenericRuntimeConfigurableServerRestHandler<?>> runtimeConfigurableServerRestHandlers = new ArrayList<>();
        RuntimeResourceDeployment runtimeResourceDeployment = new RuntimeResourceDeployment(info, executorSupplier,
                virtualExecutorSupplier,
                interceptorDeployment, dynamicEntityWriter, resourceLocatorHandler, requestContextFactory.isDefaultBlocking());
        List<ResourceClass> possibleSubResource = new ArrayList<>(locatableResourceClasses);
        possibleSubResource.addAll(resourceClasses); //the TCK uses normal resources also as sub resources
        Map<String, List<String>> disabledEndpoints = new HashMap<>();
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
            boolean isResourceClassDisabled = (clazz.getIsDisabled() != null) && clazz.getIsDisabled().get();
            if (isResourceClassDisabled) {
                mappersByMethod.forEach(new BiConsumer<>() {
                    @Override
                    public void accept(String method, RequestMapper<RuntimeResource> mapper) {
                        for (int i = 0; i < mapper.getTemplates().size(); i++) {
                            RequestMapper.RequestPath<RuntimeResource> path = mapper.getTemplates().get(i);
                            String templateWithoutSlash = path.template.template.startsWith("/")
                                    ? path.template.template.substring(1)
                                    : path.template.template;
                            String fullPath = clazz.getPath().endsWith("/")
                                    ? finalPrefix + clazz.getPath() + templateWithoutSlash
                                    : finalPrefix + clazz.getPath() + "/" + templateWithoutSlash;
                            if (!disabledEndpoints.containsKey(fullPath)) {
                                disabledEndpoints.put(fullPath, new ArrayList<>());
                            }
                            disabledEndpoints.get(fullPath).add(method);
                        }
                    }
                });
            } else {
                resourceLocatorHandler.addResource(loadClass(clazz.getClassName()), mappersByMethod);
            }
        }

        //it is possible that multiple resource classes use the same path
        //we use this map to merge them
        Map<MappersKey, Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>>> mappers = new TreeMap<>();

        for (int i = 0; i < resourceClasses.size(); i++) {
            ResourceClass clazz = resourceClasses.get(i);
            if ((clazz.getIsDisabled() != null) && clazz.getIsDisabled().get()) {
                continue;
            }
            URITemplate classTemplate = new URITemplate(clazz.getPath(), true);

            MappersKey key = new MappersKey(classTemplate);

            var perClassMappers = mappers.get(key);
            if (perClassMappers == null) {
                mappers.put(key, perClassMappers = new HashMap<>());
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
        if (info.getPreExceptionMapperHandler() != null) {
            abortHandlingChain.add(info.getPreExceptionMapperHandler());
        }
        abortHandlingChain.add(new ExceptionHandler());
        abortHandlingChain.add(ResponseHandler.NO_CUSTOMIZER_INSTANCE);
        if (!interceptors.getContainerResponseFilters().getGlobalResourceInterceptors().isEmpty()) {
            abortHandlingChain.addAll(interceptorDeployment.getGlobalResponseInterceptorHandlers());
        }
        abortHandlingChain.add(new ResponseWriterHandler(dynamicEntityWriter));

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
                                entry.getKey().isWithFormRead()));
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
                runtimeConfigurableServerRestHandlers, exceptionMapper, info.isServletPresent(),
                info.getResteasyReactiveConfig(),
                disabledEndpoints);
    }

    private void forEachMapperEntry(MappersKey key,
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> classTemplates) {
        int classTemplateNameCount = key.path.countPathParamNames();
        RuntimeMappingDeployment runtimeMappingDeployment = new RuntimeMappingDeployment(classTemplates);
        ClassRoutingHandler classRoutingHandler = new ClassRoutingHandler(runtimeMappingDeployment.buildClassMapper(),
                classTemplateNameCount, info.isServletPresent());
        classMappers.add(new RequestMapper.RequestPath<>(true, key.path,
                new RestInitialHandler.InitialMatch(new ServerRestHandler[] { classRoutingHandler },
                        runtimeMappingDeployment.getMaxMethodTemplateNameCount() + classTemplateNameCount)));
    }

    private void addRuntimeConfigurableHandlers(RuntimeResource runtimeResource,
            List<GenericRuntimeConfigurableServerRestHandler<?>> runtimeConfigurableServerRestHandlers) {
        for (ServerRestHandler serverRestHandler : runtimeResource.getHandlerChain()) {
            if (serverRestHandler instanceof GenericRuntimeConfigurableServerRestHandler) {
                runtimeConfigurableServerRestHandlers.add((GenericRuntimeConfigurableServerRestHandler<?>) serverRestHandler);
            }
        }
    }

    private ConfigurationImpl configureFeatures(Features features, ResourceInterceptors interceptors,
            RuntimeExceptionMapper exceptionMapping) {

        ConfigurationImpl configuration = new ConfigurationImpl(RuntimeType.SERVER);
        if (features.getResourceFeatures().isEmpty()) {
            return configuration;
        }

        FeatureContextImpl featureContext = new FeatureContextImpl(interceptors, exceptionMapping,
                configuration, info.getFactoryCreator());
        List<ResourceFeature> resourceFeatures = features.getResourceFeatures();
        for (int i = 0; i < resourceFeatures.size(); i++) {
            ResourceFeature resourceFeature = resourceFeatures.get(i);
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

    private static class MappersKey implements Comparable<MappersKey> {
        private final String key;
        private final URITemplate path;

        public MappersKey(URITemplate path) {
            this.path = path;

            if (path.components.length == 0) {
                this.key = "";
            } else {
                // create a key without any names. Names of e.g. default regex components can differ, but the component still has the same meaning.
                StringBuilder keyBuilder = new StringBuilder();
                for (URITemplate.TemplateComponent component : path.components) {
                    int standardLength = component.type.name().length() + 1
                            + (component.literalText != null ? component.literalText.length() : 0) + 1 + 1;
                    int additionalLength = 0;
                    if (component.pattern != null) {
                        additionalLength = component.pattern.pattern().length();
                    }
                    StringBuilder kb = new StringBuilder(standardLength + additionalLength);
                    kb.append(component.type);
                    kb.append(";");
                    kb.append(component.literalText);
                    kb.append(";");
                    if (component.pattern != null) {
                        // (?<id1>[a-zA-Z]+) -> [a-zA-Z]+
                        String pattern = component.pattern.pattern();
                        kb.append(component.pattern.pattern(), pattern.indexOf('>') + 1, pattern.length() - 1);
                    }
                    kb.append("|");
                    keyBuilder.append(kb);
                }

                this.key = keyBuilder.toString();
            }

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null) {
                return false;
            }

            return key.equals(((MappersKey) o).key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public int compareTo(MappersKey o) {
            if (key.equals(o.key)) {
                return 0;
            }

            return path.compareTo(o.path);
        }
    }

}
