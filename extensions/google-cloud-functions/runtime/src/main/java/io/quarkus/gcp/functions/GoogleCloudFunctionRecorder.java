package io.quarkus.gcp.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.core.StartContext;

public class GoogleCloudFunctionRecorder {

    public static void selectDelegate(StartContext context, GoogleCloudFunctionsConfig config, List<String> beanNames,
            List<String> classNames, List<GoogleCloudFunctionInfo.FunctionType> functionTypes) {
        List<GoogleCloudFunctionInfo> cloudFunctions = new ArrayList<>(beanNames.size());
        for (int i = 0; i < beanNames.size(); i++) {
            GoogleCloudFunctionInfo info = new GoogleCloudFunctionInfo();
            info.setBeanName(beanNames.get(i));
            info.setClassName(classNames.get(i));
            info.setFunctionType(functionTypes.get(i));
            cloudFunctions.add(info);
        }
        Map<GoogleCloudFunctionInfo.FunctionType, String> delegates = new HashMap<>();
        // if a function name is defined, check that it exists
        if (config.function().isPresent()) {
            String functionName = config.function().get();
            boolean found = false;
            for (GoogleCloudFunctionInfo info : cloudFunctions) {
                if (functionName.equals(info.getBeanName())) {
                    delegates.put(info.getFunctionType(), info.getClassName());
                    found = true;
                }
            }
            if (!found) {
                throw new RuntimeException(
                        "No function named " + functionName + ", did you forget to annotate your function with @Named ?");
            }
        } else {
            for (GoogleCloudFunctionInfo info : cloudFunctions) {
                if (delegates.containsKey(info.getFunctionType())) {
                    throw new RuntimeException(
                            "Multiple functions found, please use 'quarkus.google-cloud-functions.function' to select one");
                }
                delegates.put(info.getFunctionType(), info.getClassName());
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
