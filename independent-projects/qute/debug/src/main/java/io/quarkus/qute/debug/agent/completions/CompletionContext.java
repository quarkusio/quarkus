package io.quarkus.qute.debug.agent.completions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.lsp4j.debug.CompletionItem;
import org.eclipse.lsp4j.debug.CompletionsResponse;

import io.quarkus.qute.debug.agent.RemoteStackFrame;

public class CompletionContext {

    public static final CompletionItem[] EMPTY_COMPLETION_ITEMS = new CompletionItem[0];

    private final Object base;
    private final RemoteStackFrame stackFrame;
    private final Collection<CompletionItem> targets;
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

    public CompletionsResponse toResponse() {
        CompletionsResponse response = new CompletionsResponse();
        response.setTargets(targets.toArray(EMPTY_COMPLETION_ITEMS));
        return response;
    }

    public void add(CompletionItem item) {
        if (!existingNames.contains(item.getLabel())) {
            existingNames.add(item.getLabel());
            targets.add(item);
        }
    }
}
