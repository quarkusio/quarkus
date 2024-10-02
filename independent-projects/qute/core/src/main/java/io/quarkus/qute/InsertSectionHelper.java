package io.quarkus.qute;

import java.util.List;
import java.util.concurrent.CompletionStage;

public class InsertSectionHelper implements SectionHelper {

    private final String name;
    private final SectionBlock defaultBlock;

    public InsertSectionHelper(String name, SectionBlock defaultBlock) {
        this.name = name;
        this.defaultBlock = defaultBlock;
    }

    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        // Note that {#insert} is evaluated on the current resolution context
        // Therefore, we need to try to find the "correct" parent context to avoid stack
        // overflow errors when using the same block names
        ResolutionContext rc = findParentResolutionContext(context.resolutionContext());
        if (rc == null) {
            // No parent context found - use the current
            rc = context.resolutionContext();
        }
        SectionBlock extending = rc.getExtendingBlock(name);
        if (extending != null) {
            return context.execute(extending, rc);
        } else {
            return context.execute(defaultBlock, rc);
        }
    }

    private ResolutionContext findParentResolutionContext(ResolutionContext context) {
        if (context.getParent() == null) {
            return null;
        }
        // Let's iterate over all extending blocks and try to find the "correct" parent context
        // The "correct" parent context is the parent of a context that contains this helper
        // instance in any of its extending block
        SectionBlock block = context.getCurrentExtendingBlock(name);
        if (block != null && block.findNode(this::containsThisHelperInstance) != null) {
            return context.getParent();
        }
        return findParentResolutionContext(context.getParent());
    }

    private boolean containsThisHelperInstance(TemplateNode node) {
        return node.isSection() && node.asSection().helper == this;
    }

    public static class Factory implements SectionHelperFactory<InsertSectionHelper> {

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of("insert");
        }

        @Override
        public ParametersInfo getParameters() {
            return ParametersInfo.builder().addParameter("name", IncludeSectionHelper.DEFAULT_NAME).build();
        }

        @Override
        public InsertSectionHelper initialize(SectionInitContext context) {
            String name = context.getParameter("name");
            if (context.getEngine().getSectionHelperFactories().containsKey(name)) {
                throw context.error(
                        "\\{#insert} defined in the \\{#include\\} conflicts with an existing section/tag: {name}")
                        .code(Code.INSERT_SECTION_CONFLICT)
                        .argument("name", name)
                        .origin(context.getOrigin())
                        .build();
            }
            return new InsertSectionHelper(name, context.getBlocks().get(0));
        }

    }

    enum Code implements ErrorCode {

        INSERT_SECTION_CONFLICT,
        ;

        @Override
        public String getName() {
            return "INSERT_" + name();
        }

    }

}
