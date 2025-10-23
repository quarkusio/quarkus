package io.quarkus.qute.debug.agent.frames;

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
import io.quarkus.qute.debug.agent.DebuggerEvalContext;
import io.quarkus.qute.debug.agent.RemoteThread;
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
 * Represents a single Qute stack frame in the debugging process.
 *
 * <p>
 * A {@link RemoteStackFrame} corresponds to the evaluation of a {@link TemplateNode}
 * during the rendering of a Qute template. It encapsulates the current execution context,
 * including variables, scopes, and the source template being processed.
 * </p>
 *
 * <p>
 * This class integrates with the Debug Adapter Protocol (DAP) through
 * {@link org.eclipse.lsp4j.debug.StackFrame}, enabling external debuggers (like VSCode or IntelliJ)
 * to display Qute stack frames, inspect variables, and evaluate expressions.
 * </p>
 */
public class RemoteStackFrame extends StackFrame {

    /** Represents an empty array of stack frames. */
    public static final StackFrame[] EMPTY_STACK_FRAMES = new StackFrame[0];

    /** Counter used to assign a unique ID to each frame. */
    private static final AtomicInteger frameIdCounter = new AtomicInteger();

    /** The previous frame in the call stack, or {@code null} if this is the first frame. */
    private final transient RemoteStackFrame previousFrame;

    /** The ID of the template currently being executed. */
    private final transient String templateId;

    /** Registry of variables used in this stack frame. */
    private final transient VariablesRegistry variablesRegistry;

    /** Lazily created list of available scopes (locals, globals, namespaces). */
    private transient Collection<RemoteScope> scopes;

    /** The resolve event associated with this frame, containing runtime context. */
    private final transient ResolveEvent event;

    /** The remote thread that owns this frame, responsible for executing evaluations. */
    private final transient RemoteThread remoteThread;

    /**
     * Creates a new {@link RemoteStackFrame}.
     *
     * @param event the resolve event describing the current execution
     * @param previousFrame the previous stack frame, may be {@code null}
     * @param sourceTemplateRegistry registry for mapping templates to debug sources
     * @param variablesRegistry registry for managing variables
     * @param remoteThread the owning remote thread
     */
    public RemoteStackFrame(ResolveEvent event, RemoteStackFrame previousFrame,
            SourceTemplateRegistry sourceTemplateRegistry, VariablesRegistry variablesRegistry,
            RemoteThread remoteThread) {
        this.event = event;
        this.previousFrame = previousFrame;
        this.variablesRegistry = variablesRegistry;
        this.remoteThread = remoteThread;

        int id = frameIdCounter.incrementAndGet();
        int line = event.getTemplateNode().getOrigin().getLine();
        super.setId(id);
        super.setName(event.getTemplateNode().toString());
        super.setLine(line);

        this.templateId = event.getTemplateNode().getOrigin().getTemplateId();
        super.setSource(
                sourceTemplateRegistry.getSource(templateId, previousFrame != null ? previousFrame.getSource() : null));
    }

    /** @return the template ID associated with this frame */
    public String getTemplateId() {
        return templateId;
    }

    /** @return the template URI associated with this frame, or {@code null} if unavailable */
    public URI getTemplateUri() {
        var source = getSource();
        return source != null ? source.getUri() : null;
    }

    @Override
    public RemoteSource getSource() {
        return (RemoteSource) super.getSource();
    }

    /** @return the previous stack frame, or {@code null} if none exists */
    public RemoteStackFrame getPrevious() {
        return previousFrame;
    }

    /**
     * Returns the list of scopes for this frame.
     * <p>
     * Scopes include:
     * <ul>
     * <li>Locals — variables specific to the current template</li>
     * <li>Globals — shared Qute global variables</li>
     * <li>Namespace resolvers — registered resolvers for {@code namespace:expression}</li>
     * </ul>
     *
     * @return a collection of {@link RemoteScope}
     */
    public Collection<RemoteScope> getScopes() {
        if (scopes == null) {
            scopes = createScopes();
        }
        return scopes;
    }

    private Collection<RemoteScope> createScopes() {
        Collection<RemoteScope> scopes = new ArrayList<>();
        scopes.add(new LocalsScope(event.getContext(), this, variablesRegistry));
        scopes.add(new GlobalsScope(event.getContext(), this, variablesRegistry));
        scopes.add(new NamespaceResolversScope(event.getEngine(), this, variablesRegistry));
        return scopes;
    }

