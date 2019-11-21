package io.quarkus.qute;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * 
 */
public interface ResolutionContext {

    /**
     * 
     * @param expression
     * @return the result of the evaluated expression
     */
    CompletionStage<Object> evaluate(String expression);

    /**
     * 
     * @param expression
     * @return the result of the evaluated expression
     */
    CompletionStage<Object> evaluate(Expression expression);

    /**
     * 
     * @param data
     * @param namespaceResolversFactories
     * @return a new child resolution context
     */
    ResolutionContext createChild(Object data, List<NamespaceResolver> namespaceResolvers);

    /**
     * 
     * @param extendingBlocks
     * @return a new child resolution context
     */
    ResolutionContext createChild(Map<String, SectionBlock> extendingBlocks);

    /**
     * 
     * @return the data
     */
    Object getData();

    /**
     * 
     * @return the namespace resolvers
     */
    List<NamespaceResolver> getNamespaceResolvers();

    /**
     * 
     * @return the parent context or null
     */
    ResolutionContext getParent();

    /**
     * 
     * @param name
     * @return the extending block or null
     */
    SectionBlock getExtendingBlock(String name);

}
