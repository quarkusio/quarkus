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

    private static final String TEMPLATE = "template";

    private final Supplier<Template> templateSupplier;
    private final Map<String, SectionBlock> extendingBlocks;
    private final Map<String, Expression> parameters;

    public IncludeSectionHelper(Supplier<Template> templateSupplier, Map<String, SectionBlock> extendingBlocks,
            Map<String, Expression> parameters) {
        this.templateSupplier = templateSupplier;
        this.extendingBlocks = extendingBlocks;
        this.parameters = parameters;
    }

    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        if (parameters.isEmpty()) {
            return ((TemplateImpl) templateSupplier.get()).root
                    .resolve(context.resolutionContext().createChild(null, null, extendingBlocks));
        } else {
            CompletableFuture<ResultNode> result = new CompletableFuture<>();
            evaluateParams(parameters, context.resolutionContext()).whenComplete((evaluatedParams, t1) -> {
                if (t1 != null) {
                    result.completeExceptionally(t1);
                } else {
                    try {
                        // Execute the template with the params as the root context object
                        TemplateImpl tagTemplate = (TemplateImpl) templateSupplier.get();
                        tagTemplate.root
                                .resolve(context.resolutionContext().createChild(evaluatedParams, null, extendingBlocks))
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

    public static class Factory implements SectionHelperFactory<IncludeSectionHelper> {

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of("include");
        }

        @Override
        public ParametersInfo getParameters() {
            return ParametersInfo.builder().addParameter(TEMPLATE).build();
        }

        @Override
        public boolean treatUnknownSectionsAsBlocks() {
            return true;
        }

        @Override
        public IncludeSectionHelper initialize(SectionInitContext context) {

            Map<String, SectionBlock> extendingBlocks = new HashMap<>();
            if (context.getBlocks().size() > 1) {
                for (SectionBlock block : context.getBlocks().subList(1, context.getBlocks().size())) {
                    extendingBlocks.put(block.label, block);
                }
            }

            Map<String, Expression> params;
            if (context.getParameters().size() == 1) {
                params = Collections.emptyMap();
            } else {
                params = new HashMap<>();
                for (Entry<String, String> entry : context.getParameters().entrySet()) {
                    if (!entry.getKey().equals(TEMPLATE)) {
                        params.put(entry.getKey(), context.parseValue(entry.getValue()));
                    }
                }
            }

            String templateParam = context.getParameter(TEMPLATE);
            if (LiteralSupport.isStringLiteralSeparator(templateParam.charAt(0))) {
                templateParam = templateParam.substring(1, templateParam.length() - 1);
            }
            final String templateId = templateParam;
            final Engine engine = context.getEngine();

            return new IncludeSectionHelper(new Supplier<Template>() {
                @Override
                public Template get() {
                    Template template = engine.getTemplate(templateId);
                    if (template == null) {
                        throw new TemplateException("Template not found: " + templateId);
                    }
                    return template;
                }
            }, extendingBlocks, params);
        }

    }

}
