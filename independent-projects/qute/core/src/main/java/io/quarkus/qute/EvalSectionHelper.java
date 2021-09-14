package io.quarkus.qute;

import static io.quarkus.qute.Futures.evaluateParams;

import io.quarkus.qute.TemplateNode.Origin;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class EvalSectionHelper implements SectionHelper {

    private static final String TEMPLATE = "template";

    private final Map<String, Expression> parameters;
    private final Engine engine;

    public EvalSectionHelper(Map<String, Expression> parameters, Engine engine) {
        this.parameters = parameters;
        this.engine = engine;
    }

    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        CompletableFuture<ResultNode> result = new CompletableFuture<>();
        evaluateParams(parameters, context.resolutionContext()).whenComplete((evaluatedParams, t1) -> {
            if (t1 != null) {
                result.completeExceptionally(t1);
            } else {
                try {
                    // Parse the template and execute with the params as the root context object
                    String templateStr = evaluatedParams.get(TEMPLATE).toString();
                    TemplateImpl template;
                    try {
                        template = (TemplateImpl) engine.parse(templateStr);
                    } catch (TemplateException e) {
                        Origin origin = parameters.get(TEMPLATE).getOrigin();
                        StringBuilder builder = new StringBuilder("Parser error in the evaluated template");
                        if (!origin.getTemplateId().equals(origin.getTemplateGeneratedId())) {
                            builder.append(" in template [").append(origin.getTemplateId()).append("]");
                        }
                        builder.append(" on line ").append(origin.getLine()).append(":\n\t")
                                .append(e.getMessage());
                        throw new TemplateException(parameters.get(TEMPLATE).getOrigin(),
                                builder.toString());
                    }
                    template.root
                            .resolve(context.resolutionContext().createChild(Mapper.wrap(evaluatedParams), null))
                            .whenComplete((resultNode, t2) -> {
                                if (t2 != null) {
                                    result.completeExceptionally(t2);
                                } else {
                                    result.complete(resultNode);
                                }
                            });
                } catch (Throwable e) {
                    result.completeExceptionally(e);
                }
            }
        });
        return result;
    }

    public static class Factory implements SectionHelperFactory<EvalSectionHelper> {

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of("eval");
        }

        @Override
        public ParametersInfo getParameters() {
            return ParametersInfo.builder().addParameter(TEMPLATE).build();
        }

        @Override
        public EvalSectionHelper initialize(SectionInitContext context) {
            Map<String, Expression> params = new HashMap<>();
            for (Entry<String, String> entry : context.getParameters().entrySet()) {
                params.put(entry.getKey(), context.getExpression(entry.getKey()));
            }
            return new EvalSectionHelper(params, context.getEngine());
        }

        @Override
        public Scope initializeBlock(Scope outerScope, BlockInfo block) {
            if (block.getLabel().equals(MAIN_BLOCK_NAME)) {
                for (Entry<String, String> entry : block.getParameters().entrySet()) {
                    block.addExpression(entry.getKey(), entry.getValue());
                }
            }
            return outerScope;
        }

    }

}
