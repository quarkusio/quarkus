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
