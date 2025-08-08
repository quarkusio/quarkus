package io.quarkus.qute.debug.agent.completions.resolvers;

import java.util.List;

import org.eclipse.lsp4j.debug.CompletionItem;
import org.eclipse.lsp4j.debug.CompletionItemType;

import io.quarkus.qute.debug.agent.completions.CompletionContext;

public abstract class SimpleResolverCollector implements ResolverCollector {

    private List<CompletionItem> properties;

    public SimpleResolverCollector(List<String> properties) {
        this.properties = properties.stream() //
                .map(property -> {
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
                }).toList();
    }

    @Override
    public void collect(CompletionContext context) {
        for (CompletionItem completion : properties) {
            if (completion.getSelectionStart() == null || completion.getStart() == null) {
                context.add(completion);
            } else {
                CompletionItem item = new CompletionItem();
                item.setLabel(completion.getLabel());
                item.setText(completion.getText());
                item.setType(completion.getType());
                context.add(item);
            }
        }
    }

}
