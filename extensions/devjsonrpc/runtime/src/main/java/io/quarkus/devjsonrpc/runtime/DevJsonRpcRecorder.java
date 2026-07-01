package io.quarkus.devjsonrpc.runtime;

import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.devjsonrpc.runtime.comms.JsonRpcRouter;
import io.quarkus.devjsonrpc.runtime.jsonrpc.JsonRpcMethod;
import io.quarkus.devjsonrpc.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DevJsonRpcRecorder {

    private static final Logger LOG = Logger.getLogger(DevJsonRpcRecorder.class);
    public static final String DEV_MANAGER_GLOBALS_JSON_MAPPER_FACTORY = "devjsonrpc-databind-codec-builder";

    public void createJsonRpcRouter(BeanContainer beanContainer,
            Map<String, JsonRpcMethod> runtimeMethods,
            Map<String, JsonRpcMethod> runtimeSubscriptions,
            Map<String, JsonRpcMethod> deploymentMethods,
            Map<String, JsonRpcMethod> deploymentSubscriptions,
            Map<String, JsonRpcMethod> recordedMethods,
            Map<String, JsonRpcMethod> recordedSubscriptions) {

        LOG.debugf("Creating JSON-RPC router with %d runtime methods, %d deployment methods",
                runtimeMethods.size(), deploymentMethods.size());
        JsonRpcRouter jsonRpcRouter = beanContainer.beanInstance(JsonRpcRouter.class);
        jsonRpcRouter.populateJsonRpcEndpoints(runtimeMethods, runtimeSubscriptions, deploymentMethods, deploymentSubscriptions,
                recordedMethods, recordedSubscriptions);
    }

    public void initializeCodec(BeanContainer beanContainer, JsonMapper jsonMapper) {
        JsonRpcRouter jsonRpcRouter = beanContainer.beanInstance(JsonRpcRouter.class);
        jsonRpcRouter.initializeCodec(jsonMapper);
    }
}
