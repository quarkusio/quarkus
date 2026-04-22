package io.quarkus.devui.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.jboss.logging.Logger;
import org.yaml.snakeyaml.Yaml;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ConfigDescriptionBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devui.spi.buildtime.BuildTimeData;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * Scans active extensions for {@code META-INF/quarkus-skill.md} files and composes
 * MCP resources that combine the skill content with extension metadata and configuration
 * properties.
 * <p>
 * Additionally, writes composed skills as Agent Skills spec-compliant {@code SKILL.md} files
 * to the project's {@code .agents/skills/} directory so that any compatible AI coding agent
 * (Claude Code, Cursor, Copilot, etc.) can discover them via filesystem scanning.
 * <p>
 * This enables AI coding agents to receive extension-specific coding guidelines, testing
 * patterns, and configuration reference in a single resource.
 */
public class SkillProcessor {

    private static final Logger log = Logger.getLogger(SkillProcessor.class);

    private static final String SKILL_PATH = "META-INF/quarkus-skill.md";
    private static final String SKILLS_NAMESPACE_PREFIX = "skills-";
    private static final String AGENTS_SKILLS_DIR = ".agents/skills";
    private static final String QUARKUS_SKILL_PREFIX = "io-quarkus-";

    @BuildStep(onlyIf = IsDevelopment.class)
    void collectExtensionSkills(
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            BuildSystemTargetBuildItem buildSystemTarget,
            List<ConfigDescriptionBuildItem> configDescriptionBuildItems,
            BuildProducer<BuildTimeConstBuildItem> buildTimeConstProducer) {

        Yaml yaml = new Yaml();
        // Group skill data by groupId so MCP resource names become: skills-<groupId>_<artifactId>
        Map<String, Map<String, BuildTimeData>> skillDataByGroup = new HashMap<>();
        Map<String, String> agentSkills = new HashMap<>();

        // Build a lookup of deployment dependencies so we can find skill files in deployment jars
        Map<String, ResolvedDependency> deploymentDeps = new HashMap<>();
        for (ResolvedDependency dep : curateOutcomeBuildItem.getApplicationModel()
                .getDependencies(DependencyFlags.DEPLOYMENT_CP)) {
            deploymentDeps.put(dep.getGroupId() + ":" + dep.getArtifactId(), dep);
        }

        for (ResolvedDependency runtimeExt : curateOutcomeBuildItem.getApplicationModel()
                .getDependencies(DependencyFlags.RUNTIME_EXTENSION_ARTIFACT)) {

            // Read extension metadata
            ExtensionInfo extInfo = readExtensionMetadata(runtimeExt, yaml);
            if (extInfo == null) {
                continue;
            }

            // Read skill file from the deployment artifact, falling back to the runtime artifact
            String deploymentKey = runtimeExt.getGroupId() + ":" + runtimeExt.getArtifactId() + "-deployment";
            ResolvedDependency deploymentDep = deploymentDeps.get(deploymentKey);
            String skillContent = deploymentDep != null ? readSkillFile(deploymentDep) : null;
            if (skillContent == null) {
                skillContent = readSkillFile(runtimeExt);
            }
            if (skillContent == null) {
                continue;
            }

            // Gather config properties for this extension
            List<ConfigDescriptionBuildItem> extensionConfigs = filterConfigsForExtension(
                    configDescriptionBuildItems, extInfo.configPrefixes);

            // Compose the full skill resource
            String composed = composeSkillResource(extInfo, skillContent, extensionConfigs);

            String namespace = toSkillNamespace(runtimeExt);
            String resourceName = toSkillResourceName(runtimeExt);
            skillDataByGroup
                    .computeIfAbsent(namespace, k -> new HashMap<>())
                    .put(resourceName, new BuildTimeData(
                            composed,
                            "Coding skill and guidelines for " + extInfo.name
                                    + ". Contains patterns, testing guidelines, and configuration reference.",
                            true,
                            "text/markdown"));

            // Also prepare for .agents/skills/ output
            String agentSkillName = toAgentSkillName(runtimeExt);
            String agentSkillContent = composeAgentSkillFile(agentSkillName, extInfo, composed);
            agentSkills.put(agentSkillName, agentSkillContent);

            log.debugf("Registered skill resource for extension: %s", extInfo.name);
        }

        for (Map.Entry<String, Map<String, BuildTimeData>> entry : skillDataByGroup.entrySet()) {
            buildTimeConstProducer.produce(new BuildTimeConstBuildItem(entry.getKey(), entry.getValue()));
        }

        // Write .agents/skills/ files (including base Quarkus skill)
        Path projectRoot = buildSystemTarget.getOutputDirectory().getParent();
        if (projectRoot != null && Files.exists(projectRoot)) {
            agentSkills.put("quarkus", composeBaseQuarkusSkill());
            writeAgentSkills(projectRoot, agentSkills);
        }
    }

