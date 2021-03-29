package io.quarkus.maven.components;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.codehaus.plexus.component.annotations.Component;

/**
 * Prompt implementation.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@Component(role = Prompter.class, instantiationStrategy = "per-lookup")
public class Prompter extends io.quarkus.devtools.utils.Prompter {
    public Prompter() throws IOException {
        super();
    }

    public Prompter(InputStream in, OutputStream out) throws IOException {
        super(in, out);
    }
}
