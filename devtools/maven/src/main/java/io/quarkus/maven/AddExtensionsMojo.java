package io.quarkus.maven;

import org.apache.maven.plugins.annotations.Mojo;

/**
 * Allow adding extensions to an existing pom.xml file.
 * Because you can add one or several extension in one go, there are 2 mojos:
 * {@code add-extensions} and {@code add-extension}. Both supports the {@code extension} and {@code extensions}
 * parameters.
 */
@Mojo(name = "add-extensions")
public class AddExtensionsMojo extends AddExtensionMojo {

}
