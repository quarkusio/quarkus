package io.quarkus.qute;

import static io.quarkus.qute.Futures.evaluateParams;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Basic {@code set} statement.
 */
public class SetSectionHelper implements SectionHelper {

    private static final String SET = "set";

    private final Map<String, Expression> parameters;

    SetSectionHelper(Map<String, Expression> parameters) {
        this.parameters = parameters;
    }

    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        CompletableFuture<ResultNode> result = new CompletableFuture<>();
        evaluateParams(parameters, context.resolutionContext()).whenComplete((r, t) -> {
            if (t != null) {
                result.completeExceptionally(t);
            } else {
                // Execute the main block with the params as the current context object
                context.execute(context.resolutionContext().createChild(r, null)).whenComplete((r2, t2) -> {
                    if (t2 != null) {
                        result.completeExceptionally(t2);
                    } else {
                        result.complete(r2);
                    }
                });
            }
        });
        return result;
    }

    public static class Factory implements SectionHelperFactory<SetSectionHelper> {

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of(SET);
        }

        @Override
        public ParametersInfo getParameters() {
            return ParametersInfo.EMPTY;
        }

        @Override
        public SetSectionHelper initialize(SectionInitContext context) {
            Map<String, Expression> params = context.getParameters().entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey(), e -> context.parseValue(e.getValue())));
            return new SetSectionHelper(params);
        }

        @Override
        public Map<String, String> initializeBlock(Map<String, String> outerNameTypeInfos, BlockInfo block) {
            if (block.getLabel().equals(MAIN_BLOCK_NAME)) {
                Map<String, String> typeInfos = new HashMap<String, String>(outerNameTypeInfos);
                for (Entry<String, String> entry : block.getParameters().entrySet()) {
                    Expression expr = block.addExpression(entry.getKey(), entry.getValue());
                    typeInfos.put(entry.getKey(), expr.typeCheckInfo);
                }
                return typeInfos;
            } else {
                return Collections.emptyMap();
            }
        }

    }
}