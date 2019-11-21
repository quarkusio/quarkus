package io.quarkus.qute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class IncludeSectionHelper implements SectionHelper {

    private static final String TEMPLATE = "template";

    private final Supplier<Template> templateSupplier;
    private final Map<String, SectionBlock> extendingBlocks;

    public IncludeSectionHelper(Supplier<Template> templateSupplier, Map<String, SectionBlock> extendingBlocks) {
        this.templateSupplier = templateSupplier;
        this.extendingBlocks = extendingBlocks;
    }

    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        return ((TemplateImpl) templateSupplier.get()).root.resolve(context.resolutionContext().createChild(extendingBlocks));
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
            for (SectionBlock block : context.getBlocks().subList(1, context.getBlocks().size())) {
                extendingBlocks.put(block.label, block);
            }
            return new IncludeSectionHelper(new Supplier<Template>() {

                @Override
                public Template get() {
                    String name = context.getParameter(TEMPLATE);
                    Template template = context.getEngine().getTemplate(name);
                    if (template == null) {
                        throw new IllegalStateException("Template not found: " + name);
                    }
                    return template;
                }
            }, extendingBlocks);
        }

    }

}
