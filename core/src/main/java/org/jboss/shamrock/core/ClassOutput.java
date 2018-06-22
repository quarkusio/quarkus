package org.jboss.shamrock.core;

import java.io.IOException;

public interface ClassOutput {

    void writeClass(String className, byte[] data) throws IOException;

}