    private ExtensionInfo readExtensionMetadata(ResolvedDependency runtimeExt, Yaml yaml) {
        ExtensionInfo[] result = new ExtensionInfo[1];
        runtimeExt.getContentTree().accept(BootstrapConstants.EXTENSION_METADATA_PATH, visit -> {
            if (visit == null) {
                return;
            }
            try {
                String content;
                try (Scanner scanner = new Scanner(Files.newBufferedReader(visit.getPath(), StandardCharsets.UTF_8))) {
                    scanner.useDelimiter("\\A");
                    content = scanner.hasNext() ? scanner.next() : null;
                }
                if (content == null) {
                    return;
                }
                Map<String, Object> extMap = yaml.load(content);
                if (extMap == null || !extMap.containsKey("name")) {
                    return;
                }

                ExtensionInfo info = new ExtensionInfo();
                info.name = (String) extMap.get("name");
                info.description = (String) extMap.getOrDefault("description", null);

                Map<String, Object> metadata = (Map<String, Object>) extMap.getOrDefault("metadata", null);
                if (metadata != null) {
                    info.guide = (String) metadata.getOrDefault("guide", null);
                    info.categories = (List<String>) metadata.getOrDefault("categories", null);
                    info.configPrefixes = (List<String>) metadata.getOrDefault("config", null);
                    info.quickstart = (String) metadata.getOrDefault("quickstart", null);
                    info.keywords = (List<String>) metadata.getOrDefault("keywords", null);
                    info.status = (String) metadata.getOrDefault("status", null);
                }
                result[0] = info;
            } catch (IOException e) {
                log.debugf("Failed to read extension metadata from %s: %s",
                        runtimeExt.toCompactCoords(), e.getMessage());
            }
        });
        return result[0];
    }

