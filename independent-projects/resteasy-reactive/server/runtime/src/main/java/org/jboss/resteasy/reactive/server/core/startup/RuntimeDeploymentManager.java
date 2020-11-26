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
import org.jboss.resteasy.reactive.common.jaxrs.QuarkusRestConfiguration;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.common.model.ResourceFeature;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptors;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.DeploymentInfo;
import org.jboss.resteasy.reactive.server.core.ExceptionMapping;
import org.jboss.resteasy.reactive.server.core.Features;
import org.jboss.resteasy.reactive.server.core.ParamConverterProviders;
import org.jboss.resteasy.reactive.server.core.RequestContextFactory;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;
import org.jboss.resteasy.reactive.server.core.serialization.DynamicEntityWriter;
import org.jboss.resteasy.reactive.server.handlers.ClassRoutingHandler;
import org.jboss.resteasy.reactive.server.handlers.ExceptionHandler;
import org.jboss.resteasy.reactive.server.handlers.QuarkusRestInitialHandler;
import org.jboss.resteasy.reactive.server.handlers.ResourceLocatorHandler;
import org.jboss.resteasy.reactive.server.handlers.ResourceRequestFilterHandler;
import org.jboss.resteasy.reactive.server.handlers.ResponseHandler;
import org.jboss.resteasy.reactive.server.handlers.ResponseWriterHandler;
import org.jboss.resteasy.reactive.server.jaxrs.QuarkusRestFeatureContext;
import org.jboss.resteasy.reactive.server.mapping.RequestMapper;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.mapping.URITemplate;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.spi.BeanFactory;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

public class RuntimeDeploymentManager {
    public static final ServerRestHandler[] EMPTY_REST_HANDLER_ARRAY = new ServerRestHandler[0];
    private final DeploymentInfo info;
    private final Supplier<Executor> executorSupplier;
    private final Consumer<Closeable> closeTaskHandler;
    private final RequestContextFactory requestContextFactory;
    private final ThreadSetupAction threadSetupAction;
    private final String rootPath;

