package io.quarkus.qute.debug;

import java.io.Serializable;

/**
 * A Variable is a name/value pair.
 * <p>
 * Optionally a variable can have a 'type' that is shown if space permits or
 * when hovering over the variable's name.
 * <p>
 */
public class Variable implements Serializable {

    private static final long serialVersionUID = -186454754409625379L;

    private String name;
    private String value;
    private String type;

    public Variable(String name, String value, String type) {
        super();
        this.name = name;
        this.value = value;
        this.type = type;
    }

    /**
     * Returns the variable's name.
     *
     * @return the variable's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the variable's value.
     *
     * @return the variable's value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the variable's type.
     *
     * @return the variable's type.
     */
    public String getType() {
        return type;
    }
}
