package io.quarkus.qute.debug.agent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;

import io.quarkus.qute.EvalContext;
import io.quarkus.qute.Expression;
import io.quarkus.qute.ResolutionContext;
import io.quarkus.qute.debug.agent.frames.RemoteStackFrame;

/**
 * Implementation of {@link EvalContext} used by the Qute debugger.
 * <p>
 * This context provides evaluation support for expressions within the current
 * stack frame being debugged. It allows evaluating expressions dynamically
 * while debugging a template.
 * </p>
 */
public class DebuggerEvalContext implements EvalContext {

    private final Object base;
    private final RemoteStackFrame stackFrame;

    /**
     * Creates a new debugger evaluation context.
     *
     * @param base the base object for evaluation, may be {@code null}
     * @param stackFrame the current {@link RemoteStackFrame} associated with this context
     */
    public DebuggerEvalContext(Object base, RemoteStackFrame stackFrame) {
        this.base = base;
        this.stackFrame = stackFrame;
    }

    /**
     * Returns the base object for this evaluation context.
     *
     * @return the base object, may be {@code null}
     */
    @Override
    public Object getBase() {
        return base;
    }

    /**
     * Returns the name of the expression being evaluated.
     * <p>
     * This implementation always returns an empty string as the debugger context
     * does not use expression names directly.
     * </p>
     *
     * @return an empty string
     */
    @Override
    public String getName() {
        return "";
    }

    /**
     * Returns the list of parameters for this evaluation.
     * <p>
     * This implementation always returns an empty list as parameterized
     * expressions are not used in the debugger context.
     * </p>
     *
     * @return an empty list of parameters
     */
    @Override
    public List<Expression> getParams() {
        return Collections.emptyList();
    }

    /**
     * Evaluates a raw expression string within the current stack frame.
     *
     * @param expression the expression to evaluate
     * @return a {@link CompletionStage} with the result of the evaluation
     */
    @Override
    public CompletionStage<Object> evaluate(String expression) {
        return stackFrame.evaluate(expression);
    }

    /**
     * Evaluates a compiled {@link Expression} instance.
     * <p>
     * This method is currently not implemented and always returns {@code null}.
     * </p>
     *
     * @param expression the compiled expression to evaluate
     * @return always {@code null}
     */
    @Override
    public CompletionStage<Object> evaluate(Expression expression) {
        return null;
    }

    /**
     * Retrieves an attribute by key.
     * <p>
     * This method is currently not implemented and always returns {@code null}.
     * </p>
     *
     * @param key the attribute key
     * @return always {@code null}
     */
    @Override
    public Object getAttribute(String key) {
        return null;
    }

    /**
     * Returns the {@link ResolutionContext} associated with the current stack frame.
     *
     * @return the resolution context of the current stack frame
     */
    @Override
    public ResolutionContext resolutionContext() {
        return stackFrame.getEvent().getContext();
    }

}
