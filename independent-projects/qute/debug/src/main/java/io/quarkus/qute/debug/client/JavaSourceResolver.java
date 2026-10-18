package io.quarkus.qute.debug.client;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;

/**
 * Resolver for Java source locations referenced from Qute templates.
 * <p>
 * Implementations of this interface provide a way to resolve a Qute template reference
 * (via a {@code qute-java://} URI) to the corresponding Java source file and template position.
 * </p>
 *
 * <p>
 * This resolver is used by the Debug Adapter Protocol (DAP) **only to support breakpoints**
 * on Java methods or classes related to Qute templates.
 * </p>
 *
 * <h2>Example with a Qute template in a JAX-RS resource</h2>
 *
 * <pre>
 * package org.acme.quarkus.sample;
 *
 * import jakarta.ws.rs.GET;
 * import jakarta.ws.rs.Path;
 * import jakarta.ws.rs.QueryParam;
 * import jakarta.ws.rs.Produces;
 * import jakarta.ws.rs.core.MediaType;
 *
 * import io.quarkus.qute.TemplateContents;
 * import io.quarkus.qute.TemplateInstance;
 *
 * &#64;Path("hello")
 * public class HelloResource {
 *
 *     &#64;TemplateContents("""
 *             &lt;p&gt;Hello {name ?: "Qute"}&lt;/p&gt;!
 *             """)
 *     record Hello(String name) implements TemplateInstance {
 *     }
 *
 *     &#64;GET
 *     &#64;Produces(MediaType.TEXT_PLAIN)
 *     public TemplateInstance get(@QueryParam("name") String name) {
 *         return new Hello(name);
 *     }
 * }
 * </pre>
 *
 * <p>
 * Corresponding {@code qute-java://} URI for the record:
 * </p>
 *
 * <pre>
 * qute-java://org.acme.quarkus.sample.HelloResource$Hello@io.quarkus.qute.TemplateContents
 * </pre>
 *
 * <p>
 * Parsed into {@link JavaSourceLocationArguments}:
 * </p>
 * <ul>
 * <li>unresolvedUri = "qute-java://org.acme.quarkus.sample.HelloResource$Hello@io.quarkus.qute.TemplateContents"</li>
 * <li>typeName = "org.acme.quarkus.sample.HelloResource$Hello"</li>
 * <li>method = null (the annotation is on the record class)</li>
 * <li>annotation = "io.quarkus.qute.TemplateContents"</li>
 * </ul>
 *
 * <p>
 * Example resulting {@link JavaSourceLocationResponse}:
 * </p>
 * <ul>
 * <li>javaFileUri = "file:///path/to/project/src/main/java/org/acme/quarkus/sample/HelloResource.java"</li>
 * <li>startLine = 16 (line of the text block content of the TemplateContents annotation)</li>
 * </ul>
 */
public interface JavaSourceResolver {

    /**
     * Resolves the Java method or class referenced from a Qute template for the purpose of setting breakpoints.
     * <p>
     * The {@link JavaSourceLocationArguments} contains the parsed information from
     * a {@code qute-java://} URI. The method returns a {@link CompletableFuture} that
     * completes with a {@link JavaSourceLocationResponse} containing the Java file URI
     * and the start line of the template content (or the method/class if applicable).
     * </p>
     *
     * @param args the arguments describing the Java element to resolve
     * @return a future completing with the resolved Java source location
     */
    @JsonRequest("qute/resolveJavaSource")
    CompletableFuture<JavaSourceLocationResponse> resolveJavaSource(JavaSourceLocationArguments args);
}
