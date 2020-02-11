package io.quarkus.utilities.depchecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import io.quarkus.utilities.depchecker.DependencyOutputHandler.MavenModule;

public class DependencyChecker {

	public static void main(String args[]) {
		try {
			if (args == null || args.length < 2)
				throw new IllegalArgumentException("Two arguments are required: <sha1> and <sha2>");
			new DependencyChecker(args[0], args[1]).run();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	private static final Map<Path,MavenModule> parsedModules = new HashMap<>();
	public static final MavenModule ALL_MODULES = new MavenModule("ALL_MODULES");
	
	private final String sha1;
	private final String sha2;
	private final Path REPO_ROOT;
	
	public DependencyChecker(String sha1, String sha2) {
		this.sha1 = sha1;
		this.sha2 = sha2;
		Path root = Paths.get(".").toAbsolutePath();
		while (!root.getFileName().toString().equals("quarkus")) {
			root = root.getParent();
			if (root == null)
				throw new IllegalStateException("Unable to find root of Quarkus repo");
		}
		REPO_ROOT = root;
	}

	public void run() throws Exception {
		Map<String,MavenModule> depsMap = getMvnDependencies();
		// Validate module dependencies map to known modules
		for (MavenModule module : depsMap.values()) {
			for (String dependsOn : module.dependsOn())
				if (!depsMap.containsKey(dependsOn))
					System.out.println("WARN: Module " + module.name + " depended on an unknown module " + dependsOn);
		}
		
		Set<String> modifiedFiles = getModifiedFiles();
		if (modifiedFiles.isEmpty())
			throw new IllegalStateException("No modified files detected");
		System.out.println("Modified files: ");
		modifiedFiles.stream().forEach(f -> System.out.println("  " + f));
		
		Set<MavenModule> modifiedModules = getModifiedModules(depsMap, modifiedFiles);
		if (modifiedModules.isEmpty())
			throw new IllegalStateException("No modified maven modules detected");
		System.out.println("\nModified modules: ");
		modifiedModules.stream().forEach(m -> System.out.println("  " + m));
		
		// Resolve affected modules
		Set<MavenModule> affectedModules = new HashSet<>();
		affectedModules.addAll(modifiedModules);
		for (MavenModule modifiedModule : modifiedModules) {
			if (!depsMap.containsKey(modifiedModule.name)) {
				throw new IllegalStateException("Modified module '" + modifiedModule + 
						"' was not found in dependnecy map: " + depsMap.keySet());
			}
			MavenModule currentModule = depsMap.get(modifiedModule.name);
			for (MavenModule otherModule : depsMap.values()) {
				if (otherModule.dependsOn().contains(modifiedModule.name)) {
					currentModule.dependents.add(otherModule);
					affectedModules.add(otherModule);
				}
			}
		}
		// @AGG LEFTOFF split this out into a separate method
		System.out.println("\nFinal affected modules: ");
		affectedModules.stream().forEach(m -> System.out.println("  " + m.name));
		
		System.out.println("\nIntegration tests to run:");
		affectedModules = affectedModules.stream()
			.filter(m -> m.name.contains("-integration-test-"))
			.collect(Collectors.toSet());
		
		String testsToRun = "--projects=" + affectedModules.stream()
			.map(m -> m.name)
			.map(name -> name.substring(name.indexOf("-integration-test-") + 18))
			.collect(Collectors.joining(","));
		if (affectedModules.size() == 0) {
			testsToRun = "";
			System.out.println("No native image tests will be run.");
		} else {
			System.out.println("Writing tests to run argument:");
			System.out.println(testsToRun);
		}
		
		Path outputFile = Paths.get("target", "tests-to-run.txt");
		Files.deleteIfExists(outputFile);
		Files.createDirectories(outputFile.getParent());
		Files.write(outputFile, testsToRun.getBytes());
	}
	
	private Map<String,MavenModule> getMvnDependencies() throws MavenInvocationException, CommandLineException {
		InvocationRequest request = new DefaultInvocationRequest();
		File rootPom = REPO_ROOT.resolve("pom.xml").toFile();
		if (!rootPom.exists())
			throw new IllegalStateException("Root pom not found at: " + rootPom.getAbsolutePath());
		request.setPomFile(rootPom);
		request.setBatchMode(true);
		request.setGoals(Collections.singletonList("dependency:tree"));
		Properties props = new Properties();
		props.put("includes", ":::999-SNAPSHOT");
		request.setProperties(props);

		Invoker invoker = new DefaultInvoker();
		invoker.setWorkingDirectory(REPO_ROOT.toFile());
		invoker.setMavenHome(Paths.get(System.getProperty("user.home"), ".m2").toFile());
		Path mvnw = System.getProperty("os.name").toUpperCase().contains("WINDOWS") ?
					REPO_ROOT.resolve("mvnw.bat") :
					REPO_ROOT.resolve("mvnw");
		invoker.setMavenExecutable(mvnw.toFile());
		DependencyOutputHandler dependencyOutput = new DependencyOutputHandler();
		invoker.setOutputHandler(dependencyOutput);
		InvocationResult result = invoker.execute(request);

		if (result.getExecutionException() != null)
			throw result.getExecutionException();
		if (result.getExitCode() != 0)
			throw new IllegalStateException("Invocation failed with return code: " + result.getExitCode());
		return dependencyOutput.mavenModules;
	}
	
	public Set<MavenModule> getModifiedModules(Map<String,MavenModule> depsMap, Set<String> modifiedFiles) {
		Set<MavenModule> modifiedModules = new HashSet<>();
		for (String modifiedFile : modifiedFiles) {
			// Changes to the BOM have the ability to impact any other
			// project without further source code changes
			if (modifiedFile.startsWith("bom/"))
				return Collections.singleton(ALL_MODULES);
			modifiedModules.add(getModule(depsMap, modifiedFile));
		}
		
		// If only the parent module was modified, assume any child module could be impacted
		if (modifiedModules.size() == 1 && 
			"io.quarkus:quarkus-parent".equals(modifiedModules.iterator().next().name))
			return Collections.singleton(ALL_MODULES);
		return modifiedModules;
	}
	
	private String getTestsToRun() {
		
	}
	
	private MavenModule getModule(Map<String,MavenModule> depsMap, String filePath) {
		Path p = REPO_ROOT.resolve(filePath);
		Path pomXml = findPom(p);
		// Don't parse the same pom multiple times
		if (parsedModules.containsKey(pomXml))
			return parsedModules.get(pomXml);
		MavenXpp3Reader pomReader = new MavenXpp3Reader();
		try {
			Model pomModel = pomReader.read(new FileReader(pomXml.toFile()));
			String groupId = pomModel.getGroupId();
			if (groupId == null) {
				groupId = pomModel.getParent().getGroupId();
			}
			Objects.requireNonNull(groupId, "GroupId of module and parent was null for " + pomXml);
			String moduleName = groupId + ":" + pomModel.getArtifactId();
			MavenModule module = depsMap.get(moduleName);
			Objects.requireNonNull(module, "Did not find maven module " + moduleName + " in map");
			parsedModules.put(pomXml, module);
			return module;
		} catch (IOException | XmlPullParserException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private Path findPom(Path filePath) {
		if (filePath.toFile().isDirectory()) {
			// See if this folder has a pom.xml, otherwise go up a directory
			Path pom = filePath.resolve("pom.xml");
			if (pom.toFile().exists())
				return pom;
		}
		return findPom(filePath.getParent());
	}
	
	public Set<String> getModifiedFiles() throws IOException, InterruptedException {
		Process gitDiff = new ProcessBuilder("git", "diff", "--name-only", sha1 + ".." + sha2)
				.directory(REPO_ROOT.toFile())
				.start();
			if (!gitDiff.waitFor(30, TimeUnit.SECONDS))
				throw new IllegalStateException("Timed out waiting for git diff command to complete");
			BufferedReader reader = new BufferedReader(new InputStreamReader(gitDiff.getInputStream()));
			Set<String> modifiedFiles = new HashSet<>();
			String line = null;
			while ( (line = reader.readLine()) != null) {
				modifiedFiles.add(line);
			}
			return modifiedFiles;
	}
}
