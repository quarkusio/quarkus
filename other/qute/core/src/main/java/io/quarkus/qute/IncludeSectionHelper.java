package io.quarkus.qute;

import static io.quarkus.qute.Futures.evaluateParams;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class IncludeSectionHelper implements SectionHelper {

    static final String DEFAULT_NAME = "$default$";
    private static final String TEMPLATE = "template";

    protected final Supplier<Template> template;
    protected final Map<String, SectionBlock> extendingBlocks;
    protected final Map<String, Expression> parameters;
    protected final boolean isIsolated;

    public IncludeSectionHelper(Supplier<Template> templateSupplier, Map<String, SectionBlock> extendingBlocks,
            Map<String, Expression> parameters, boolean isIsolated) {
        this.template = templateSupplier;
        this.extendingBlocks = extendingBlocks;
        this.parameters = parameters;
        this.isIsolated = isIsolated;
    }

    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        if (parameters.isEmpty() && optimizeIfNoParams()) {
            // No params
            SectionNode root = ((TemplateImpl) template.get()).root;
            if (isIsolated) {
                return root.resolve(context.newResolutionContext(null, extendingBlocks));
            } else if (extendingBlocks.isEmpty()) {
                // No params and no extending blocks: {#include foo /}
                return root.resolve(context.resolutionContext());
            } else {
                return root.resolve(context.resolutionContext().createChild(null, extendingBlocks));
            }
        } else {
            CompletableFuture<ResultNode> result = new CompletableFuture<>();
            evaluateParams(parameters, context.resolutionContext()).whenComplete((evaluatedParams, t1) -> {
                if (t1 != null) {
                    result.completeExceptionally(t1);
                } else {
                    addAdditionalEvaluatedParams(context, evaluatedParams);
                    try {
                        ResolutionContext resolutionContext;
                        // Execute the template with the params as the root context object
                        Object data = Mapper.wrap(evaluatedParams);
                        if (isIsolated) {
                            resolutionContext = context.newResolutionContext(data, extendingBlocks);
                        } else {
                            resolutionContext = context.resolutionContext().createChild(data, extendingBlocks);
                        }
                        SectionNode root = ((TemplateImpl) template.get()).root;
                        // Execute the template with the params as the root context object
                        root.resolve(resolutionContext)
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
    }

    protected boolean optimizeIfNoParams() {
        return true;
    }

    protected void addAdditionalEvaluatedParams(SectionResolutionContext context, Map<String, Object> evaluatedParams) {
        // no-op
    }

    public static class Factory extends AbstractIncludeFactory<IncludeSectionHelper> {

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of("include");
        }

        @Override
        public ParametersInfo getParameters() {
            return ParametersInfo.builder().addParameter(TEMPLATE)
                    .addParameter(Parameter.builder(ISOLATED).defaultValue(ISOLATED_DEFAULT_VALUE).optional()
                            .valuePredicate(v -> ISOLATED.equals(v)))
                    .build();
        }

        @Override
        protected boolean ignoreParameterInit(String key, String value) {
            return key.equals(TEMPLATE)
                    // {#myTag _isolated=true /}
                    || key.equals(ISOLATED)
                    // {#myTag _isolated /}
                    || value.equals(ISOLATED);
        }

        @Override
        protected String getTemplateId(SectionInitContext context) {
            String templateParam = context.getParameter(TEMPLATE);
            if (LiteralSupport.isStringLiteralSeparator(templateParam.charAt(0))) {
                templateParam = templateParam.substring(1, templateParam.length() - 1);
            }
            return templateParam;
        }

        @Override
        protected IncludeSectionHelper newHelper(Supplier<Template> template, Map<String, Expression> params,
                Map<String, SectionBlock> extendingBlocks, boolean isolated, SectionInitContext context) {
            return new IncludeSectionHelper(template, extendingBlocks, params, isolated);
        }

    }

    static abstract class AbstractIncludeFactory<T extends SectionHelper> implements SectionHelperFactory<T> {

        static final String ISOLATED = "_isolated";
        static final String ISOLATED_DEFAULT_VALUE = "false";

        @Override
        public boolean treatUnknownSectionsAsBlocks() {
            return true;
        }

        @Override
        public Scope initializeBlock(Scope outerScope, BlockInfo block) {
            if (block.getLabel().equals(MAIN_BLOCK_NAME)) {
                for (Entry<String, String> entry : block.getParameters().entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (ignoreParameterInit(key, value)) {
                        continue;
                    } else if (useDefaultedKey(key, value)) {
                        // As "order" in {#include foo order /}
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
        public T initialize(SectionInitContext context) {
            boolean isEmpty = context.getBlocks().size() == 1 && context.getBlocks().get(0).isEmpty();
            boolean isolatedValue = false;
            Map<String, SectionBlock> extendingBlocks;

            if (isEmpty) {
                extendingBlocks = Collections.emptyMap();
            } else {
                extendingBlocks = new HashMap<>();
                for (SectionBlock block : context.getBlocks()) {
                    String name = block.id.equals(MAIN_BLOCK_NAME) ? DEFAULT_NAME : block.label;
                    if (extendingBlocks.put(name, block) != null) {
                        throw block.error("multiple blocks define the content for the \\{#insert\\} section of name [{name}]")
                                .code(Code.MULTIPLE_INSERTS_OF_NAME)
                                .origin(context.getOrigin())
                                .argument("name", name)
                                .build();
                    }
                }
            }

            Map<String, Expression> params;
            if (context.getParameters().size() == 1) {
                params = Collections.emptyMap();
            } else {
                params = new HashMap<>();
                for (Entry<String, String> entry : context.getParameters().entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (value.equals(ISOLATED)) {
                        isolatedValue = true;
                        continue;
                    }
                    handleParamInit(key, value, context, params);
                }
            }

            final String templateId = getTemplateId(context);
            final Engine engine = context.getEngine();
            Supplier<Template> template = new Supplier<Template>() {
                @Override
                public Template get() {
                    Template template = engine.getTemplate(templateId);
                    if (template == null) {
                        throw engine.error("included template [{templateId}] not found")
                                .code(Code.TEMPLATE_NOT_FOUND)
                                .argument("templateId", templateId)
                                .origin(context.getOrigin())
                                .build();
                    }
                    return template;
                }
            };

            return newHelper(template, params, extendingBlocks, isolatedValue ? true
                    : Boolean.parseBoolean(context.getParameterOrDefault(ISOLATED, ISOLATED_DEFAULT_VALUE)), context);
        }

        protected abstract String getTemplateId(SectionInitContext context);

        protected void handleParamInit(String key, String value, SectionInitContext context, Map<String, Expression> params) {
            if (ignoreParameterInit(key, value)) {
                return;
            } else if (useDefaultedKey(key, value)) {
                key = value;
            }
            params.put(key, context.getExpression(key));
        }

        protected boolean useDefaultedKey(String key, String value) {
            // Use the defaulted key if:
            // 1. The key is a generated id
            // 2. The value is a single identifier
            // {#myTag user} should be equivalent to {#myTag user=user}
            return LiteralSupport.INTEGER_LITERAL_PATTERN.matcher(key).matches()
                    && isSinglePart(value);
        }

        protected boolean isSinglePart(String value) {
            return Expressions.splitParts(value).size() == 1;
        }

        protected abstract boolean ignoreParameterInit(String key, String value);

        protected abstract T newHelper(Supplier<Template> template, Map<String, Expression> params,
                Map<String, SectionBlock> extendingBlocks, boolean isolated, SectionInitContext context);
    }

    enum Code implements ErrorCode {

        MULTIPLE_INSERTS_OF_NAME,

        TEMPLATE_NOT_FOUND,

        ;

        @Override
        public String getName() {
            return "INCLUDE_" + name();
        }

    }

}
