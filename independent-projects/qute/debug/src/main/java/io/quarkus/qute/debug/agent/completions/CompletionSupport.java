package io.quarkus.qute.debug.agent.completions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.debug.CompletionItem;
import org.eclipse.lsp4j.debug.CompletionItemType;
import org.eclipse.lsp4j.debug.CompletionsArguments;
import org.eclipse.lsp4j.debug.CompletionsResponse;

import io.quarkus.qute.ValueResolver;
import io.quarkus.qute.debug.agent.DebuggeeAgent;
import io.quarkus.qute.debug.agent.RemoteStackFrame;
import io.quarkus.qute.debug.agent.completions.resolvers.CollectionResolverCollector;
import io.quarkus.qute.debug.agent.completions.resolvers.ListResolverCollector;
import io.quarkus.qute.debug.agent.completions.resolvers.ReflectionValueResolverCollector;
import io.quarkus.qute.debug.agent.completions.resolvers.ResolverCollector;
import io.quarkus.qute.debug.agent.completions.resolvers.ThisResolverCollector;

public class CompletionSupport {

    public static final CompletionItem[] EMPTY_COMPLETION_ITEMS = new CompletionItem[0];

    private final Map<String /* class name */, ResolverCollector> valueResolverCollectors;

    private final DebuggeeAgent agent;

    public CompletionSupport(DebuggeeAgent agent) {
        this.agent = agent;
        this.valueResolverCollectors = initializeCollectors();
    }

    private Map<String, ResolverCollector> initializeCollectors() {
        Map<String /* class name */, ResolverCollector> valueResolverCollectors = new HashMap<>();
        registerCollector(new CollectionResolverCollector(), valueResolverCollectors);
        registerCollector(new ListResolverCollector(), valueResolverCollectors);
        registerCollector(new ThisResolverCollector(), valueResolverCollectors);
        registerCollector(new ReflectionValueResolverCollector(), valueResolverCollectors);
        return valueResolverCollectors;
    }

    private static void registerCollector(ResolverCollector resolverCollector,
            Map<String, ResolverCollector> valueResolverCollectors) {
        valueResolverCollectors.put(resolverCollector.getClassName(), resolverCollector);
    }

    public CompletableFuture<CompletionsResponse> completions(CompletionsArguments args) {
        if (agent.isEnabled()) {
            Integer frameId = args.getFrameId();
            var stackFrame = agent.findStackFrame(frameId);
            if (stackFrame != null) {
                CompletableFuture<Object> baseObject = getBaseObject(args.getText(), args.getColumn() - 1, stackFrame);
                if (baseObject != null) {
                    // Returns method/property part of base object (ex: items.size -> returns items,
                    // items_count -> returns null)
                    return baseObject //
                            .handle((base, error) -> {
                                if (base == null || error != null) {
                                    return getRootCompletions(stackFrame);
                                }
                                CompletionContext context = new CompletionContext(base, stackFrame);
                                // Fill with value resolvers
                                fillWithValueResolvers(context);
                                return context.toResponse();
                            });
                } else {
                    // Returns root object completions (ex: items, items_count, item, etc)
                    CompletionsResponse response = getRootCompletions(stackFrame);
                    return CompletableFuture.completedFuture(response);
                }
            }
        }
        // Returns empty completions
        CompletionsResponse response = new CompletionsResponse();
        response.setTargets(EMPTY_COMPLETION_ITEMS);
        return CompletableFuture.completedFuture(response);
    }

    private void fillWithValueResolvers(CompletionContext context) {
        var base = context.getBase();
        var stackFrame = context.getStackFrame();
        var engine = stackFrame.getEngine();
        var evalContext = stackFrame.createEvalContext(base);
        var valueResolvers = engine.getValueResolvers();
        for (ValueResolver valueResolver : valueResolvers) {
            String className = valueResolver.getClass().getName();
            var collector = valueResolverCollectors.get(className);
            if (collector != null && collector.isApplicable(valueResolver, evalContext)) {
                collector.collect(context);
            }
        }
    }

    private CompletionsResponse getRootCompletions(RemoteStackFrame stackFrame) {
        CompletionContext context = new CompletionContext(null, stackFrame);
        for (var scope : stackFrame.getScopes()) {
            for (var variable : scope.getVariables()) {
                CompletionItem item = new CompletionItem();
                item.setLabel(variable.getName());
                item.setType(CompletionItemType.REFERENCE);
                context.add(item);
            }
        }

        return context.toResponse();
    }

    private CompletableFuture<Object> getBaseObject(String text, int column, RemoteStackFrame stackFrame) {
        if (text == null || text.isBlank() || column < 0 || column > text.length()) {
            return null;
        }
        String baseExpression = extractReceiverBeforeCaret(text, column);
        if (baseExpression == null) {
            return null;
        }
        return stackFrame.evaluate(baseExpression);
    }

    /**
     * Extracts the receiver expression before the caret for code completion
     * purposes. For example, in: - "items.get(items.<caret>size)" → returns "items"
     * - "items.size > 10 && items.<caret>" → returns "items" -
     * "items.foo.bar.in<caret>dex" → returns "items.foo.bar"
     *
     * This function parses the expression up to the caret, walks backward to find
     * the full access chain (e.g. a.b.c), and returns the chain without the partial
     * identifier currently being typed.
     *
     * @param expression the full source expression (e.g. from the template)
     * @param caretOffset the caret position where completion is invoked
     * @return the receiver expression before the caret, or null if none found
     */
    public static String extractReceiverBeforeCaret(String expression, int caretOffset) {
        if (caretOffset > expression.length() || caretOffset <= 0) {
            return null;
        }

        // Work only on the part before the caret
        String beforeCaret = expression.substring(0, caretOffset);

        int end = beforeCaret.length();
        int start = end;

        // Walk back to find the full access chain (e.g. items.size.in)
        while (start > 0) {
            char c = beforeCaret.charAt(start - 1);
            if (Character.isJavaIdentifierPart(c) || c == '.') {
                start--;
            } else {
                break; // Stop at non-access characters
            }
        }

        String accessChain = beforeCaret.substring(start, end).trim();

        // If empty or doesn't end with identifier, return null
        if (accessChain.isEmpty() || accessChain.equals(".")) {
            return null;
        }

        // Remove partial identifier after last dot (e.g. "items.size.in" ->
        // "items.size")
        int lastDot = accessChain.lastIndexOf('.');
        if (lastDot >= 0) {
            return accessChain.substring(0, lastDot);
        }

        // If no dot, return the identifier (e.g. "items")
        return accessChain;
    }

}