    /**
     * Evaluates an arbitrary Qute expression in the current frame context.
     * <p>
     * If the expression looks like a conditional (e.g. {@code user.age > 18}),
     * it is parsed and evaluated as a conditional expression. Otherwise, it is
     * evaluated as a simple Qute value expression.
     * </p>
     *
     * @param expression the Qute expression or condition to evaluate
     * @return a {@link CompletableFuture} resolving to the evaluation result
     */
    public CompletableFuture<Object> evaluate(String expression) {
        if (isConditionExpression(expression)) {
            TemplateNode ifNode;
            try {
                ifNode = ConditionalExpressionHelper.parseCondition(expression);
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
            // Run the condition evaluation in the render thread
            return evaluateConditionInRenderThread(ifNode, false);
        }
        return evaluateExpressionInRenderThread(expression);
    }

    /**
     * Evaluates a Qute expression inside the render thread.
     *
     * <p>
     * Qute expressions (like {@code uri:Todos.index}) must be evaluated inside the
     * original rendering thread to ensure CDI {@code @RequestScoped} contexts are
     * active. Evaluating them elsewhere may cause
     * {@code ContextNotActiveException}.
     * </p>
     */
    private CompletableFuture<Object> evaluateExpressionInRenderThread(String expression) {
        return remoteThread.evaluateInRenderThread(() -> {
            try {
                return event.getContext().evaluate(expression).toCompletableFuture();
            } catch (Exception e) {
                // ex : with expression 'http:', the getContext().evaluate(expression) throws a TemplateException
                // with the message "Parser error: empty expression found {http:}"
                return CompletableFuture.failedFuture(e);
            }
        });
    }

    /**
     * Checks if an expression contains conditional operators and should be
     * interpreted as a condition.
     */
    private static boolean isConditionExpression(String expression) {
        return expression.contains("!") || expression.contains(">") || expression.contains("==")
                || expression.contains("<") || expression.contains("&&") || expression.contains("||")
                || expression.contains(" eq") || expression.contains(" ne")
                || expression.contains(" gt") || expression.contains(" lt")
                || expression.contains(" ge") || expression.contains(" le")
                || expression.contains(" and") || expression.contains(" or")
                || expression.contains(" is");
    }

    /**
     * Evaluates a parsed conditional expression within the render thread context.
     *
     * <p>
     * This is used for conditional breakpoints: before suspending execution,
     * the condition must be evaluated safely inside the render thread.
     * </p>
     *
     * <p>
     * Calling {@code evaluateConditionInRenderThread()} from a suspended state
     * ensures the evaluation is scheduled on the render thread asynchronously
     * (via {@link RemoteThread#evaluateInRenderThread(java.util.concurrent.Callable)}),
     * avoiding deadlocks or premature resumption.
     * </p>
     *
     * @param ifNode the parsed {@link TemplateNode} representing the condition
     * @param ignoreError whether to ignore evaluation errors and return {@code false}
     * @return a future resolving to {@code true} or {@code false}
     */
    public CompletableFuture<Object> evaluateConditionInRenderThread(TemplateNode ifNode, boolean ignoreError) {
        return remoteThread.evaluateInRenderThread(() -> evaluateCondition(ifNode, ignoreError));
    }

    /**
     * Evaluates the given Qute {@code if} node in the current context.
     *
     * <p>
     * This method converts the Qute {@link TemplateNode} result into a boolean
     * value. If evaluation fails and {@code ignoreError} is {@code true}, it
     * returns {@code false} instead of throwing an exception.
     * </p>
     *
     * <p>
     * This method runs synchronously inside the render thread. It is typically
     * called by {@link #evaluateConditionInRenderThread(TemplateNode, boolean)}.
     * </p>
     */
    public CompletableFuture<Object> evaluateCondition(TemplateNode ifNode, boolean ignoreError) {
        try {
            return ifNode.resolve(event.getContext())
                    .toCompletableFuture()
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

    /** @return the current Qute engine */
    public Engine getEngine() {
        return event.getEngine();
    }

    /** @return the current {@link ResolveEvent} associated with this frame */
    public ResolveEvent getEvent() {
        return event;
    }

    /** Creates a new {@link EvalContext} for the given base object. */
    public EvalContext createEvalContext(Object base) {
        return new DebuggerEvalContext(base, this);
    }
}
