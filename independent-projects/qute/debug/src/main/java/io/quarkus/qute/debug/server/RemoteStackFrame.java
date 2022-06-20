package io.quarkus.qute.debug.server;

import io.quarkus.qute.debug.StackFrame;
import io.quarkus.qute.debug.server.scopes.GlobalsScope;
import io.quarkus.qute.debug.server.scopes.LocalsScope;
import io.quarkus.qute.trace.ResolveEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class RemoteStackFrame {

    private static final AtomicInteger frameIdCounter = new AtomicInteger();

    private final StackFrame previousFrame;

    private Collection<RemoteScope> scopes;

    private final StackFrame data;

    private ResolveEvent event;

    public RemoteStackFrame(ResolveEvent event, StackFrame previousFrame) {
        super();
        this.event = event;
        int id = frameIdCounter.incrementAndGet();
        String templateId = event.getTemplateNode().getOrigin().getTemplateId();
        int line = event.getTemplateNode().getOrigin().getLine();
        String name = event.getTemplateNode().toString();
        this.data = new StackFrame(id, name, templateId, line);
        this.previousFrame = previousFrame;
    }

    public int getId() {
        return data.getId();
    }

    public String getName() {
        return data.getName();
    }

    public String getTemplateId() {
        return data.getTemplateId();
    }

    public int getLine() {
        return data.getLine();
    }

    public StackFrame getPrevious() {
        return previousFrame;
    }

    public Collection<RemoteScope> getScopes() {
        if (scopes == null) {
            scopes = createScopes();
        }
        return scopes;
    }

    private Collection<RemoteScope> createScopes() {
        Collection<RemoteScope> scopes = new ArrayList<>();
        // Global scope
        scopes.add(new GlobalsScope(event.getContext()));
        // Locals scope
        scopes.add(new LocalsScope());
        return scopes;
    }

    public StackFrame getData() {
        return data;
    }
}
