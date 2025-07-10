package io.quarkus.qute.debug.agent.completions;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.debug.CompletionItem;
import org.eclipse.lsp4j.debug.CompletionItemType;
import org.eclipse.lsp4j.debug.CompletionsArguments;
import org.eclipse.lsp4j.debug.CompletionsResponse;

import io.quarkus.qute.debug.agent.DebuggeeAgent;
import io.quarkus.qute.debug.agent.RemoteStackFrame;
import io.quarkus.qute.debug.agent.resolvers.ValueResolverRegistry;

public class CompletionSupport {

    public static final CompletionItem[] EMPTY_COMPLETION_ITEMS = new CompletionItem[0];

    private final DebuggeeAgent agent;

    public CompletionSupport(DebuggeeAgent agent) {
        this.agent = agent;
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
                                ValueResolverRegistry.getInstance().fillWithValueResolvers(context);
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
        String baseExpression = extractReceiverBeforeCaret(text, column - 1);
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

        boolean lastPart = true;
        int startOffset = -1;
        int endOffset = -1;
        int nbBracket = 0;
        int nbSquareBracket = 0;
        boolean inString = false;
        for (int i = caretOffset; i >= 0; i--) {
            char c = expression.charAt(i);
            if (c == '\'') {
                inString = !inString;
            } else {
                if (Character.isJavaIdentifierPart(c)) {
                    if (endOffset == -1) {
                        endOffset = i;
                    }
                } else if (!inString) {
                    if (c == '.') {
                        if (lastPart) {
                            lastPart = false;
                            endOffset = i;
                        }
                    } else {
                        if (c == ')') {
                            nbBracket++;
                        } else if (c == '(') {
                            if (nbBracket == 0) {
                                startOffset = i + 1;
                                break;
                            }
                            nbBracket--;
                        } else if (c == ']') {
                            nbSquareBracket++;
                        } else if (c == '[') {
                            if (nbSquareBracket == 0) {
                                startOffset = i + 1;
                                break;
                            }
                            nbSquareBracket--;
                        } else if (c == '\'') {
                            inString = !inString;
                        } else {
                            startOffset = i + 1;
                            break;
                        }
                    }
                }

            }
        }
        if (endOffset == -1) {
            return null;
        }

        if (startOffset == -1) {
            startOffset = 0;
        }
        return expression.substring(startOffset, endOffset);
    }

}
