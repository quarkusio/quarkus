package io.quarkus.gcp.functions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.core.StartContext;

/**
 * Recorder for Google Cloud Functions.
 */
public class GoogleCloudFunctionRecorder {

    /**
     * Select the appropriate delegate function based on runtime configuration and registered functions.
     *
     * @param context the startup context
     * @param config the google cloud functions configuration
     * @param cloudFunctions the list of discovered cloud functions
     */
    public static void selectDelegate(StartContext context, GoogleCloudFunctionsConfig config,
            List<GoogleCloudFunctionInfo> cloudFunctions) {
        Map<GoogleCloudFunctionInfo.FunctionType, String> delegates = new HashMap<>();
        // if a function name is defined, check that it exists
        if (config.function().isPresent()) {
            String functionName = config.function().get();
            boolean found = false;
            for (GoogleCloudFunctionInfo info : cloudFunctions) {
                if (functionName.equals(info.beanName())) {
                    delegates.put(info.functionType(), info.className());
                    found = true;
                }
            }
            if (!found) {
                throw new RuntimeException(
                        "No function named " + functionName + ", did you forget to annotate your function with @Named ?");
            }
        } else {
            for (GoogleCloudFunctionInfo info : cloudFunctions) {
                if (delegates.containsKey(info.functionType())) {
                    throw new RuntimeException(
                            "Multiple functions found, please use 'quarkus.google-cloud-functions.function' to select one");
                }
                delegates.put(info.functionType(), info.className());
            }
        }
        QuarkusHttpFunction.setDelegate(delegates.get(GoogleCloudFunctionInfo.FunctionType.HTTP));
        QuarkusBackgroundFunction.setDelegates(delegates.get(GoogleCloudFunctionInfo.FunctionType.BACKGROUND),
                delegates.get(GoogleCloudFunctionInfo.FunctionType.RAW_BACKGROUND));
        QuarkusCloudEventsFunction.setDelegate(delegates.get(GoogleCloudFunctionInfo.FunctionType.CLOUD_EVENT));

        context.onStop(new Runnable() {
            @Override
            public void run() {
                QuarkusHttpFunction.clearState();
                QuarkusBackgroundFunction.clearState();
                QuarkusCloudEventsFunction.clearState();
            }
        });
    }
}
