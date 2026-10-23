package io.quarkus.qute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.quarkus.qute.TemplateNode.Origin;

public class EvalSectionHelper implements SectionHelper {

    public static final String EVAL = "eval";
    private static final String TEMPLATE = "template";

    private final Map<String, Expression> parameters;
    private final Engine engine;

    public EvalSectionHelper(Map<String, Expression> parameters, Engine engine) {
        this.parameters = parameters;
        this.engine = engine;
    }

    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        if (parameters.size() > 1) {
            return context.evaluate(parameters)
                    .thenCompose(evaluatedParams -> {
                        // Parse the template and execute with the params as the root context object
                        String contents = evaluatedParams.get(TEMPLATE).toString();
                        return parseAndResolve(contents,
                                context.resolutionContext().createChild(Mapper.wrap(evaluatedParams), null));
                    });
        } else {
            Expression contents = parameters.get(TEMPLATE);
            if (contents.isLiteral()) {
                return parseAndResolve(contents.getLiteral().toString(), context.resolutionContext());
            } else {
                return context.evaluate(contents)
                        .thenCompose(r -> parseAndResolve(r.toString(), context.resolutionContext()));
            }
        }
    }

    private CompletionStage<ResultNode> parseAndResolve(String contents, ResolutionContext resolutionContext) {
        try {
            Template template = engine.parse(contents, resolutionContext.getTemplate().getVariant().orElse(null));
            return template.getRootNode().resolve(resolutionContext);
        } catch (TemplateException e) {
            Origin origin = parameters.get(TEMPLATE).getOrigin();
            return CompletableFuture.failedStage(TemplateException.builder()
                    .message(
                            "Parser error in the evaluated template: {templateId} line {line}:\\n\\t{originalMessage}")
                    .code(Code.ERROR_IN_EVALUATED_TEMPLATE)
                    .argument("templateId",
                            origin.hasNonGeneratedTemplateId() ? " template [" + origin.getTemplateId() + "]"
                                    : "")
                    .argument("line", origin.getLine())
                    .argument("originalMessage", e.getMessage())
                    .build());
        }
    }

    public static class Factory implements SectionHelperFactory<EvalSectionHelper> {

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of(EVAL);
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

    enum Code implements ErrorCode {

        ERROR_IN_EVALUATED_TEMPLATE,

        ;

        @Override
        public String getName() {
            return "EVAL_" + name();
        }

    }

}
