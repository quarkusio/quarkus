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

    private static final String NESTED_CONTENT = "nested-content";

    private final Supplier<Template> templateSupplier;
    private final Map<String, Expression> parameters;
    private final boolean isEmpty;
    private final boolean isIsolated;

    UserTagSectionHelper(Supplier<Template> templateSupplier, Map<String, Expression> parameters, boolean isEmpty,
            boolean isIsolated) {
        this.templateSupplier = templateSupplier;
        this.parameters = parameters;
        this.isEmpty = isEmpty;
        this.isIsolated = isIsolated;
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
                    ResolutionContext resolutionContext;
                    // Execute the template with the params as the root context object
                    Object data = Mapper.wrap(evaluatedParams);
                    if (isIsolated) {
                        resolutionContext = context.newResolutionContext(data, null);
                    } else {
                        resolutionContext = context.resolutionContext().createChild(data, null);
                    }
                    TemplateImpl tagTemplate = (TemplateImpl) templateSupplier.get();
                    tagTemplate.root.resolve(resolutionContext)
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

        private static final String IT = "it";
        private static final String ISOLATED = "_isolated";
        private static final String ISOLATED_DEFAULT_VALUE = "false";

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
            return ParametersInfo.builder()
                    .addParameter(Parameter.builder(IT).defaultValue(IT))
                    .addParameter(Parameter.builder(ISOLATED).defaultValue(ISOLATED_DEFAULT_VALUE).optional()
                            .valuePredicate(v -> ISOLATED.equals(v)))
                    .build();
        }

        @Override
        public Scope initializeBlock(Scope outerScope, BlockInfo block) {
            if (block.getLabel().equals(MAIN_BLOCK_NAME)) {
                for (Entry<String, String> entry : block.getParameters().entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    // {#myTag _isolated=true /}
                    if (key.equals(ISOLATED)
                            // {#myTag _isolated /}
                            || value.equals(ISOLATED)
                            // IT with default value, e.g. {#myTag foo=bar /}
                            || (key.equals(IT) && value.equals(IT))) {
                        continue;
                    } else if (useDefaultedKey(key, value)) {
                        // As "order" in {#myTag user order /}
                        key = value;
                    }
                    block.addExpression(key, value);
                }
                return outerScope;
            } else {
                return outerScope;
            }
        }

        @Override
        public UserTagSectionHelper initialize(SectionInitContext context) {
            Map<String, Expression> params = new HashMap<>();
            boolean isolatedValue = false;
            for (Entry<String, String> entry : context.getParameters().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value.equals(ISOLATED)) {
                    isolatedValue = true;
                    continue;
                } else if (key.equals(ISOLATED)) {
                    continue;
                } else if (key.equals(IT)) {
                    if (value.equals(IT)) {
                        continue;
                    } else if (isSinglePart(value)) {
                        // Also register the param with a defaulted key
                        params.put(value, context.getExpression(key));
                    }
                } else if (useDefaultedKey(key, value)) {
                    key = value;
                }
                params.put(key, context.getExpression(key));
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
            }, params, isEmpty, isolatedValue ? true
                    : Boolean.parseBoolean(context.getParameterOrDefault(ISOLATED, ISOLATED_DEFAULT_VALUE)));
        }

        private boolean useDefaultedKey(String key, String value) {
            // Use the defaulted key if:
            // 1. The key is a generated id
            // 2. The value is a single identifier
            // {#myTag user} should be equivalent to {#myTag user=user}
            return LiteralSupport.INTEGER_LITERAL_PATTERN.matcher(key).matches()
                    && isSinglePart(value);
        }

        private boolean isSinglePart(String value) {
            return Expressions.splitParts(value).size() == 1;
        }

    }

}
