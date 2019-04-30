/**
 * 
 */
package io.quarkus.cli.commands.writer;

import java.io.IOException;

/**
 * @author guillaumedufour
 *         This writer provide a way to write direct a file or inside a zip for project creation
 *
 */
public interface Writer {

    default boolean init() {
        return true;
    }

    void write(String path, String content) throws IOException;

    byte[] getContent(String path) throws IOException;

    String mkdirs(String path) throws IOException;

    boolean exists(String path);
}
