package io.quarkus.qute;

import java.util.concurrent.CompletionStage;

/**
 * Defines the logic of a section node.
 */
@FunctionalInterface
public interface SectionHelper {

    /**
     * 
     * @param context
     * @return the result node
     */
    CompletionStage<ResultNode> resolve(SectionResolutionContext context);

    /**
     * 
     */
    public interface SectionResolutionContext {

        ResolutionContext resolutionContext();

        /**
         * Execute the main block with the current resolution context.
         * 
         * @return the result node
         */
        default CompletionStage<ResultNode> execute() {
            return execute(null, resolutionContext());
        }

        /**
         * Execute the main block with the specified {@link ResolutionContext}.
         * 
         * @param context
         * @return the result node
         */
        default CompletionStage<ResultNode> execute(ResolutionContext context) {
            return execute(null, context);
        }

        /**
         * Execute the specified block with the specified {@link ResolutionContext}.
         * 
         * @param block
         * @param context
         * @return the result node
         */
        CompletionStage<ResultNode> execute(SectionBlock block, ResolutionContext context);

    }

}
