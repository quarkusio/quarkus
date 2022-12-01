package io.quarkus.maven;

import org.apache.maven.plugins.annotations.Mojo;

/**
 * Allow removing extensions to an existing pom.xml file.
 * Because you can remove one or several extension in one go, there are 2 mojos:
 * {@code remove-extensions} and {@code remove-extension}. Both supports the {@code extension} and {@code extensions}
 * parameters.
 */
@Mojo(name = "remove-extensions")
public class RemoveExtensionsMojo extends RemoveExtensionMojo {

}
