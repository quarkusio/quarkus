package io.quarkus.qute;

import io.quarkus.qute.TemplateNode.Origin;
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
        SectionBlock extending = context.resolutionContext().getExtendingBlock(name);
        if (extending != null) {
            return context.execute(extending, context.resolutionContext());
        } else {
            return context.execute(defaultBlock, context.resolutionContext());
        }
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
                Origin origin = context.getOrigin();
                StringBuilder msg = new StringBuilder("An {#insert} section defined in the {#include} section");
                origin.appendTo(msg);
                msg.append(" conflicts with an existing section/tag: ").append(name);
                throw new TemplateException(origin, msg.toString());
            }
            return new InsertSectionHelper(name, context.getBlocks().get(0));
        }

    }

}
