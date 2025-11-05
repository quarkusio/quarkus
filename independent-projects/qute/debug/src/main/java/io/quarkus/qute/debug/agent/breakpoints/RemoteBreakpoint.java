package io.quarkus.qute.debug.agent.breakpoints;

import static io.quarkus.qute.debug.agent.evaluations.ConditionalExpressionHelper.parseCondition;

import org.eclipse.lsp4j.debug.Breakpoint;
import org.eclipse.lsp4j.debug.Source;

import io.quarkus.qute.TemplateNode;
import io.quarkus.qute.debug.agent.frames.RemoteStackFrame;

/**
 * Represents a remote breakpoint set by the client through the Debug Adapter Protocol (DAP).
 * <p>
 * A {@link RemoteBreakpoint} extends the standard {@link Breakpoint} by adding
 * Qute-specific features such as conditional breakpoints. A conditional
 * breakpoint will only be triggered if its condition evaluates to {@code true}
 * at runtime.
 * </p>
 */
public class RemoteBreakpoint extends Breakpoint {

    /**
     * The raw condition expression as provided by the client, may be {@code null}.
     */
    private final transient String condition;

    /**
     * Indicates whether the condition has been parsed.
     */
    private transient boolean parsed;

    /**
     * The parsed representation of the condition, as a Qute {@link TemplateNode}.
     * <p>
     * This node can be evaluated dynamically against the current template
     * execution state to determine if the breakpoint should trigger.
     * </p>
     */
    private transient TemplateNode ifNode;

    /**
     * Creates a new {@link RemoteBreakpoint} for a given source and line.
     *
     * @param source the source file where the breakpoint is set
     * @param line the line number of the breakpoint
     * @param condition an optional condition expression, may be {@code null} or blank
     */
    public RemoteBreakpoint(Source source, int line, String condition) {
        super.setLine(line);
        super.setSource(source);
        this.condition = condition;
    }

    /**
     * Returns the raw condition expression, if any.
     *
     * @return the condition expression, or {@code null} if none
     */
    public String getCondition() {
        return condition;
    }

    /**
     * Checks whether this breakpoint should be triggered given the current
     * {@link RemoteStackFrame}.
     * <p>
     * - If no condition is defined, the breakpoint always triggers.
     * - If a condition exists, it is lazily parsed and evaluated against the
     * provided stack frame context.
     * </p>
     *
     * @param frame the current stack frame during template execution
     * @return {@code true} if the breakpoint should trigger, {@code false} otherwise
     */
    public boolean checkCondition(RemoteStackFrame frame) {
        String condition = getCondition();
        if (!hasCondition()) {
            return true;
        }
        if (!parsed) {
            try {
                ifNode = parseCondition(condition);
            } catch (Throwable e) {
                return false;
            } finally {
                parsed = true;
            }
        }
        return ifNode != null && (boolean) frame.evaluateCondition(ifNode, true) //
                .toCompletableFuture() //
                .getNow(false);
    }

    /**
     * Returns whether this breakpoint has a condition.
     *
     * @return {@code true} if a non-blank condition is set, {@code false} otherwise
     */
    public boolean hasCondition() {
        return condition != null && !condition.isBlank();
    }

}