    public RuntimeDeploymentManager(DeploymentInfo info,
            Supplier<Executor> executorSupplier,
            Consumer<Closeable> closeTaskHandler,
            RequestContextFactory requestContextFactory, ThreadSetupAction threadSetupAction, String rootPath) {
        this.info = info;
        this.executorSupplier = executorSupplier;
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

        QuarkusRestConfiguration quarkusRestConfiguration = configureFeatures(features, interceptors, exceptionMapping);

        RuntimeInterceptorDeployment interceptorDeployment = new RuntimeInterceptorDeployment(info, quarkusRestConfiguration,
                closeTaskHandler);
        ResourceLocatorHandler resourceLocatorHandler = new ResourceLocatorHandler(
                new Function<Class<?>, BeanFactory.BeanInstance<?>>() {
                    @Override
                    public BeanFactory.BeanInstance<?> apply(Class<?> aClass) {
                        return info.getFactoryCreator().apply(aClass).createInstance();
                    }
                });
        RuntimeResourceDeployment runtimeResourceDeployment = new RuntimeResourceDeployment(info, executorSupplier,
                interceptorDeployment, dynamicEntityWriter, resourceLocatorHandler);
        List<ResourceClass> possibleSubResource = new ArrayList<>(locatableResourceClasses);
        possibleSubResource.addAll(resourceClasses); //the TCK uses normal resources also as sub resources
        for (ResourceClass clazz : possibleSubResource) {
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> templates = new HashMap<>();
            URITemplate classPathTemplate = clazz.getPath() == null ? null : new URITemplate(clazz.getPath(), true);
            for (ResourceMethod method : clazz.getMethods()) {
                //TODO: add DynamicFeature for these
                RuntimeResource runtimeResource = runtimeResourceDeployment.buildResourceMethod(
                        clazz, method, true, classPathTemplate);

                RuntimeMappingDeployment.buildMethodMapper(templates, method, runtimeResource);
            }
            Map<String, RequestMapper<RuntimeResource>> mappersByMethod = RuntimeMappingDeployment.buildClassMapper(templates);
            resourceLocatorHandler.addResource(loadClass(clazz.getClassName()), mappersByMethod);
        }

        //it is possible that multiple resource classes use the same path
        //we use this map to merge them
        Map<URITemplate, Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>>> mappers = new TreeMap<>();

        for (ResourceClass clazz : resourceClasses) {
            URITemplate classTemplate = new URITemplate(clazz.getPath(), true);
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> perClassMappers = mappers
                    .get(classTemplate);
            if (perClassMappers == null) {
                mappers.put(classTemplate, perClassMappers = new HashMap<>());
            }
            for (ResourceMethod method : clazz.getMethods()) {
                RuntimeResource runtimeResource = runtimeResourceDeployment.buildResourceMethod(
                        clazz, method, false, classTemplate);

                RuntimeMappingDeployment.buildMethodMapper(perClassMappers, method, runtimeResource);
            }

        }
        List<RequestMapper.RequestPath<QuarkusRestInitialHandler.InitialMatch>> classMappers = new ArrayList<>();
        for (Map.Entry<URITemplate, Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>>> entry : mappers
                .entrySet()) {
            URITemplate classTemplate = entry.getKey();
            int classTemplateNameCount = classTemplate.countPathParamNames();
            Map<String, TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>>> perClassMappers = entry
                    .getValue();
            Map<String, RequestMapper<RuntimeResource>> mappersByMethod = RuntimeMappingDeployment
                    .buildClassMapper(perClassMappers);
            ClassRoutingHandler classRoutingHandler = new ClassRoutingHandler(mappersByMethod, classTemplateNameCount);

            int maxMethodTemplateNameCount = 0;
            for (TreeMap<URITemplate, List<RequestMapper.RequestPath<RuntimeResource>>> i : perClassMappers.values()) {
                for (URITemplate j : i.keySet()) {
                    maxMethodTemplateNameCount = Math.max(maxMethodTemplateNameCount, j.countPathParamNames());
                }
            }
            classMappers.add(new RequestMapper.RequestPath<>(true, classTemplate,
                    new QuarkusRestInitialHandler.InitialMatch(new ServerRestHandler[] { classRoutingHandler },
                            maxMethodTemplateNameCount + classTemplateNameCount)));
        }

        List<ServerRestHandler> abortHandlingChain = new ArrayList<>();

        if (interceptorDeployment.getGlobalInterceptorHandler() != null) {
            abortHandlingChain.add(interceptorDeployment.getGlobalInterceptorHandler());
        }
        abortHandlingChain.add(new ExceptionHandler());
        if (!interceptors.getContainerResponseFilters().getGlobalResourceInterceptors().isEmpty()) {
            abortHandlingChain.addAll(interceptorDeployment.getGlobalResponseInterceptorHandlers());
        }
        abortHandlingChain.add(new ResponseHandler());
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
        List<ResourceRequestFilterHandler> preMatchHandlers = null;
        if (!interceptors.getContainerRequestFilters().getPreMatchInterceptors().isEmpty()) {
            preMatchHandlers = new ArrayList<>(interceptorDeployment.getPreMatchContainerRequestFilters().size());
            for (ContainerRequestFilter containerRequestFilter : interceptorDeployment.getPreMatchContainerRequestFilters()
                    .values()) {
                preMatchHandlers.add(new ResourceRequestFilterHandler(containerRequestFilter, true));
            }
        }

        Deployment deployment = new Deployment(exceptionMapping, info.getCtxResolvers(), serialisers,
                abortHandlingChain.toArray(EMPTY_REST_HANDLER_ARRAY), dynamicEntityWriter,
                prefix, paramConverterProviders, quarkusRestConfiguration, applicationSupplier,
                threadSetupAction, requestContextFactory, preMatchHandlers, classMappers);

        return deployment;
    }

    //TODO: this needs plenty more work to support all possible types and provide all information the FeatureContext allows
    private QuarkusRestConfiguration configureFeatures(Features features, ResourceInterceptors interceptors,
            ExceptionMapping exceptionMapping) {

        QuarkusRestConfiguration configuration = new QuarkusRestConfiguration(RuntimeType.SERVER);
        if (features.getResourceFeatures().isEmpty()) {
            return configuration;
        }

        QuarkusRestFeatureContext featureContext = new QuarkusRestFeatureContext(interceptors, exceptionMapping,
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
