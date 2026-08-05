package io.quarkus.deployment.dev.testing;

import java.io.OutputStream;

/**
 * Delegates to {@link org.aesh.terminal.utils.HtmlAnsiOutputStream} from aesh-readline.
 */
public class HtmlAnsiOutputStream extends org.aesh.terminal.utils.HtmlAnsiOutputStream {

    public HtmlAnsiOutputStream(OutputStream out) {
        super(out);
    }
}
