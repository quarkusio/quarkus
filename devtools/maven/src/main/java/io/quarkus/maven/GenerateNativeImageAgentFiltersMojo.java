package io.quarkus.maven;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.model.Profile;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.builder.Json;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.runtime.LaunchMode;

@Mojo(name = "generate-filters", defaultPhase = LifecyclePhase.PROCESS_TEST_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class GenerateNativeImageAgentFiltersMojo extends GenerateCodeMojo {
    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        System.out.println("Calling generate native image agent filters");

        final Optional<Profile> runsWithNativeWithAgent = mavenProject().getActiveProfiles().stream()
                .filter(p -> "native-with-agent".equals(p.getId()))
                .findFirst();

        if (runsWithNativeWithAgent.isPresent()) {
            generateNativeAgentAccessFilter();
            // generateNativeAgentCallerFilter();
        }
    }

    private void generateNativeAgentAccessFilter() throws MojoExecutionException {
        // Get dependencies
        Collection<ResolvedDependency> dependencies = getDependencies();

        // Filter dependencies to exclude
        final List<ResolvedDependency> excludeDependencies = dependencies
                .stream()
                .filter(d -> "test".equals(d.getScope()) || (d.isDeploymentCp() && !d.isRuntimeCp()))
                .collect(Collectors.toList());

        // Get packages to exclude
        Collection<String> commonExcludePackageNames = getPackageNames(excludeDependencies);
        // System.out.println(commonExcludePackageNames.stream().sorted().collect(Collectors.joining("\n")));

        // Generate json using the packages
        generateNativeAgentFilter(getCallerExcludePackageNames(commonExcludePackageNames),
                Path.of(mavenProject().getModel().getBuild().getDirectory(),
                        "quarkus-caller-filter.json"));
        generateNativeAgentFilter(getAccessExcludePackageNames(commonExcludePackageNames),
                Path.of(mavenProject().getModel().getBuild().getDirectory(),
                        "quarkus-access-filter.json"));
    }

    private Collection<String> getAccessExcludePackageNames(Collection<String> commonExcludePackageNames) {
        final Set<String> result = new HashSet<>(commonExcludePackageNames);
        result.add("javax.crac");
        result.add("jdk.crac");
        return result;
    }

    private Set<String> getCallerExcludePackageNames(Collection<String> commonExcludePackageNames) {
        final Set<String> result = new HashSet<>(commonExcludePackageNames);
        result.add("com.sun");
        result.add("java");
        result.add("javax");
        result.add("jdk");
        result.add("sun");
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

    private Set<String> getPackageNames(List<ResolvedDependency> dependencies) {
        Set<String> packageNames = new HashSet<>();
        // Add default packages
        packageNames.add("io.netty");
        packageNames.add("io.quarkus");
        packageNames.add("io.smallrye");
        packageNames.add("io.vertx");
        packageNames.add("jakarta");
        packageNames.add("org.jboss");

        for (ResolvedDependency dep : dependencies) {
            dep.getContentTree().walk(visit -> {
                final String pathText = visit.getPath().toString();
                if (pathText.endsWith(".class") && !pathText.contains("META-INF")) {
                    final String[] split = pathText.split("/");
                    final int start = 1;
                    final int end = split.length - 2;
                    // Handle special 1 or 2 component packages separately
                    if (end > start) {
                        final String firstPackageComponent = split[start];
                        switch (firstPackageComponent) {
                            case "groovy":
                                packageNames.add(firstPackageComponent);
                                break;
                            default:
                                final int minPackageComponents = 3;
                                if (end >= start + minPackageComponents) {
                                    final String packageName = Arrays.stream(split, start, end)
                                            .collect(Collectors.joining("."));
                                    packageNames.add(packageName);
                                }
                        }
                    }

                }
            });
        }
        return packageNames;
    }
}
