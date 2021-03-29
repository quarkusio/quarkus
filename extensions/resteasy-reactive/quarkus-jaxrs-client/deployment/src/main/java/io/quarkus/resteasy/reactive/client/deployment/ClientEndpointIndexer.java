package io.quarkus.resteasy.reactive.client.deployment;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_ARRAY;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_NUMBER;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_OBJECT;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_STRING;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_STRUCTURE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.JSONP_JSON_VALUE;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.model.InjectableBean;
import org.jboss.resteasy.reactive.common.model.MaybeRestClientInterface;
import org.jboss.resteasy.reactive.common.model.MethodParameter;
import org.jboss.resteasy.reactive.common.model.ParameterType;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.common.model.RestClientInterface;
import org.jboss.resteasy.reactive.common.processor.AdditionalReaderWriter;
import org.jboss.resteasy.reactive.common.processor.AdditionalReaders;
import org.jboss.resteasy.reactive.common.processor.AdditionalWriters;
import org.jboss.resteasy.reactive.common.processor.EndpointIndexer;
import org.jboss.resteasy.reactive.common.processor.IndexedParameter;
import org.jboss.resteasy.reactive.common.providers.serialisers.jsonp.JsonArrayHandler;
import org.jboss.resteasy.reactive.common.providers.serialisers.jsonp.JsonObjectHandler;
import org.jboss.resteasy.reactive.common.providers.serialisers.jsonp.JsonStructureHandler;
import org.jboss.resteasy.reactive.common.providers.serialisers.jsonp.JsonValueHandler;

import io.quarkus.resteasy.reactive.client.deployment.beanparam.BeanParamParser;
import io.quarkus.resteasy.reactive.client.deployment.beanparam.ClientBeanParamInfo;
import io.quarkus.resteasy.reactive.client.deployment.beanparam.Item;

public class ClientEndpointIndexer
        extends EndpointIndexer<ClientEndpointIndexer, ClientEndpointIndexer.ClientIndexedParam, ResourceMethod> {

    private final String[] defaultProduces;
    private final String[] defaultProducesNegotiated;

    ClientEndpointIndexer(Builder builder, String defaultProduces) {
        super(builder);
        this.defaultProduces = new String[] { defaultProduces };
        this.defaultProducesNegotiated = new String[] { defaultProduces, MediaType.WILDCARD };
    }

    public MaybeRestClientInterface createClientProxy(ClassInfo classInfo,
            String path) {
        try {
            RestClientInterface clazz = new RestClientInterface();
            clazz.setClassName(classInfo.name().toString());
            if (path != null) {
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                clazz.setPath(path);
            }
            List<ResourceMethod> methods = createEndpoints(classInfo, classInfo, new HashSet<>(),
                    clazz.getPathParameters());
            clazz.getMethods().addAll(methods);
            return MaybeRestClientInterface.success(clazz);
        } catch (Exception e) {
            //kinda bogus, but we just ignore failed interfaces for now
            //they can have methods that are not valid until they are actually extended by a concrete type

            log.warn("Ignoring interface for creating client proxy" + classInfo.name(), e);
            return MaybeRestClientInterface.failure(e.getMessage());
        }
    }

    @Override
    protected ResourceMethod createResourceMethod(MethodInfo info, Map<String, Object> methodContext) {
        return new ResourceMethod();
    }

    @Override
    protected boolean handleBeanParam(ClassInfo actualEndpointInfo, Type paramType, MethodParameter[] methodParameters, int i) {
        ClassInfo beanParamClassInfo = index.getClassByName(paramType.name());
        methodParameters[i] = parseClientBeanParam(beanParamClassInfo, index);

        return false;
    }

    private MethodParameter parseClientBeanParam(ClassInfo beanParamClassInfo, IndexView index) {
        List<Item> items = BeanParamParser.parse(beanParamClassInfo, index);
        return new ClientBeanParamInfo(items, beanParamClassInfo.name().toString());
    }

    protected InjectableBean scanInjectableBean(ClassInfo currentClassInfo, ClassInfo actualEndpointInfo,
            Map<String, String> existingConverters, AdditionalReaders additionalReaders,
            Map<String, InjectableBean> injectableBeans, boolean hasRuntimeConverters) {
        throw new RuntimeException("Injectable beans not supported in client");
    }

    protected MethodParameter createMethodParameter(ClassInfo currentClassInfo, ClassInfo actualEndpointInfo, boolean encoded,
            Type paramType, ClientIndexedParam parameterResult, String name, String defaultValue, ParameterType type,
            String elementType, boolean single, String signature) {
        return new MethodParameter(name,
                elementType, toClassName(paramType, currentClassInfo, actualEndpointInfo, index), signature, type, single,
                defaultValue, parameterResult.isObtainedAsCollection(), parameterResult.isOptional(), encoded);
    }

    @Override
    protected String[] applyAdditionalDefaults(Type nonAsyncReturnType) {
        if (config.isSingleDefaultProduces()) {
            return defaultProduces;
        } else {
            return defaultProducesNegotiated;
        }
    }

    protected void addWriterForType(AdditionalWriters additionalWriters, Type paramType) {
        addReaderWriterForType(additionalWriters, paramType);
    }

    protected void addReaderForType(AdditionalReaders additionalReaders, Type paramType) {
        addReaderWriterForType(additionalReaders, paramType);
    }

    private void addReaderWriterForType(AdditionalReaderWriter additionalReaderWriter, Type paramType) {
        DotName dotName = paramType.name();
        if (dotName.equals(JSONP_JSON_NUMBER)
                || dotName.equals(JSONP_JSON_VALUE)
                || dotName.equals(JSONP_JSON_STRING)) {
            additionalReaderWriter.add(JsonValueHandler.class, APPLICATION_JSON, javax.json.JsonValue.class);
        } else if (dotName.equals(JSONP_JSON_ARRAY)) {
            additionalReaderWriter.add(JsonArrayHandler.class, APPLICATION_JSON, javax.json.JsonArray.class);
        } else if (dotName.equals(JSONP_JSON_OBJECT)) {
            additionalReaderWriter.add(JsonObjectHandler.class, APPLICATION_JSON, javax.json.JsonObject.class);
        } else if (dotName.equals(JSONP_JSON_STRUCTURE)) {
            additionalReaderWriter.add(JsonStructureHandler.class, APPLICATION_JSON, javax.json.JsonStructure.class);
        }
    }

    @Override
    protected ClientIndexedParam createIndexedParam() {
        return new ClientIndexedParam();
    }

    public static class ClientIndexedParam extends IndexedParameter<ClientIndexedParam> {

    }

    public static final class Builder extends EndpointIndexer.Builder<ClientEndpointIndexer, Builder, ResourceMethod> {
        private String defaultProduces = MediaType.TEXT_PLAIN;

        public Builder setDefaultProduces(String defaultProduces) {
            this.defaultProduces = defaultProduces;
            return this;
        }

        @Override
        public ClientEndpointIndexer build() {
            return new ClientEndpointIndexer(this, defaultProduces);
        }
    }
}
