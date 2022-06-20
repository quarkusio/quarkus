package io.quarkus.qute.debug;

import java.io.Serializable;

/**
 * Information about a Breakpoint created in setBreakpoints.
 */
public class Breakpoint implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String templateId;

    private final int line;

    public Breakpoint(String templateId, int line) {
        this.templateId = templateId;
        this.line = line;
    }

    /**
     * Returns the source template id where the breakpoint is located.
     *
     * @return the source template id where the breakpoint is located.
     */
    public String getTemplateId() {
        return templateId;
    }

    /**
     * Return the start line of the actual range covered by the breakpoint.
     *
     * @return the start line of the actual range covered by the breakpoint.
     */
    public int getLine() {
        return line;
    }

}
