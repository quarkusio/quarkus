package io.quarkus.qute;

import static io.quarkus.qute.Futures.evaluateParams;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class UserTagSectionHelper implements SectionHelper {

    private static final String IT = "it";

    private final Supplier<Template> templateSupplier;
    private final Map<String, Expression> parameters;

    public UserTagSectionHelper(Supplier<Template> templateSupplier, Map<String, Expression> parameters) {
        this.templateSupplier = templateSupplier;
        this.parameters = parameters;
    }

    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        CompletableFuture<ResultNode> result = new CompletableFuture<>();
        evaluateParams(parameters, context.resolutionContext()).whenComplete((r1, t1) -> {
            if (t1 != null) {
                result.completeExceptionally(t1);
            } else {
                // Execute the template with the params as the root context object
                try {
                    TemplateImpl tagTemplate = (TemplateImpl) templateSupplier.get();
                    tagTemplate.root.resolve(context.resolutionContext().createChild(r1, null))
                            .whenComplete((r2, t2) -> {
                                if (t2 != null) {
                                    result.completeExceptionally(t2);
                                } else {
                                    result.complete(r2);
                                }
                            });
                } catch (Exception e) {
                    result.completeExceptionally(e);
                }

            }
        });
        return result;
    }

    public static class Factory implements SectionHelperFactory<UserTagSectionHelper> {

        private final String name;

        public Factory(String name) {
            this.name = name;
        }

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of(name);
        }

        @Override
        public ParametersInfo getParameters() {
            return ParametersInfo.builder().addParameter(new Parameter(IT, "it", false)).build();
        }

        @Override
        public UserTagSectionHelper initialize(SectionInitContext context) {

            Map<String, Expression> params = context.getParameters().entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey(), e -> context.parseValue(e.getValue())));

            return new UserTagSectionHelper(new Supplier<Template>() {

                @Override
                public Template get() {
                    Template template = context.getEngine().getTemplate(name);
                    if (template == null) {
                        throw new IllegalStateException("Tag template not found: " + name);
                    }
                    return template;
                }
            }, params);
        }

    }

}
