package io.quarkus.maven.config.doc;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * We will recommend using generate-config-doc that is more flexible.
 * <p>
 * The GenerateConfigDocMojo defaults have to stay consistent with this.
 */
@Mojo(name = "generate-asciidoc", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class GenerateAsciidocMojo extends GenerateConfigDocMojo {
}
