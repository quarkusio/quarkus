import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class MavenModelReader {
    
    public static void main(String[] args) {
        boolean generateNxConfig = false;
        boolean useStdin = false;
        boolean useHierarchical = false;
        List<String> pomPaths = new ArrayList<>();
        
        // Parse arguments
        for (String arg : args) {
            if ("--nx".equals(arg)) {
                generateNxConfig = true;
            } else if ("--stdin".equals(arg)) {
                useStdin = true;
            } else if ("--hierarchical".equals(arg)) {
                useHierarchical = true;
            } else {
                pomPaths.add(arg);
            }
        }
        
        try {
            if (generateNxConfig) {
                if (useHierarchical) {
                    // Start from root pom.xml and traverse modules hierarchically
                    generateHierarchicalNxProjectConfigurations();
                } else if (useStdin) {
                    // Read from stdin if requested (legacy mode)
                    pomPaths = readPomPathsFromStdin();
                    generateSequentialNxProjectConfigurations(pomPaths);
                } else if (pomPaths.isEmpty()) {
                    // Default to hierarchical traversal from workspace root
                    generateHierarchicalNxProjectConfigurations();
                } else {
                    // Process specific paths
                    generateSequentialNxProjectConfigurations(pomPaths);
                }
            } else {
                // Single file analysis mode (backwards compatibility)
                if (pomPaths.size() == 1) {
                    Model model = readPomFile(pomPaths.get(0));
                    System.out.println("Project: " + model.getGroupId() + ":" + model.getArtifactId());
                }
            }
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static List<String> readPomPathsFromStdin() throws IOException {
        List<String> pomPaths = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                pomPaths.add(line);
            }
        }
        
        System.err.println("INFO: Read " + pomPaths.size() + " POM paths from stdin");
        return pomPaths;
    }
    
    private static void generateHierarchicalNxProjectConfigurations() {
        System.err.println("INFO: Starting hierarchical Maven module traversal");
        long startTime = System.currentTimeMillis();
        
        // Create output file in workspace root
        String outputFile = System.getProperty("maven.output.file", "maven-results.json");
        System.err.println("INFO: Writing results to " + outputFile);
        
        try {
            // Start from workspace root pom.xml
            String workspaceRoot = System.getProperty("user.dir");
            String rootPomPath = workspaceRoot + "/pom.xml";
            File rootPomFile = new File(rootPomPath);
            
            if (!rootPomFile.exists()) {
                throw new Exception("Root pom.xml not found at: " + rootPomPath);
            }
            
            // PASS 1: Discover all project names first
            List<String> processedPomPaths = new ArrayList<>();
            List<String> discoveredProjects = new ArrayList<>();
            int[] stats = {0, 0}; // [processed, successful]
            
            System.err.println("INFO: Pass 1 - Discovering all project names");
            discoverAllProjects(rootPomPath, workspaceRoot, processedPomPaths, discoveredProjects, stats);
            
            System.err.println("INFO: Pass 1 completed - Discovered " + discoveredProjects.size() + " projects");
            
            // PASS 2: Generate configurations with filtered dependencies
            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write("{\n");
                
                // Reset for second pass
                processedPomPaths.clear();
                stats[0] = 0; stats[1] = 0;
                boolean[] firstProject = {true};
                
                System.err.println("INFO: Pass 2 - Generating configurations with filtered dependencies");
                traverseModulesWithFiltering(rootPomPath, workspaceRoot, processedPomPaths, discoveredProjects, stats, firstProject, writer);
                
                writer.write("\n}\n");
            }
            
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("INFO: Hierarchical processing completed in " + duration + "ms");
            System.err.println("INFO: Final results - Total: " + stats[0] + ", Successful: " + stats[1]);
            System.err.println("INFO: Results written to " + outputFile);
            
            // Output success message to stdout for TypeScript
            System.out.println("SUCCESS: " + outputFile);
            
        } catch (Exception e) {
            System.err.println("ERROR: Failed to generate hierarchical configurations: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void traverseModules(String pomPath, String workspaceRoot, List<String> processedPomPaths, 
                                        List<String> discoveredProjects, int[] stats, boolean[] firstProject, FileWriter writer) throws Exception {
        
        // Skip if already processed (avoid cycles)
        if (processedPomPaths.contains(pomPath)) {
            return;
        }
        
        File pomFile = new File(pomPath);
        if (!pomFile.exists()) {
            System.err.println("WARN: POM file not found: " + pomPath);
            return;
        }
        
        // Read and process this POM
        try {
            stats[0]++; // increment processed count
            processedPomPaths.add(pomPath);
            
            Model model = readPomFile(pomPath);
            String packaging = model.getPackaging();
            
            // Process this project (even if it's a parent POM)
            processOneFileLightweightToFile(pomPath, firstProject[0], writer, stats[0]);
            stats[1]++; // increment successful count
            firstProject[0] = false;
            
            // Track the discovered project name for dependency filtering
            String artifactId = model.getArtifactId();
            String groupId = model.getGroupId();
            if (groupId == null && model.getParent() != null) {
                groupId = model.getParent().getGroupId();
            }
            String projectName = (groupId != null ? groupId : "unknown") + ":" + artifactId;
            discoveredProjects.add(projectName);
            
            // If this is a parent POM with modules, traverse child modules
            if ("pom".equals(packaging) && model.getModules() != null && !model.getModules().isEmpty()) {
                String parentDir = pomFile.getParent();
                
                System.err.println("INFO: Processing " + model.getModules().size() + " modules from " + pomPath);
                
                for (String module : model.getModules()) {
                    String childPomPath = parentDir + "/" + module + "/pom.xml";
                    
                    // Recursively traverse child modules
                    traverseModules(childPomPath, workspaceRoot, processedPomPaths, discoveredProjects, stats, firstProject, writer);
                }
            }
            
            // Progress reporting every 50 projects
            if (stats[0] % 50 == 0) {
                System.err.println("INFO: Progress: " + stats[0] + " processed, " + stats[1] + " successful");
                System.gc(); // Force cleanup
            }
            
        } catch (Exception e) {
            System.err.println("ERROR: Failed to process " + pomPath + ": " + e.getMessage());
            // Continue processing other modules
        }
    }
    
    private static void discoverAllProjects(String pomPath, String workspaceRoot, List<String> processedPomPaths, 
                                           List<String> discoveredProjects, int[] stats) throws Exception {
        // Skip if already processed (avoid cycles)
        if (processedPomPaths.contains(pomPath)) {
            return;
        }
        
        File pomFile = new File(pomPath);
        if (!pomFile.exists()) {
            return;
        }
        
        try {
            stats[0]++; // increment processed count
            processedPomPaths.add(pomPath);
            
            Model model = readPomFile(pomPath);
            
            // Get project name
            String artifactId = model.getArtifactId();
            String groupId = model.getGroupId();
            if (groupId == null && model.getParent() != null) {
                groupId = model.getParent().getGroupId();
            }
            String projectName = (groupId != null ? groupId : "unknown") + ":" + artifactId;
            discoveredProjects.add(projectName);
            
            // Traverse child modules if this is a parent POM
            String packaging = model.getPackaging();
            if ("pom".equals(packaging) && model.getModules() != null && !model.getModules().isEmpty()) {
                String parentDir = pomFile.getParent();
                for (String module : model.getModules()) {
                    String childPomPath = parentDir + "/" + module + "/pom.xml";
                    discoverAllProjects(childPomPath, workspaceRoot, processedPomPaths, discoveredProjects, stats);
                }
            }
            
        } catch (Exception e) {
            // Continue processing other modules
        }
    }
    
    private static void traverseModulesWithFiltering(String pomPath, String workspaceRoot, List<String> processedPomPaths, 
                                                    List<String> discoveredProjects, int[] stats, boolean[] firstProject, FileWriter writer) throws Exception {
        // Skip if already processed (avoid cycles)
        if (processedPomPaths.contains(pomPath)) {
            return;
        }
        
        File pomFile = new File(pomPath);
        if (!pomFile.exists()) {
            return;
        }
        
        try {
            stats[0]++; // increment processed count
            processedPomPaths.add(pomPath);
            
            Model model = readPomFile(pomPath);
            String packaging = model.getPackaging();
            
            // Process this project with filtered dependencies
            processOneFileLightweightToFileWithFiltering(pomPath, firstProject[0], writer, stats[0], discoveredProjects);
            stats[1]++; // increment successful count
            firstProject[0] = false;
            
            // If this is a parent POM with modules, traverse child modules
            if ("pom".equals(packaging) && model.getModules() != null && !model.getModules().isEmpty()) {
                String parentDir = pomFile.getParent();
                for (String module : model.getModules()) {
                    String childPomPath = parentDir + "/" + module + "/pom.xml";
                    traverseModulesWithFiltering(childPomPath, workspaceRoot, processedPomPaths, discoveredProjects, stats, firstProject, writer);
                }
            }
            
        } catch (Exception e) {
            // Continue processing other modules
        }
    }
    
    private static void generateSequentialNxProjectConfigurations(List<String> pomPaths) {
        System.err.println("INFO: Starting sequential processing of " + pomPaths.size() + " Maven projects");
        long startTime = System.currentTimeMillis();
        
        // Create output file in workspace root
        String outputFile = System.getProperty("maven.output.file", "maven-results.json");
        System.err.println("INFO: Writing results to " + outputFile);
        
        try (FileWriter writer = new FileWriter(outputFile)) {
            // Output JSON start to file
            writer.write("{\n");
            
            int processed = 0;
            int successful = 0;
            boolean firstProject = true;
            
            for (String pomPath : pomPaths) {
                processed++;
                
                try {
                    // Process ONE file at a time with minimal memory footprint
                    processOneFileLightweightToFile(pomPath, firstProject, writer, processed);
                    successful++;
                    firstProject = false;
                    
                    // Progress reporting and GC every 50 files
                    if (processed % 50 == 0) {
                        System.err.println("INFO: Progress: " + processed + "/" + pomPaths.size() + 
                            " (" + (processed * 100 / pomPaths.size()) + "%) - Successful: " + successful);
                        System.gc(); // Force cleanup
                    }
                    
                } catch (Exception e) {
                    System.err.println("ERROR: Failed to process " + pomPath + ": " + e.getMessage());
                    // Continue processing other files
                }
            }
            
            // Close JSON in file
            writer.write("\n}\n");
            
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("INFO: Sequential processing completed in " + duration + "ms");
            System.err.println("INFO: Final results - Total: " + processed + ", Successful: " + successful);
            System.err.println("INFO: Results written to " + outputFile);
            
            // Output success message to stdout for TypeScript
            System.out.println("SUCCESS: " + outputFile);
            
        } catch (IOException e) {
            System.err.println("ERROR: Failed to write output file: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static void processOneFileLightweight(String pomPath, boolean isFirst) throws Exception {
        Model model = null;
        try {
            // Read and immediately process
            model = readPomFile(pomPath);
            String artifactId = model.getArtifactId();
            String groupId = model.getGroupId();
            
            // Use parent groupId if not defined locally
            if (groupId == null && model.getParent() != null) {
                groupId = model.getParent().getGroupId();
            }
            
            // Create unique project name: groupId:artifactId
            String projectName = (groupId != null ? groupId : "unknown") + ":" + artifactId;
            
            // Get project root
            File pomFile = new File(pomPath);
            String projectRoot = pomFile.getParent();
            if (projectRoot == null) {
                projectRoot = ".";
            }
            
            // Convert absolute path to relative path
            if (projectRoot.startsWith("/")) {
                String workspaceRoot = System.getProperty("user.dir");
                if (projectRoot.startsWith(workspaceRoot + "/")) {
                    projectRoot = projectRoot.substring(workspaceRoot.length() + 1);
                }
            }
            
            // Count internal dependencies only (no complex objects)
            int internalDepCount = 0;
            if (model.getDependencies() != null) {
                for (Dependency dep : model.getDependencies()) {
                    if (isInternalDependency(dep.getGroupId(), dep.getArtifactId())) {
                        internalDepCount++;
                    }
                }
            }
            
            // Output minimal JSON immediately
            if (!isFirst) {
                System.out.println(",");
            }
            
            System.out.print("  \"" + escapeJsonString(projectRoot) + "\": {");
            System.out.print("\"name\":\"" + escapeJsonString(projectName) + "\",");
            System.out.print("\"projectType\":\"library\",");
            System.out.print("\"internalDeps\":" + internalDepCount);
            System.out.print("}");
            
        } finally {
            // Explicitly clear references
            model = null;
        }
    }
    
    private static void processOneFileLightweightToFile(String pomPath, boolean isFirst, FileWriter writer, int processed) throws Exception {
        Model model = null;
        try {
            // Only log every 10 projects to reduce output
            if (processed % 10 == 1) {
                System.err.println("DEBUG: Processing " + pomPath);
            }
            
            // Read and immediately process
            model = readPomFile(pomPath);
            String artifactId = model.getArtifactId();
            String groupId = model.getGroupId();
            
            // Use parent groupId if not defined locally
            if (groupId == null && model.getParent() != null) {
                groupId = model.getParent().getGroupId();
            }
            
            // Create unique project name: groupId:artifactId
            String projectName = (groupId != null ? groupId : "unknown") + ":" + artifactId;
            
            // Get project root
            File pomFile = new File(pomPath);
            String projectRoot = pomFile.getParent();
            if (projectRoot == null) {
                projectRoot = ".";
            }
            
            // Convert absolute path to relative path
            if (projectRoot.startsWith("/")) {
                String workspaceRoot = System.getProperty("user.dir");
                if (projectRoot.startsWith(workspaceRoot + "/")) {
                    projectRoot = projectRoot.substring(workspaceRoot.length() + 1);
                }
            }
            
            // Count internal dependencies and collect their names
            List<String> internalDeps = new ArrayList<>();
            if (model.getDependencies() != null) {
                for (Dependency dep : model.getDependencies()) {
                    if (isInternalDependency(dep.getGroupId(), dep.getArtifactId())) {
                        // Use the same groupId:artifactId format for dependencies
                        String depName = dep.getGroupId() + ":" + dep.getArtifactId();
                        internalDeps.add(depName);
                    }
                }
            }
            
            // Write to file
            if (!isFirst) {
                writer.write(",\n");
            }
            
            writer.write("  \"" + escapeJsonString(projectRoot) + "\": {\n");
            writer.write("    \"name\": \"" + escapeJsonString(projectName) + "\",\n");
            writer.write("    \"projectType\": \"library\",\n");
            writer.write("    \"implicitDependencies\": {\n");
            writer.write("      \"projects\": [");
            for (int i = 0; i < internalDeps.size(); i++) {
                if (i > 0) writer.write(", ");
                writer.write("\"" + escapeJsonString(internalDeps.get(i)) + "\"");
            }
            writer.write("]\n");
            writer.write("    }\n");
            writer.write("  }");
            
            // Only log every 25 projects to reduce output
            if (processed % 25 == 0 || processed == 1) {
                System.err.println("DEBUG: Processed " + projectName + " (" + processed + " total)");
            }
            
        } finally {
            // Explicitly clear references
            model = null;
        }
    }
    
    private static void processOneFileLightweightToFileWithFiltering(String pomPath, boolean isFirst, FileWriter writer, int processed, List<String> discoveredProjects) throws Exception {
        Model model = null;
        try {
            // Only log every 10 projects to reduce output
            if (processed % 10 == 1) {
                System.err.println("DEBUG: Processing " + pomPath);
            }
            
            // Read and immediately process
            model = readPomFile(pomPath);
            String artifactId = model.getArtifactId();
            String groupId = model.getGroupId();
            
            // Use parent groupId if not defined locally
            if (groupId == null && model.getParent() != null) {
                groupId = model.getParent().getGroupId();
            }
            
            // Create unique project name: groupId:artifactId
            String projectName = (groupId != null ? groupId : "unknown") + ":" + artifactId;
            
            // Get project root
            File pomFile = new File(pomPath);
            String projectRoot = pomFile.getParent();
            if (projectRoot == null) {
                projectRoot = ".";
            }
            
            // Convert absolute path to relative path
            if (projectRoot.startsWith("/")) {
                String workspaceRoot = System.getProperty("user.dir");
                if (projectRoot.startsWith(workspaceRoot + "/")) {
                    projectRoot = projectRoot.substring(workspaceRoot.length() + 1);
                }
            }
            
            // Filter internal dependencies to only include discovered projects
            List<String> internalDeps = new ArrayList<>();
            if (model.getDependencies() != null) {
                for (Dependency dep : model.getDependencies()) {
                    if (isInternalDependency(dep.getGroupId(), dep.getArtifactId())) {
                        String depName = dep.getGroupId() + ":" + dep.getArtifactId();
                        // Only include if the dependency is in our discovered projects list
                        if (discoveredProjects.contains(depName)) {
                            internalDeps.add(depName);
                        }
                    }
                }
            }
            
            // Write to file
            if (!isFirst) {
                writer.write(",\n");
            }
            
            writer.write("  \"" + escapeJsonString(projectRoot) + "\": {\n");
            writer.write("    \"name\": \"" + escapeJsonString(projectName) + "\",\n");
            writer.write("    \"projectType\": \"library\",\n");
            writer.write("    \"implicitDependencies\": {\n");
            writer.write("      \"projects\": [");
            for (int i = 0; i < internalDeps.size(); i++) {
                if (i > 0) writer.write(", ");
                writer.write("\"" + escapeJsonString(internalDeps.get(i)) + "\"");
            }
            writer.write("]\n");
            writer.write("    }\n");
            writer.write("  }");
            
            // Only log every 25 projects to reduce output
            if (processed % 25 == 0 || processed == 1) {
                System.err.println("DEBUG: Processed " + projectName + " with " + internalDeps.size() + " filtered dependencies (" + processed + " total)");
            }
            
        } finally {
            // Explicitly clear references
            model = null;
        }
    }
    
    private static Model readPomFile(String pomPath) throws Exception {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        return reader.read(new FileReader(pomPath));
    }
    
    private static Map<String, Object> generateNxProjectConfigurationMap(Model model) {
        Map<String, Object> config = new LinkedHashMap<>();
        
        // Basic project info
        String artifactId = model.getArtifactId();
        String projectName = artifactId.replace("quarkus-", "").replace("-", "_");
        config.put("name", projectName);
        config.put("projectType", "library");
        
        // Dependencies analysis
        Map<String, Object> implicitDependencies = new LinkedHashMap<>();
        List<String> projects = new ArrayList<>();
        List<String> inheritsFrom = new ArrayList<>();
        
        // Analyze Maven dependencies
        if (model.getDependencies() != null) {
            System.err.println("DEBUG: Analyzing " + model.getDependencies().size() + " dependencies for " + artifactId);
            
            for (Dependency dep : model.getDependencies()) {
                if (isInternalDependency(dep)) {
                    String depName = dep.getArtifactId().replace("quarkus-", "").replace("-", "_");
                    projects.add(depName);
                    System.err.println("DEBUG: Found internal dependency: " + dep.getGroupId() + ":" + dep.getArtifactId());
                }
            }
        }
        
        // Add parent dependency if exists
        if (model.getParent() != null && isInternalDependency(model.getParent().getGroupId(), model.getParent().getArtifactId())) {
            String parentName = model.getParent().getArtifactId().replace("quarkus-", "").replace("-", "_");
            inheritsFrom.add(parentName);
        }
        
        implicitDependencies.put("projects", projects);
        implicitDependencies.put("inheritsFrom", inheritsFrom);
        config.put("implicitDependencies", implicitDependencies);
        
        // Targets
        Map<String, Object> targets = new LinkedHashMap<>();
        targets.put("build", createBuildTarget());
        targets.put("test", createTestTarget());
        config.put("targets", targets);
        
        return config;
    }
    
    private static boolean isInternalDependency(Dependency dep) {
        return isInternalDependency(dep.getGroupId(), dep.getArtifactId());
    }
    
    private static boolean isInternalDependency(String groupId, String artifactId) {
        if (groupId == null || artifactId == null) {
            return false;
        }
        
        // More conservative check for Quarkus internal dependencies
        // Only include artifacts that are likely to be in the workspace
        if (!groupId.equals("io.quarkus")) {
            return false;
        }
        
        // Exclude known external dependencies that are not part of the workspace
        if (artifactId.equals("quarkus-fs-util") || 
            artifactId.equals("quarkus-spring-context-api") ||
            artifactId.equals("quarkus-spring-data-rest-api") ||
            artifactId.contains("-api") && !artifactId.endsWith("-deployment")) {
            return false;
        }
        
        return artifactId.startsWith("quarkus-") || artifactId.equals("arc");
    }
    
    private static Map<String, Object> createBuildTarget() {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("executor", "@nx/maven:build");
        
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("command", "compile");
        target.put("options", options);
        
        return target;
    }
    
    private static Map<String, Object> createTestTarget() {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("executor", "@nx/maven:test");
        
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("command", "test");
        target.put("options", options);
        
        return target;
    }
    
    private static String mapToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(escapeJsonString(entry.getKey())).append("\":");
            json.append(objectToJson(entry.getValue()));
            first = false;
        }
        
        json.append("}");
        return json.toString();
    }
    
    private static String objectToJson(Object obj) {
        if (obj == null) {
            return "null";
        } else if (obj instanceof String) {
            return "\"" + escapeJsonString((String) obj) + "\"";
        } else if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) json.append(",");
                json.append(objectToJson(list.get(i)));
            }
            json.append("]");
            return json.toString();
        } else if (obj instanceof Map) {
            return mapToJson((Map<String, Object>) obj);
        } else {
            return "\"" + escapeJsonString(obj.toString()) + "\"";
        }
    }
    
    private static String escapeJsonString(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}