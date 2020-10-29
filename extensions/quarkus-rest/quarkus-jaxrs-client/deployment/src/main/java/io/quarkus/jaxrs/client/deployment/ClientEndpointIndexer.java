package io.quarkus.jaxrs.client.deployment;

import static io.quarkus.rest.common.deployment.framework.QuarkusRestDotNames.JSONP_JSON_ARRAY;
import static io.quarkus.rest.common.deployment.framework.QuarkusRestDotNames.JSONP_JSON_NUMBER;
import static io.quarkus.rest.common.deployment.framework.QuarkusRestDotNames.JSONP_JSON_OBJECT;
import static io.quarkus.rest.common.deployment.framework.QuarkusRestDotNames.JSONP_JSON_STRING;
import static io.quarkus.rest.common.deployment.framework.QuarkusRestDotNames.JSONP_JSON_STRUCTURE;
import static io.quarkus.rest.common.deployment.framework.QuarkusRestDotNames.JSONP_JSON_VALUE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.rest.common.deployment.framework.AdditionalReaders;
import io.quarkus.rest.common.deployment.framework.AdditionalWriters;
import io.quarkus.rest.common.deployment.framework.EndpointIndexer;
import io.quarkus.rest.common.deployment.framework.IndexedParameter;
import io.quarkus.rest.common.runtime.model.InjectableBean;
import io.quarkus.rest.common.runtime.model.MethodParameter;
import io.quarkus.rest.common.runtime.model.ParameterType;
import io.quarkus.rest.common.runtime.model.ResourceMethod;
import io.quarkus.rest.common.runtime.model.RestClientInterface;
import io.quarkus.rest.common.runtime.providers.serialisers.jsonp.JsonArrayHandler;
import io.quarkus.rest.common.runtime.providers.serialisers.jsonp.JsonObjectHandler;
import io.quarkus.rest.common.runtime.providers.serialisers.jsonp.JsonStructureHandler;
import io.quarkus.rest.common.runtime.providers.serialisers.jsonp.JsonValueHandler;

public class ClientEndpointIndexer extends EndpointIndexer<ClientEndpointIndexer, ClientEndpointIndexer.ClientIndexedParam> {
    ClientEndpointIndexer(Builder builder) {
        super(builder);
    }

    public RestClientInterface createClientProxy(ClassInfo classInfo,
            String path) {
        try {
            RestClientInterface clazz = new RestClientInterface();
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
            List<ResourceMethod> methods = createEndpoints(classInfo, classInfo, new HashSet<>(),
                    clazz.getPathParameters());
            clazz.getMethods().addAll(methods);
            return clazz;
        } catch (Exception e) {
            //kinda bogus, but we just ignore failed interfaces for now
            //they can have methods that are not valid until they are actually extended by a concrete type
            log.debug("Ignoring interface for creating client proxy" + classInfo.name(), e);
            return null;
        }
    }

    @Override
    protected InjectableBean scanInjectableBean(ClassInfo currentClassInfo,
            ClassInfo actualEndpointInfo,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildProducer,
            Map<String, String> existingConverters,
            AdditionalReaders additionalReaders,
            Map<String, InjectableBean> injectableBeans,
            boolean hasRuntimeConverters) {
        throw new RuntimeException("Injectable beans not supported in client");
    }

    protected MethodParameter createMethodParameter(ClassInfo currentClassInfo, ClassInfo actualEndpointInfo, boolean encoded,
            Type paramType, ClientIndexedParam parameterResult, String name, String defaultValue, ParameterType type,
            String elementType, boolean single) {
        return new MethodParameter(name,
                elementType, toClassName(paramType, currentClassInfo, actualEndpointInfo, index), type, single,
                defaultValue, parameterResult.isObtainedAsCollection(), encoded);
    }

    protected void addWriterForType(AdditionalWriters additionalWriters, Type paramType) {
        DotName dotName = paramType.name();
        if (dotName.equals(JSONP_JSON_VALUE)
                || dotName.equals(JSONP_JSON_NUMBER)
                || dotName.equals(JSONP_JSON_STRING)) {
            additionalWriters.add(JsonValueHandler.class, APPLICATION_JSON, javax.json.JsonValue.class);
        } else if (dotName.equals(JSONP_JSON_ARRAY)) {
            additionalWriters.add(JsonArrayHandler.class, APPLICATION_JSON, javax.json.JsonArray.class);
        } else if (dotName.equals(JSONP_JSON_OBJECT)) {
            additionalWriters.add(JsonObjectHandler.class, APPLICATION_JSON, javax.json.JsonObject.class);
        } else if (dotName.equals(JSONP_JSON_STRUCTURE)) {
            additionalWriters.add(JsonStructureHandler.class, APPLICATION_JSON, javax.json.JsonStructure.class);
        }
    }

    protected void addReaderForType(AdditionalReaders additionalReaders, Type paramType) {
        DotName dotName = paramType.name();
        if (dotName.equals(JSONP_JSON_NUMBER)
                || dotName.equals(JSONP_JSON_VALUE)
                || dotName.equals(JSONP_JSON_STRING)) {
            additionalReaders.add(JsonValueHandler.class, APPLICATION_JSON, javax.json.JsonValue.class);
        } else if (dotName.equals(JSONP_JSON_ARRAY)) {
            additionalReaders.add(JsonArrayHandler.class, APPLICATION_JSON, javax.json.JsonArray.class);
        } else if (dotName.equals(JSONP_JSON_OBJECT)) {
            additionalReaders.add(JsonObjectHandler.class, APPLICATION_JSON, javax.json.JsonObject.class);
        } else if (dotName.equals(JSONP_JSON_STRUCTURE)) {
            additionalReaders.add(JsonStructureHandler.class, APPLICATION_JSON, javax.json.JsonStructure.class);
        }
    }

    @Override
    protected ClientIndexedParam createIndexedParam() {
        return new ClientIndexedParam();
    }

    public static class ClientIndexedParam extends IndexedParameter<ClientIndexedParam> {

    }

    public static final class Builder extends EndpointIndexer.Builder<ClientEndpointIndexer, Builder> {
        @Override
        public ClientEndpointIndexer build() {
            return new ClientEndpointIndexer(this);
        }
    }
}
