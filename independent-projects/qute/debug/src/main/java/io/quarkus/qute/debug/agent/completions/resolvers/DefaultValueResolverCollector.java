package io.quarkus.qute.debug.agent.completions.resolvers;

import org.eclipse.lsp4j.debug.CompletionItem;
import org.eclipse.lsp4j.debug.CompletionItemType;

import io.quarkus.qute.ValueResolver;
import io.quarkus.qute.debug.agent.completions.CompletionContext;

public class DefaultValueResolverCollector implements ResolverCollector {

    @Override
    public void collect(CompletionContext context, ValueResolver valueResolver) {
        valueResolver.getSupportedProperties().forEach(property -> context.add(createItem(property)));
        valueResolver.getSupportedMethods().forEach(method -> context.add(createItem(method)));
    }

    private static CompletionItem createItem(String property) {
        String label = property;
        CompletionItem item = new CompletionItem();
        int selectionStart = property.indexOf("${");
        int selectionEnd = -1;
        if (selectionStart != -1) {
            selectionEnd = property.indexOf("}", selectionStart + 1);
            if (selectionEnd != -1) {
                label = property.substring(0, selectionStart)
                        + property.substring(selectionStart + 2, selectionEnd)
                        + property.substring(selectionEnd + 1, property.length());

            }
        }
        item.setLabel(label);
        item.setType(CompletionItemType.PROPERTY);
        if (selectionEnd != -1) {
            item.setSelectionStart(selectionStart);
            item.setSelectionLength(selectionEnd - selectionStart - 2);
        }
        return item;
    }

}
