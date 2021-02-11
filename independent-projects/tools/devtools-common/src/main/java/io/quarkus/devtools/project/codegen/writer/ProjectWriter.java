/**
 * 
 */
package io.quarkus.devtools.project.codegen.writer;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * This writer provide a way to write direct a file or inside a zip for project creation.
 */
public interface ProjectWriter extends Closeable {

    void init() throws IOException;

    void write(String path, String content) throws IOException;

    byte[] getContent(String path) throws IOException;

    String mkdirs(String path) throws IOException;

    boolean exists(String path);

    File getProjectFolder();

    boolean hasFile();
}
