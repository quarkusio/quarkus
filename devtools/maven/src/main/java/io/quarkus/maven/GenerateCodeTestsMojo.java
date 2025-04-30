package io.quarkus.maven;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.builder.Json;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.runtime.LaunchMode;

@Mojo(name = "generate-code-tests", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class GenerateCodeTestsMojo extends GenerateCodeMojo {

    /**
     * A switch that enables or disables serialization of an {@link ApplicationModel} to a file for tests.
     * Deserializing an application model when bootstrapping Quarkus tests has a performance advantage in that
     * the tests will not have to initialize a Maven resolver and re-resolve the application model, which may save,
     * depending on a project, ~80-95% of time on {@link ApplicationModel} resolution.
     * <p>
     * Serialization of the test model is enabled by default.
     */
    @Parameter(property = "quarkus.generate-code.serialize-test-model", defaultValue = "true")
    boolean serializeTestModel;

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        generateCode(getParentDirs(mavenProject().getTestCompileSourceRoots()),
                path -> mavenProject().addTestCompileSourceRoot(path.toString()), true);

        if (isTestWithNativeAgent()) {
            generateNativeAgentFilters();
        }
    }

    @Override
    protected boolean isSerializeTestModel() {
        return serializeTestModel;
    }

    private boolean isTestWithNativeAgent() {
        String value = System.getProperty("quarkus.test.integration-test-profile");
        if ("test-with-native-agent".equals(value)) {
            return true;
        }

        final Object obj = mavenProject().getProperties().get("quarkus.test.integration-test-profile");
        return obj != null && "test-with-native-agent".equals(obj.toString());
    }

    private void generateNativeAgentFilters() throws MojoExecutionException {
        getLog().debug("Generate native image agent filters");

        // Get packages to exclude
        Collection<String> commonExcludePackageNames = getCommonExcludePackageNames();

        // Generate json using the packages
        generateNativeAgentFilter(commonExcludePackageNames,
                Path.of(mavenProject().getModel().getBuild().getDirectory(),
                        "quarkus-caller-filter.json"));
        generateNativeAgentFilter(commonExcludePackageNames,
                Path.of(mavenProject().getModel().getBuild().getDirectory(),
                        "quarkus-access-filter.json"));
    }

    private Collection<String> getAccessExcludePackageNames(Collection<String> commonExcludePackageNames) {
        final Set<String> result = new HashSet<>(commonExcludePackageNames);
        // Quarkus bootstrap depends on CRaC on startup and its APIs do reflection lookups.
        // These should be excluded from generated configuration because Quarkus takes care of it.
        result.add("javax.crac");
        result.add("jdk.crac");
        return result;
    }

    private void generateNativeAgentFilter(Collection<String> packageNames, Path path) throws MojoExecutionException {
        final Json.JsonObjectBuilder result = Json.object();

        final Json.JsonArrayBuilder rules = Json.array();
        packageNames.stream()
                .map(packageName -> Json.object().put("excludeClasses", packageName + ".**"))
                .forEach(rules::add);
        result.put("rules", rules);

        final Json.JsonArrayBuilder regexRules = Json.array();
        regexRules.add(Json.object().put("excludeClasses", ".*_Bean"));
        result.put("regexRules", regexRules);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile(), StandardCharsets.UTF_8))) {
            result.appendTo(writer);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write quarkus native image agent caller filter to " + path, e);
        }
    }

    private Collection<ResolvedDependency> getDependencies() throws MojoExecutionException {
        try (CuratedApplication curatedApplication = bootstrapApplication(LaunchMode.TEST)) {
            return curatedApplication.getApplicationModel().getDependencies();
        } catch (Exception any) {
            throw new MojoExecutionException("Quarkus native image agent filter generation phase has failed", any);
        }
    }

    private Set<String> getCommonExcludePackageNames() {
        Set<String> packageNames = new HashSet<>();
        // Any calls that access or originate in these packages
        // that require native configuration should be handled by Quarkus.
        packageNames.add("io.netty");
        packageNames.add("io.quarkus");
        packageNames.add("io.smallrye");
        packageNames.add("io.vertx");
        packageNames.add("jakarta");
        packageNames.add("org.jboss");
        return packageNames;
    }
}