    private String readSkillFile(ResolvedDependency dependency) {
        String[] result = new String[1];
        dependency.getContentTree().accept(SKILL_PATH, visit -> {
            if (visit == null) {
                return;
            }
            try {
                result[0] = Files.readString(visit.getPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.debugf("Failed to read skill file from %s: %s",
                        dependency.toCompactCoords(), e.getMessage());
            }
        });
        return result[0];
    }

    private List<ConfigDescriptionBuildItem> filterConfigsForExtension(
            List<ConfigDescriptionBuildItem> allConfigs, List<String> configPrefixes) {
        if (configPrefixes == null || configPrefixes.isEmpty()) {
            return List.of();
        }
        List<ConfigDescriptionBuildItem> matched = new ArrayList<>();
        for (ConfigDescriptionBuildItem config : allConfigs) {
            for (String prefix : configPrefixes) {
                if (config.getPropertyName().startsWith(prefix)) {
                    matched.add(config);
                    break;
                }
            }
        }
        return matched;
    }

    private String composeSkillResource(ExtensionInfo extInfo, String skillContent,
            List<ConfigDescriptionBuildItem> configs) {
        StringBuilder sb = new StringBuilder();

        // Header with extension metadata
        sb.append("# ").append(extInfo.name).append("\n\n");

        if (extInfo.description != null && !extInfo.description.isBlank()) {
            sb.append("> ").append(extInfo.description).append("\n");
        }
        if (extInfo.guide != null && !extInfo.guide.isBlank()) {
            sb.append("> Guide: ").append(extInfo.guide).append("\n");
        }
        if (extInfo.quickstart != null && !extInfo.quickstart.isBlank()) {
            sb.append("> Quickstart: https://github.com/quarkusio/quarkus-quickstarts/tree/development/")
                    .append(extInfo.quickstart).append("\n");
        }
        if (extInfo.categories != null && !extInfo.categories.isEmpty()) {
            sb.append("> Categories: ").append(String.join(", ", extInfo.categories)).append("\n");
        }
        sb.append("\n");

        // Skill content (the extension-authored guidelines)
        sb.append("## Patterns and Guidelines\n\n");
        sb.append(skillContent.trim()).append("\n\n");

        // Configuration reference
        if (!configs.isEmpty()) {
            sb.append("## Configuration Reference\n\n");
            sb.append("| Property | Type | Default | Description |\n");
            sb.append("|----------|------|---------|-------------|\n");

            // Sort by property name for readability
            configs.stream()
                    .sorted(Comparator.comparing(ConfigDescriptionBuildItem::getPropertyName))
                    .forEach(config -> {
                        String name = escapeMarkdownPipe(config.getPropertyName());
                        String type = config.getValueTypeName() != null
                                ? escapeMarkdownPipe(simplifyTypeName(config.getValueTypeName()))
                                : "";
                        String defaultVal = config.getDefaultValue() != null
                                ? escapeMarkdownPipe(config.getDefaultValue())
                                : "";
                        String docs = config.getDocs() != null
                                ? escapeMarkdownPipe(truncateDescription(config.getDocs()))
                                : "";
                        sb.append("| `").append(name).append("` | ").append(type)
                                .append(" | ").append(defaultVal)
                                .append(" | ").append(docs).append(" |\n");
                    });
        }

        return sb.toString();
    }

    // --- Agent Skills spec (.agents/skills/) support ---

    /**
     * Derives an Agent Skills spec-compliant name from the dependency's groupId and artifactId.
     * Uses the artifact coordinates to guarantee uniqueness (extension display names can collide).
     * The spec requires: lowercase, hyphens only, no consecutive hyphens,
     * no leading/trailing hyphens, max 64 chars.
     */
    private String toAgentSkillName(ResolvedDependency dep) {
        String raw = dep.getGroupId() + "-" + dep.getArtifactId();
        String normalized = raw.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        if (normalized.length() > 64) {
            normalized = normalized.substring(0, 64).replaceAll("-$", "");
        }
        return normalized;
    }

    /**
     * Composes a SKILL.md file with YAML frontmatter per the Agent Skills spec.
     */
    private String composeAgentSkillFile(String skillName, ExtensionInfo extInfo, String composedContent) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("name: ").append(skillName).append("\n");
        sb.append("description: >-\n  Coding patterns, testing guidelines, and configuration reference for the ")
                .append(extInfo.name).append(" Quarkus extension.\n");
        if (extInfo.categories != null && !extInfo.categories.isEmpty()) {
            sb.append("categories: \"").append(String.join(", ", extInfo.categories)).append("\"\n");
        }
        if ((extInfo.guide != null && !extInfo.guide.isBlank())
                || (extInfo.quickstart != null && !extInfo.quickstart.isBlank())) {
            sb.append("metadata:\n");
            if (extInfo.guide != null && !extInfo.guide.isBlank()) {
                sb.append("  guide: \"").append(extInfo.guide).append("\"\n");
            }
            if (extInfo.quickstart != null && !extInfo.quickstart.isBlank()) {
                sb.append("  quickstart: \"https://github.com/quarkusio/quarkus-quickstarts/tree/development/")
                        .append(extInfo.quickstart).append("\"\n");
            }
        }
        sb.append("---\n\n");
        sb.append(composedContent);
        return sb.toString();
    }

