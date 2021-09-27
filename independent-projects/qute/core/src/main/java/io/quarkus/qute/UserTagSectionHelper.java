package io.quarkus.qute;

import static io.quarkus.qute.Futures.evaluateParams;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class UserTagSectionHelper implements SectionHelper {

    private static final String IT = "it";
    private static final String NESTED_CONTENT = "nested-content";

    private final Supplier<Template> templateSupplier;
    private final Map<String, Expression> parameters;
    private final boolean isEmpty;

    UserTagSectionHelper(Supplier<Template> templateSupplier, Map<String, Expression> parameters, boolean isEmpty) {
        this.templateSupplier = templateSupplier;
        this.parameters = parameters;
        this.isEmpty = isEmpty;
    }

    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        CompletableFuture<ResultNode> result = new CompletableFuture<>();
        evaluateParams(parameters, context.resolutionContext()).whenComplete((evaluatedParams, t1) -> {
            if (t1 != null) {
                result.completeExceptionally(t1);
            } else {
                if (!isEmpty) {
                    // Execute the nested content first and make it accessible via the "nested-content" key 
                    evaluatedParams.put(NESTED_CONTENT,
                            context.execute(
                                    context.resolutionContext().createChild(Mapper.wrap(evaluatedParams), null)));
                }
                try {
                    // Execute the template with the params as the root context object
                    TemplateImpl tagTemplate = (TemplateImpl) templateSupplier.get();
                    tagTemplate.root.resolve(context.resolutionContext().createChild(Mapper.wrap(evaluatedParams), null))
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

    public static class Factory implements SectionHelperFactory<UserTagSectionHelper> {

        private final String name;
        private final String templateId;

        /**
         * 
         * @param name Identifies the tag
         * @param templateId Used to locate the template
         */
        public Factory(String name, String templateId) {
            this.name = name;
            this.templateId = templateId;
        }

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of(name);
        }

        @Override
        public ParametersInfo getParameters() {
            return ParametersInfo.builder().addParameter(new Parameter(IT, IT, false)).build();
        }

        @Override
        public Scope initializeBlock(Scope outerScope, BlockInfo block) {
            if (block.getLabel().equals(MAIN_BLOCK_NAME)) {
                for (Entry<String, String> entry : block.getParameters().entrySet()) {
                    if (entry.getKey().equals(IT) && entry.getValue().equals(IT)) {
                        continue;
                    }
                    block.addExpression(entry.getKey(), entry.getValue());
                }
                return outerScope;
            } else {
                return outerScope;
            }
        }

        @Override
        public UserTagSectionHelper initialize(SectionInitContext context) {
            Map<String, Expression> params = new HashMap<>();
            for (Entry<String, String> entry : context.getParameters().entrySet()) {
                if (entry.getKey().equals(IT) && entry.getValue().equals(IT)) {
                    continue;
                }
                params.put(entry.getKey(), context.getExpression(entry.getKey()));
            }
            boolean isEmpty = context.getBlocks().size() == 1 && context.getBlocks().get(0).isEmpty();
            final Engine engine = context.getEngine();

            return new UserTagSectionHelper(new Supplier<Template>() {

                @Override
                public Template get() {
                    Template template = engine.getTemplate(templateId);
                    if (template == null) {
                        throw new TemplateException("Tag template not found: " + templateId);
                    }
                    return template;
                }
            }, params, isEmpty);
        }

    }

}
