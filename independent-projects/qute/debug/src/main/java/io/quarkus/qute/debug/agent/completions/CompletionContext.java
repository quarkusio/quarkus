package io.quarkus.qute.debug.agent.completions;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.lsp4j.debug.CompletionItem;
import org.eclipse.lsp4j.debug.CompletionItemType;
import org.eclipse.lsp4j.debug.CompletionsResponse;

import io.quarkus.qute.debug.agent.frames.RemoteStackFrame;
import io.quarkus.qute.debug.agent.resolvers.ValueResolverContext;

/**
 * Context for collecting code completions in the debugger.
 * <p>
 * Implements {@link ValueResolverContext} and is used when the debugger
 * needs to provide completion suggestions in Qute templates.
 * </p>
 * <p>
 * Collects properties (fields and string-named) and methods (reflection
 * or string-named) of the {@link #base} object, storing them as
 * {@link CompletionItem}. Can be converted into a {@link CompletionsResponse}.
 * </p>
 */
public class CompletionContext implements ValueResolverContext {

    /** Empty array of completion items for DAP responses. */
    public static final CompletionItem[] EMPTY_COMPLETION_ITEMS = new CompletionItem[0];

    /** The object to inspect for completions. */
    private final Object base;

    /** Stack frame associated with this context. */
    private final RemoteStackFrame stackFrame;

    /** Collected completion items. */
    private final Collection<CompletionItem> targets;

    /** Prevents duplicate completion labels. */
    private final Set<String> existingNames;

    public CompletionContext(Object base, RemoteStackFrame stackFrame) {
        this.base = base;
        this.stackFrame = stackFrame;
        this.targets = new ArrayList<>();
        this.existingNames = new HashSet<>();
    }

    public Object getBase() {
        return base;
    }

    public RemoteStackFrame getStackFrame() {
        return stackFrame;
    }

    /** Converts collected items to a DAP {@link CompletionsResponse}. */
    public CompletionsResponse toResponse() {
        CompletionsResponse response = new CompletionsResponse();
        response.setTargets(targets.toArray(EMPTY_COMPLETION_ITEMS));
        return response;
    }

    @Override
    public void addMethod(Method method) {
        CompletionItem item = new CompletionItem();
        StringBuilder signature = new StringBuilder(method.getName());
        signature.append("(");

        var parameters = method.getParameters();
        int selectionStart = parameters.length > 0 ? signature.length() : -1;
        int selectionLength = -1;

        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                signature.append(",");
            }
            String arg = parameters[i].getName();
            signature.append(arg);
            if (i == 0) {
                selectionLength = arg.length();
            }
        }
        signature.append(")");
        item.setLabel(signature.toString());
        item.setType(CompletionItemType.METHOD);
        if (selectionStart != -1) {
            item.setSelectionStart(selectionStart);
            item.setSelectionLength(selectionLength);
        }
        add(item);
    }

    @Override
    public void addProperty(Field field) {
        CompletionItem item = new CompletionItem();
        item.setLabel(field.getName());
        item.setType(CompletionItemType.FIELD);
        add(item);
    }

    @Override
    public void addProperty(String property) {
        add(createItem(property, CompletionItemType.PROPERTY));
    }

    @Override
    public void addMethod(String method) {
        add(createItem(method, CompletionItemType.FUNCTION));
    }

    /** Adds a completion item if it is not already present. */
    public void add(CompletionItem item) {
        if (!existingNames.contains(item.getLabel())) {
            existingNames.add(item.getLabel());
            targets.add(item);
        }
    }

    /** Creates a completion item from a property or method name. */
    private static CompletionItem createItem(String property, CompletionItemType type) {
        String label = property;
        CompletionItem item = new CompletionItem();
        int selectionStart = property.indexOf("${");
        int selectionEnd = -1;
        if (selectionStart != -1) {
            selectionEnd = property.indexOf("}", selectionStart + 1);
            if (selectionEnd != -1) {
                label = property.substring(0, selectionStart) + property.substring(selectionStart + 2, selectionEnd)
                        + property.substring(selectionEnd + 1, property.length());

            }
        }
        item.setLabel(label);
        item.setType(type);
        if (selectionEnd != -1) {
            item.setSelectionStart(selectionStart);
            item.setSelectionLength(selectionEnd - selectionStart - 2);
        }
        return item;
    }

    @Override
    public boolean isCollectProperty() {
        return true;
    }

    @Override
    public boolean isCollectMethod() {
        return true;
    }
}
