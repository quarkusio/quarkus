package io.quarkus.qute.debug.agent;

import java.net.URI;
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
import io.quarkus.qute.debug.agent.scopes.NamespaceResolversScope;
import io.quarkus.qute.debug.agent.scopes.RemoteScope;
import io.quarkus.qute.debug.agent.source.RemoteSource;
import io.quarkus.qute.debug.agent.source.SourceTemplateRegistry;
import io.quarkus.qute.debug.agent.variables.VariablesRegistry;
import io.quarkus.qute.trace.ResolveEvent;

/**
 * Represents a single stack frame in the Qute debugging process.
 * <p>
 * A {@link RemoteStackFrame} corresponds to the evaluation of a
 * {@link TemplateNode} at runtime. It stores contextual information such as the
 * variables in scope, the template being executed, and the current execution
 * state.
 * </p>
 *
 * <p>
 * It extends {@link StackFrame} from the Debug Adapter Protocol (DAP), allowing
 * integration with remote debugging clients.
 * </p>
 */
public class RemoteStackFrame extends StackFrame {

    /**
     * Represents an empty array of stack frames.
     */
    public static final StackFrame[] EMPTY_STACK_FRAMES = new StackFrame[0];

    /**
     * Counter used to assign a unique ID to each frame.
     */
    private static final AtomicInteger frameIdCounter = new AtomicInteger();

    /**
     * The previous frame in the call stack, or {@code null} if this is the first
     * frame.
     */
    private final transient RemoteStackFrame previousFrame;

    /**
     * The ID of the template currently being executed.
     */
    private final transient String templateId;

    /**
     * Registry of variables used in this stack frame.
     */
    private final transient VariablesRegistry variablesRegistry;

    /**
     * Lazily created list of available scopes (locals, globals, namespaces).
     */
    private transient Collection<RemoteScope> scopes;

    /**
     * The resolve event associated with this frame, containing runtime context.
     */
    private final transient ResolveEvent event;

    /**
     * Creates a new {@link RemoteStackFrame}.
     *
     * @param event the resolve event describing the current
     *        execution
     * @param previousFrame the previous stack frame, may be {@code null}
     * @param sourceTemplateRegistry registry for mapping templates to debug sources
     * @param variablesRegistry the registry for managing variables
     */
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
                sourceTemplateRegistry.getSource(templateId,
                        previousFrame != null ? previousFrame.getSource() : null));
    }

    /**
     * Returns the template ID associated with this frame.
     *
     * @return the template ID
     */
    public String getTemplateId() {
        return templateId;
    }

    /**
     * Returns the template Uri associated with this frame and null otherwise.
     *
     * @return the template Uri associated with this frame and null otherwise.
     */
    public URI getTemplateUri() {
        var source = getSource();
        return source != null ? source.getUri() : null;
    }

    @Override
    public RemoteSource getSource() {
        return (RemoteSource) super.getSource();
    }

    /**
     * Returns the previous stack frame, or {@code null} if none exists.
     *
     * @return the previous {@link RemoteStackFrame}
     */
    public RemoteStackFrame getPrevious() {
        return previousFrame;
    }

    /**
     * Returns the list of scopes for this frame.
     * <p>
     * Scopes include:
     * <ul>
     * <li>Locals (variables in the current template context)</li>
     * <li>Globals (global variables accessible in Qute)</li>
     * <li>Namespace resolvers (custom resolvers for Qute templates)</li>
     * </ul>
     * </p>
     *
     * @return the collection of {@link RemoteScope}
     */
    public Collection<RemoteScope> getScopes() {
        if (scopes == null) {
            scopes = createScopes();
        }
        return scopes;
    }

    /**
     * Creates the list of scopes for this frame.
     *
     * @return a collection of {@link RemoteScope}
     */
    private Collection<RemoteScope> createScopes() {
        Collection<RemoteScope> scopes = new ArrayList<>();
        // Locals scope
        scopes.add(new LocalsScope(event.getContext(), this, variablesRegistry));
        // Global scope
        scopes.add(new GlobalsScope(event.getContext(), this, variablesRegistry));
        // Namespace resolvers scope
        scopes.add(new NamespaceResolversScope(event.getEngine(), this, variablesRegistry));
        return scopes;
    }

    /**
     * Evaluates an expression in the current frame context.
     * <p>
     * If the expression contains conditional operators, it is parsed and evaluated
     * as a conditional expression. Otherwise, it is treated as a simple Qute
     * expression.
     * </p>
     *
     * @param expression the expression to evaluate
     * @return a {@link CompletableFuture} containing the result of the evaluation
     */
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

    /**
     * Determines if a given expression should be treated as a conditional
     * expression.
     *
     * @param expression the expression to test
     * @return {@code true} if the expression contains conditional operators,
     *         {@code false} otherwise
     */
    private static boolean isConditionExpression(String expression) {
        return expression.contains("!") || expression.contains(">") || expression.contains("gt")
                || expression.contains(">=") || expression.contains(" ge") || expression.contains("<")
                || expression.contains(" lt") || expression.contains("<=") || expression.contains(" le")
                || expression.contains(" eq") || expression.contains("==") || expression.contains(" is")
                || expression.contains("!=") || expression.contains(" ne") || expression.contains("&&")
                || expression.contains(" and") || expression.contains("||") || expression.contains(" or");
    }

    /**
     * Evaluates a parsed conditional expression.
     *
     * @param ifNode the parsed {@link TemplateNode} representing the condition
     * @param ignoreError whether to ignore evaluation errors and return
     *        {@code false}
     * @return a {@link CompletableFuture} containing {@code true} or {@code false}
     */
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

    /**
     * Returns the current Qute engine.
     *
     * @return the {@link Engine}
     */
    public Engine getEngine() {
        return event.getEngine();
    }

    /**
     * Returns the {@link ResolveEvent} associated with this frame.
     *
     * @return the {@link ResolveEvent}
     */
    ResolveEvent getEvent() {
        return event;
    }

    /**
     * Creates a new evaluation context for the given base object.
     *
     * @param base the base object
     * @return a new {@link EvalContext}
     */
    public EvalContext createEvalContext(Object base) {
        return new DebuggerEvalContext(base, this);
    }

}
