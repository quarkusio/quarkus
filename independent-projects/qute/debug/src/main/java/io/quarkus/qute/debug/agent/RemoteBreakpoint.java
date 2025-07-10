package io.quarkus.qute.debug.agent;

import static io.quarkus.qute.debug.agent.evaluations.ConditionalExpressionHelper.parseCondition;

import org.eclipse.lsp4j.debug.Breakpoint;
import org.eclipse.lsp4j.debug.Source;

import io.quarkus.qute.TemplateNode;

/**
 * Information about a Breakpoint created in setBreakpoints.
 */
public class RemoteBreakpoint extends Breakpoint {

    private final transient String condition;
    private transient boolean parsed;
    private transient TemplateNode ifNode;

    public RemoteBreakpoint(Source source, int line, String condition) {
        super.setLine(line);
        super.setSource(source);
        this.condition = condition;
    }

    public String getCondition() {
        return condition;
    }

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

    public boolean hasCondition() {
        return condition != null && !condition.isBlank();
    }

}
