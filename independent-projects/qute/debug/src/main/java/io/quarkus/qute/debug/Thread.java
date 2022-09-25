package io.quarkus.qute.debug;

import java.io.Serializable;

/**
 * A debuggee Thread
 */
public class Thread implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;

    private final String name;

    public Thread(long id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Returns the unique identifier for the thread.
     *
     * @return the unique identifier for the thread.
     */
    public long getId() {
        return id;
    }

    /**
     * Returns the name of the thread.
     *
     * @return the name of the thread.
     */
    public String getName() {
        return name;
    }

}
