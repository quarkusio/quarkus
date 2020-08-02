package io.quarkus.qute;

import io.quarkus.qute.SectionHelperFactory.SectionInitContext;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Basic {@code with} statement.
 */
public class WithSectionHelper implements SectionHelper {

    private static final String OBJECT = "object";
    private static final String WITH = "with";

    private final Expression object;
    private final SectionBlock main;

    WithSectionHelper(SectionInitContext context) {
        this.object = context.getExpression(OBJECT);
        this.main = context.getBlocks().get(0);
    }

    @Override
    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
        return context.resolutionContext().evaluate(object)
                .thenCompose(with -> {
                    return context.execute(main, context.resolutionContext().createChild(with, null));
                });
    }

    public static class Factory implements SectionHelperFactory<WithSectionHelper> {

        @Override
        public List<String> getDefaultAliases() {
            return ImmutableList.of(WITH);
        }

        @Override
        public ParametersInfo getParameters() {
            return ParametersInfo.builder().addParameter(OBJECT).build();
        }

        @Override
        public WithSectionHelper initialize(SectionInitContext context) {
            return new WithSectionHelper(context);
        }

        @Override
        public Scope initializeBlock(Scope previousScope, BlockInfo block) {
            if (block.getLabel().equals(MAIN_BLOCK_NAME)) {
                String object = block.getParameters().get(OBJECT);
                if (object == null) {
                    throw new IllegalStateException("Object param not present");
                }
                block.addExpression(OBJECT, object);
                return previousScope;
            } else {
                return previousScope;
            }
        }

    }
}
