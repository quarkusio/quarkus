package org.acme.serialization;

import java.io.Serializable;

/**
 * This class is never referenced directly from application bytecode.
 * It is only referenced inside the serialized resource file (data.ser),
 * created by {@link GenerateResource} during the build.
 * The tree shaker must discover it by scanning serialized resources
 * in this dependency.
 */
public class SerializedTarget implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;

    public SerializedTarget(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
