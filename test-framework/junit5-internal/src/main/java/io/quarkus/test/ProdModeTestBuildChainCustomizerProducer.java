package io.quarkus.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import io.quarkus.builder.BuildChainBuilder;

// needs to be in a class of it's own in order to avoid java.lang.IncompatibleClassChangeError
public class ProdModeTestBuildChainCustomizerProducer
        implements Function<Map<String, Object>, List<Consumer<BuildChainBuilder>>> {

    @SuppressWarnings("unchecked")
    @Override
    public List<Consumer<BuildChainBuilder>> apply(Map<String, Object> testContext) {
        Map<String, Map<String, Object>> entries = (Map<String, Map<String, Object>>) testContext
                .get(QuarkusProdModeTest.BUILD_CONTEXT_BUILD_STEP_ENTRIES);
        List<Consumer<BuildChainBuilder>> result = new ArrayList<>(entries.size());
        for (Map.Entry<String, Map<String, Object>> entry : entries.entrySet()) {
            Map<String, Object> copiedTestContext = new HashMap<>(testContext);
            copiedTestContext.remove(QuarkusProdModeTest.BUILD_CONTEXT_BUILD_STEP_ENTRIES);
            Map<String, Object> entryContext = entry.getValue();
            String buildStepClassName = entry.getKey();
            result.add(new ProdModeTestBuildChainBuilderConsumer(
                    buildStepClassName,
                    (List<String>) entryContext.get(QuarkusProdModeTest.BUILD_CONTEXT_BUILD_STEP_ENTRY_PRODUCES),
                    (List<String>) entryContext.get(QuarkusProdModeTest.BUILD_CONTEXT_BUILD_STEP_ENTRY_CONSUMES),
                    copiedTestContext));
        }
        return result;
    }
}
