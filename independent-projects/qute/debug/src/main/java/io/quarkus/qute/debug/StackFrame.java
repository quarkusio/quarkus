package io.quarkus.qute.debug;

import java.io.Serializable;

/**
 * A Stackframe contains the source location.
 *
 */
public class StackFrame implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final String name;
    private final String templateId;
    private final int line;

    public StackFrame(int id, String name, String templateId, int line) {
        super();
        this.id = id;
        this.name = name;
        this.templateId = templateId;
        this.line = line;
    }

    /**
     * Returns the identifier for the stack frame. It must be unique across all
     * threads.
     * <p>
     * This id can be used to retrieve the scopes of the frame or to restart the
     * execution of a stackframe.
     *
     * @return the identifier for the stack frame. It must be unique across all
     *         threads.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the name of the stack frame.
     *
     * @return the name of the stack frame.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the template source id.
     *
     * @return the template source id.
     */
    public String getTemplateId() {
        return templateId;
    }

    /**
     * Returns the line within the file of the frame.
     *
     * @return the line within the file of the frame.
     */
    public int getLine() {
        return line;
    }

}
