package io.quarkus.devjsonrpc.runtime;

import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devjsonrpc.runtime.comms.JsonRpcRouter;
import io.quarkus.devjsonrpc.runtime.jsonrpc.JsonRpcMethod;
import io.quarkus.devjsonrpc.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DevJsonRpcRecorder {

    private static final Logger LOG = Logger.getLogger(DevJsonRpcRecorder.class);
    public static final String DEV_MANAGER_GLOBALS_JSON_MAPPER_FACTORY = "devjsonrpc-databind-codec-builder";

    /**
     * Initializes the router with its endpoints and codec in one go.
     *
     * @param jsonMapper the mapper to use, or null to use a mapper without any extra type adapters
     */
    public void createJsonRpcRouter(BeanContainer beanContainer,
            Map<String, JsonRpcMethod> runtimeMethods,
            Map<String, JsonRpcMethod> runtimeSubscriptions,
            Map<String, JsonRpcMethod> deploymentMethods,
            Map<String, JsonRpcMethod> deploymentSubscriptions,
            Map<String, JsonRpcMethod> recordedMethods,
            Map<String, JsonRpcMethod> recordedSubscriptions,
            RuntimeValue<JsonMapper> jsonMapper) {

        LOG.debugf("Creating JSON-RPC router with %d runtime methods, %d deployment methods",
                runtimeMethods.size(), deploymentMethods.size());
        JsonRpcRouter jsonRpcRouter = beanContainer.beanInstance(JsonRpcRouter.class);
        jsonRpcRouter.initialize(runtimeMethods, runtimeSubscriptions, deploymentMethods, deploymentSubscriptions,
                recordedMethods, recordedSubscriptions,
                jsonMapper != null ? jsonMapper.getValue() : createJsonMapperFactory().create(null, null, null));
    }

    /**
     * @return the factory the deployment registered, which creates mappers implemented in the deployment module
     */
    public static JsonMapper.Factory createJsonMapperFactory() {
        return JsonMapper.Factory.deploymentLinker()
                .createLink(DevConsoleManager.getGlobal(DEV_MANAGER_GLOBALS_JSON_MAPPER_FACTORY));
    }
}
