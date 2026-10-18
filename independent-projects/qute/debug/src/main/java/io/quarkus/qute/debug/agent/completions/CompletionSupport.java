package io.quarkus.qute.debug.agent.completions;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.debug.CompletionItem;
import org.eclipse.lsp4j.debug.CompletionItemType;
import org.eclipse.lsp4j.debug.CompletionsArguments;
import org.eclipse.lsp4j.debug.CompletionsResponse;

import io.quarkus.qute.debug.agent.DebuggeeAgent;
import io.quarkus.qute.debug.agent.frames.RemoteStackFrame;
import io.quarkus.qute.debug.agent.resolvers.ValueResolverRegistry;

/**
 * Provides support for code completions within the Qute template debugger.
 * <p>
 * This class is responsible for generating completion items for expressions at a given
 * caret position inside a template, taking into account the current stack frame and
 * the available variables, properties, and resolvers.
 * </p>
 */
public class CompletionSupport {

    /** Empty array used as default for completions. */
    public static final CompletionItem[] EMPTY_COMPLETION_ITEMS = new CompletionItem[0];

    /** Reference to the debuggee agent managing threads, frames, and variables. */
    private final DebuggeeAgent agent;

    public CompletionSupport(DebuggeeAgent agent) {
        this.agent = agent;
    }

    /**
     * Computes code completions for a given template expression and caret position.
     *
     * @param args the completion arguments containing frameId, text, column
     * @return a CompletableFuture resolving to a {@link CompletionsResponse} containing possible completions
     */
    public CompletableFuture<CompletionsResponse> completions(CompletionsArguments args) {
        if (agent.isEnabled()) {
            Integer frameId = args.getFrameId();
            var stackFrame = agent.findStackFrame(frameId);
            if (stackFrame != null) {
                // Determine the base object for the expression at the caret
                CompletableFuture<Object> baseObject = getBaseObject(args.getText(), args.getColumn() - 1, stackFrame);
                if (baseObject != null) {
                    // Evaluate the base object asynchronously
                    return baseObject.handle((base, error) -> {
                        if (base == null || error != null) {
                            // If evaluation fails, fallback to root scope completions
                            return getRootCompletions(stackFrame);
                        }
                        // Create a CompletionContext for the evaluated base
                        CompletionContext context = new CompletionContext(base, stackFrame);
                        // Fill completion items using registered value resolvers
                        ValueResolverRegistry.getInstance().fillWithValueResolvers(context);
                        return context.toResponse();
                    });
                } else {
                    // No base object found → return root scope completions
                    CompletionsResponse response = getRootCompletions(stackFrame);
                    return CompletableFuture.completedFuture(response);
                }
            }
        }
        // Debugger disabled or frame not found → return empty completions
        CompletionsResponse response = new CompletionsResponse();
        response.setTargets(EMPTY_COMPLETION_ITEMS);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Generates completions for the root scope (variables in all accessible scopes).
     *
     * @param stackFrame the current stack frame
     * @return a {@link CompletionsResponse} containing variable names as completion items
     */
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

    /**
     * Evaluates the base object expression before the caret for completions.
     *
     * @param text the full template expression
     * @param column the caret column position (0-based)
     * @param stackFrame the current stack frame
     * @return a CompletableFuture containing the evaluated base object, or null if none
     */
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
     * Extracts the receiver expression before the caret for code completion purposes.
     * <p>
     * Examples:
     * <ul>
     * <li>"items.get(items.<caret>size)" → returns "items"</li>
     * <li>"items.size > 10 && items.<caret>" → returns "items"</li>
     * <li>"items.foo.bar.in<caret>dex" → returns "items.foo.bar"</li>
     * </ul>
     * </p>
     *
     * @param expression the full expression from the template
     * @param caretOffset the caret position where completion is invoked
     * @return the receiver expression before the caret, or null if none
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

        // Walk backward from caret to find the start of the base expression
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
