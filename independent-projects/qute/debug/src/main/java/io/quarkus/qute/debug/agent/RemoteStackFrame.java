package io.quarkus.qute.debug.agent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.lsp4j.debug.StackFrame;

import io.quarkus.qute.Engine;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.TemplateNode;
import io.quarkus.qute.TextNode;
import io.quarkus.qute.debug.agent.evaluations.ConditionalExpressionHelper;
import io.quarkus.qute.debug.agent.scopes.GlobalsScope;
import io.quarkus.qute.debug.agent.scopes.LocalsScope;
import io.quarkus.qute.debug.agent.scopes.RemoteScope;
import io.quarkus.qute.debug.agent.variables.VariablesRegistry;
import io.quarkus.qute.trace.ResolveEvent;

public class RemoteStackFrame extends StackFrame {

    public static final StackFrame[] EMPTY_STACK_FRAMES = new StackFrame[0];

    private static final AtomicInteger frameIdCounter = new AtomicInteger();

    private final transient RemoteStackFrame previousFrame;
    private final transient String templateId;
    private final transient VariablesRegistry variablesRegistry;
    private transient Collection<RemoteScope> scopes;
    private final transient ResolveEvent event;

    public RemoteStackFrame(ResolveEvent event, RemoteStackFrame previousFrame,
            SourceTemplateRegistry sourceTemplateRegistry, VariablesRegistry variablesRegistry) {
        this.event = event;
        this.previousFrame = previousFrame;
        this.variablesRegistry = variablesRegistry;
        int id = frameIdCounter.incrementAndGet();
        int line = event.getTemplateNode().getOrigin().getLine();

        super.setId(id);
        super.setName(event.getTemplateNode().toString());
        super.setLine(line);
        this.templateId = event.getTemplateNode().getOrigin().getTemplateId();
        super.setSource(
                sourceTemplateRegistry.getSource(templateId, previousFrame != null ? previousFrame.getSource() : null));
    }

    public String getTemplateId() {
        return templateId;
    }

    public RemoteStackFrame getPrevious() {
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
        // Locals scope
        scopes.add(new LocalsScope(event.getContext(), variablesRegistry));
        // Global scope
        scopes.add(new GlobalsScope(event.getContext(), variablesRegistry));
        return scopes;
    }

    public CompletableFuture<Object> evaluate(String expression) {
        if (isConditionExpression(expression)) {
            TemplateNode ifNode;
            try {
                ifNode = ConditionalExpressionHelper.parseCondition(expression);
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
            // Evaluate condition expression without ignoring syntax expression
            return evaluateCondition(ifNode, false);
        }
        // Evaluate simple expression
        return event.getContext().evaluate(expression).toCompletableFuture();
    }

    private static boolean isConditionExpression(String expression) {
        return expression.contains("!") || expression.contains(">") || expression.contains("gt")
                || expression.contains(">=") || expression.contains(" ge") || expression.contains("<")
                || expression.contains(" lt") || expression.contains("<=") || expression.contains(" le")
                || expression.contains(" eq") || expression.contains("==") || expression.contains(" is")
                || expression.contains("!=") || expression.contains(" ne") || expression.contains("&&")
                || expression.contains(" and") || expression.contains("||") || expression.contains(" or");
    }

    public CompletableFuture<Object> evaluateCondition(TemplateNode ifNode, boolean ignoreError) {
        try {
            return ifNode.resolve(event.getContext())//
                    .toCompletableFuture()//
                    .handle((result, error) -> {
                        if (error != null) {
                            if (ignoreError) {
                                return false;
                            }
                            throw new CompletionException(error);
                        }
                        var textNode = (TextNode) result;
                        return Boolean.parseBoolean(textNode.getValue());
                    });
        } catch (Throwable e) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
    }

    public Engine getEngine() {
        return event.getEngine();
    }

    ResolveEvent getEvent() {
        return event;
    }

    public EvalContext createEvalContext(Object base) {
        return new DebuggerEvalContext(base, this);
    }

}