    private static final String BASE_SKILL_RESOURCE = "META-INF/quarkus-base-skill.md";

    /**
     * Reads the base Quarkus skill from a resource file.
     * This covers fundamentals applicable to all Quarkus projects.
     */
    private String composeBaseQuarkusSkill() {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(BASE_SKILL_RESOURCE)) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warnf("Failed to read base skill resource %s: %s", BASE_SKILL_RESOURCE, e.getMessage());
        }
        log.warn("Base Quarkus skill resource not found: " + BASE_SKILL_RESOURCE);
        return "";
    }

    /**
     * Writes Agent Skills spec-compliant SKILL.md files to the project's .agents/skills/ directory.
     * Cleans up previously generated Quarkus skill directories that are no longer needed.
     */
    private void writeAgentSkills(Path projectRoot, Map<String, String> skills) {
        Path skillsDir = projectRoot.resolve(AGENTS_SKILLS_DIR);

        try {
            // Clean up old Quarkus-generated skills
            Set<String> currentSkillNames = new HashSet<>(skills.keySet());
            if (Files.isDirectory(skillsDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(skillsDir, QUARKUS_SKILL_PREFIX + "*")) {
                    for (Path dir : stream) {
                        if (Files.isDirectory(dir) && !currentSkillNames.contains(dir.getFileName().toString())) {
                            // This skill was generated before but the extension is no longer present
                            Path skillFile = dir.resolve("SKILL.md");
                            Files.deleteIfExists(skillFile);
                            Files.deleteIfExists(dir);
                            log.debugf("Removed stale agent skill: %s", dir.getFileName());
                        }
                    }
                }
            }

            // Write current skills
            for (Map.Entry<String, String> entry : skills.entrySet()) {
                Path skillDir = skillsDir.resolve(entry.getKey());
                Files.createDirectories(skillDir);
                Path skillFile = skillDir.resolve("SKILL.md");
                Files.writeString(skillFile, entry.getValue(), StandardCharsets.UTF_8);
            }

            log.debugf("Wrote %d agent skills to %s", skills.size(), skillsDir);
        } catch (IOException e) {
            log.warnf("Failed to write agent skills to %s: %s", skillsDir, e.getMessage());
        }
    }

    // --- Utility methods ---

    /**
     * Derives the MCP namespace from the groupId, e.g. "io.quarkus" becomes "skills-io-quarkus".
     */
    private String toSkillNamespace(ResolvedDependency dep) {
        String groupIdHyphenated = dep.getGroupId().replace('.', '-');
        return SKILLS_NAMESPACE_PREFIX + groupIdHyphenated;
    }

    /**
     * Derives the MCP resource key from the artifactId in camelCase.
     * The build-time data system uses {@code namespace_constname} format with underscore
     * as separator, so const names must not contain underscores or hyphens.
     */
    private String toSkillResourceName(ResolvedDependency dep) {
        String[] words = dep.getArtifactId().toLowerCase().split("[^a-z0-9]+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (sb.isEmpty()) {
                sb.append(word);
            } else {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1));
                }
            }
        }
        return sb.toString();
    }

    private String simplifyTypeName(String typeName) {
        if (typeName == null) {
            return "";
        }
        // Strip java.lang. and java.util. prefixes for readability
        return typeName
                .replace("java.lang.", "")
                .replace("java.util.", "")
                .replace("java.time.", "");
    }

    private String escapeMarkdownPipe(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("|", "\\|").replace("\n", " ").replace("\r", "");
    }

    private String truncateDescription(String docs) {
        if (docs == null) {
            return "";
        }
        // Strip HTML tags and truncate for table readability
        String clean = docs.replaceAll("<[^>]+>", "").trim();
        if (clean.length() > 200) {
            return clean.substring(0, 197) + "...";
        }
        return clean;
    }

    private static class ExtensionInfo {
        String name;
        String description;
        String guide;
        String quickstart;
        List<String> categories;
        List<String> configPrefixes;
        List<String> keywords;
        String status;
    }
}
