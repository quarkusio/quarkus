//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.quarkus.platform:quarkus-bom:3.8.1@pom
//DEPS io.quarkus:quarkus-picocli

//JAVAC_OPTIONS -parameters
//JAVA_OPTIONS -Djava.util.logging.manager=org.jboss.logmanager.LogManager

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.function.Function;

import org.jboss.logging.Logger;

import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import picocli.CommandLine.Command;

@Command(name = "generaterelocations", mixinStandardHelpOptions = true)
public class generaterelocations implements Runnable {

    private static final Logger LOG = Logger.getLogger(generaterelocations.class);

    private static final Map<String, Function<String, Relocation>> RELOCATIONS = new TreeMap<>();

    static {
        Function<String, Relocation> resteasyReactiveRelocationFunction = a -> Relocation.ofArtifactId(a, a.replace("resteasy-reactive", "rest"),
                "3.9");
        RELOCATIONS.put("quarkus-resteasy-reactive", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-deployment", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-common", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-common-deployment", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-server-common", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-server-spi-deployment", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-jackson", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-jackson-deployment", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-jackson-common", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-jackson-common-deployment", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-jsonb", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-jsonb-deployment", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-jsonb-common", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-jsonb-common-deployment", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-jaxb", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-jaxb-deployment", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-jaxb-common", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-jaxb-common-deployment", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-kotlin", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-kotlin-deployment", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-kotlin-serialization", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-kotlin-serialization-deployment", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-kotlin-serialization-common", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-kotlin-serialization-common-deployment", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-links", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-links-deployment", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-servlet", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-servlet-deployment", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-qute", resteasyReactiveRelocationFunction);
        RELOCATIONS.put("quarkus-resteasy-reactive-qute-deployment", resteasyReactiveRelocationFunction);

        Function<String, Relocation>  restClientReactiveRelocation = a -> Relocation.ofArtifactId(a, a.replace("rest-client-reactive", "rest-client"),
                "3.9");
        RELOCATIONS.put("quarkus-rest-client-reactive", restClientReactiveRelocation);
        RELOCATIONS.put("quarkus-rest-client-reactive-deployment", restClientReactiveRelocation);
        RELOCATIONS.put("quarkus-rest-client-reactive-jackson", restClientReactiveRelocation);
        RELOCATIONS.put("quarkus-rest-client-reactive-jackson-deployment", restClientReactiveRelocation);
        RELOCATIONS.put("quarkus-rest-client-reactive-jsonb", restClientReactiveRelocation);
        RELOCATIONS.put("quarkus-rest-client-reactive-jsonb-deployment", restClientReactiveRelocation);
        RELOCATIONS.put("quarkus-rest-client-reactive-jaxb", restClientReactiveRelocation);
        RELOCATIONS.put("quarkus-rest-client-reactive-jaxb-deployment", restClientReactiveRelocation);
        RELOCATIONS.put("quarkus-rest-client-reactive-spi-deployment", restClientReactiveRelocation);
        RELOCATIONS.put("quarkus-rest-client-reactive-kotlin-serialization", restClientReactiveRelocation);
        RELOCATIONS.put("quarkus-rest-client-reactive-kotlin-serialization-deployment", restClientReactiveRelocation);

        Function<String, Relocation>  jaxrsClientReactiveRelocation = a -> Relocation.ofArtifactId(a, a.replace("jaxrs-client-reactive", "rest-client-jaxrs"),
                "3.9");
        RELOCATIONS.put("quarkus-jaxrs-client-reactive", jaxrsClientReactiveRelocation);
        RELOCATIONS.put("quarkus-jaxrs-client-reactive-deployment", jaxrsClientReactiveRelocation);

        Function<String, Relocation>  csrfReactiveRelocation = a -> Relocation.ofArtifactId(a, a.replace("csrf-reactive", "rest-csrf"),
                "3.9");
        RELOCATIONS.put("quarkus-csrf-reactive", csrfReactiveRelocation);
        RELOCATIONS.put("quarkus-csrf-reactive-deployment", csrfReactiveRelocation);

        Function<String, Relocation>  oidcTokenPropagationReactiveRelocation = a -> Relocation.ofArtifactId(a, a.replace("oidc-token-propagation-reactive", "rest-client-oidc-token-propagation"),
                "3.9");
        RELOCATIONS.put("quarkus-oidc-token-propagation-reactive", oidcTokenPropagationReactiveRelocation);
        RELOCATIONS.put("quarkus-oidc-token-propagation-reactive-deployment", oidcTokenPropagationReactiveRelocation);

        Function<String, Relocation>  oidcTokenPropagationRelocation = a -> Relocation.ofArtifactId(a, a.replace("oidc-token-propagation", "resteasy-client-oidc-token-propagation"),
                "3.9");
        RELOCATIONS.put("quarkus-oidc-token-propagation", oidcTokenPropagationRelocation);
        RELOCATIONS.put("quarkus-oidc-token-propagation-deployment", oidcTokenPropagationRelocation);

        Function<String, Relocation>  oidcClientFilterRelocation = a -> Relocation.ofArtifactId(a, a.replace("oidc-client-filter", "resteasy-client-oidc-filter"),
                "3.9");
        RELOCATIONS.put("quarkus-oidc-client-filter", oidcClientFilterRelocation);
        RELOCATIONS.put("quarkus-oidc-client-filter-deployment", oidcClientFilterRelocation);


        Function<String, Relocation>  oidcClientReactiveFilterRelocation = a -> Relocation.ofArtifactId(a, a.replace("oidc-client-reactive-filter", "rest-client-oidc-filter"),
                "3.9");
        RELOCATIONS.put("quarkus-oidc-client-reactive-filter", oidcClientReactiveFilterRelocation);
        RELOCATIONS.put("quarkus-oidc-client-reactive-filter-deployment", oidcClientReactiveFilterRelocation);

        Function<String, Relocation>  keycloakAdminClientRelocation = a -> Relocation.ofArtifactId(a, a.replace("keycloak-admin-client", "keycloak-admin-resteasy-client"),
                "3.9");
        RELOCATIONS.put("quarkus-keycloak-admin-client", keycloakAdminClientRelocation);
        RELOCATIONS.put("quarkus-keycloak-admin-client-deployment", keycloakAdminClientRelocation);

        Function<String, Relocation>  keycloakAdminClientReactiveRelocation = a -> Relocation.ofArtifactId(a, a.replace("keycloak-admin-client-reactive", "keycloak-admin-rest-client"),
                "3.9");
        RELOCATIONS.put("quarkus-keycloak-admin-client-reactive", keycloakAdminClientReactiveRelocation);
        RELOCATIONS.put("quarkus-keycloak-admin-client-reactive-deployment", keycloakAdminClientReactiveRelocation);

        Function<String, Relocation>  springWebRESTEasyReactiveRelocation = a -> Relocation.ofArtifactId(a, a.replace("spring-web-resteasy-reactive", "spring-web-rest"),
                "3.9");
        RELOCATIONS.put("quarkus-spring-web-resteasy-reactive", springWebRESTEasyReactiveRelocation);
        RELOCATIONS.put("quarkus-spring-web-resteasy-reactive-deployment", springWebRESTEasyReactiveRelocation);

        Function<String, Relocation>  springWebRESTEasyClassicRelocation = a -> Relocation.ofArtifactId(a, a.replace("spring-web-resteasy-classic", "spring-web-resteasy"),
                "3.9");
        RELOCATIONS.put("quarkus-spring-web-resteasy-classic", springWebRESTEasyClassicRelocation);
        RELOCATIONS.put("quarkus-spring-web-resteasy-classic-deployment", springWebRESTEasyClassicRelocation);

        Function<String, Relocation>  smallryeReactiveMessagingRelocation = a -> Relocation.ofArtifactId(a, a.replace("smallrye-reactive-messaging", "messaging"),
                "3.9");
        RELOCATIONS.put("quarkus-smallrye-reactive-messaging", smallryeReactiveMessagingRelocation);
        RELOCATIONS.put("quarkus-smallrye-reactive-messaging-kotlin", smallryeReactiveMessagingRelocation);
        RELOCATIONS.put("quarkus-smallrye-reactive-messaging-deployment", smallryeReactiveMessagingRelocation);
        RELOCATIONS.put("quarkus-smallrye-reactive-messaging-kafka", smallryeReactiveMessagingRelocation);
        RELOCATIONS.put("quarkus-smallrye-reactive-messaging-kafka-deployment", smallryeReactiveMessagingRelocation);
        RELOCATIONS.put("quarkus-smallrye-reactive-messaging-pulsar", smallryeReactiveMessagingRelocation);
        RELOCATIONS.put("quarkus-smallrye-reactive-messaging-pulsar-deployment", smallryeReactiveMessagingRelocation);
        RELOCATIONS.put("quarkus-smallrye-reactive-messaging-amqp", smallryeReactiveMessagingRelocation);
        RELOCATIONS.put("quarkus-smallrye-reactive-messaging-amqp-deployment", smallryeReactiveMessagingRelocation);
        RELOCATIONS.put("quarkus-smallrye-reactive-messaging-mqtt", smallryeReactiveMessagingRelocation);
        RELOCATIONS.put("quarkus-smallrye-reactive-messaging-mqtt-deployment", smallryeReactiveMessagingRelocation);
        RELOCATIONS.put("quarkus-smallrye-reactive-messaging-rabbitmq", smallryeReactiveMessagingRelocation);
        RELOCATIONS.put("quarkus-smallrye-reactive-messaging-rabbitmq-deployment", smallryeReactiveMessagingRelocation);

        Function<String, Relocation>  webjarsLocatorRelocation = a -> Relocation.ofArtifactId(a, a.replace("webjars-locator", "web-dependency-locator"),
                "3.11");
        RELOCATIONS.put("quarkus-webjars-locator", webjarsLocatorRelocation);
        RELOCATIONS.put("quarkus-webjars-locator-deployment", webjarsLocatorRelocation);
    }

    private static final String RELOCATION_POM_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" + //
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + //
            "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" + //
            "    <parent>\n" + //
            "        <artifactId>quarkus-relocations-parent</artifactId>\n" + //
            "        <groupId>io.quarkus</groupId>\n" + //
            "        <version>999-SNAPSHOT</version>\n" + //
            "    </parent>\n" + //
            "    <modelVersion>4.0.0</modelVersion>\n" + //
            "\n" + //
            "    <artifactId>%1$s</artifactId>\n" + //
            "\n" + //
            "    <distributionManagement>\n" + //
            "        <relocation>\n" + //
            "            <groupId>%2$s</groupId>\n" + //
            "            <artifactId>%3$s</artifactId>\n" + //
            "            <version>%4$s</version>\n" + //
            "            <message>Update the artifactId in your project build file. Refer to https://github.com/quarkusio/quarkus/wiki/Migration-Guide-%5$s for more information.</message>\n" + //
            "        </relocation>\n" + //
            "    </distributionManagement>\n" + //
            "</project>";

    @Override
    public void run() {
        List<String> modules = new ArrayList<>();
        Map<String, MigrationGuideAsciidoc> migrationGuides = new TreeMap<>();
        Map<String, List<Relocation>> relocations = new TreeMap<>();

        for (Entry<String, Function<String, Relocation>> relocationEntry : RELOCATIONS.entrySet()) {
            String originalArtifactId = relocationEntry.getKey();
            Relocation relocation = relocationEntry.getValue().apply(originalArtifactId);
            String newGroupId = relocation.getGroupId() != null ? relocation.getGroupId() : "io.quarkus";
            String newArtifactId = relocation.getArtifactId() != null ? relocation.getArtifactId() : originalArtifactId;
            String newVersion = relocation.getVersion() != null ? relocation.getVersion() : "${project.version}";
            String quarkusVersion = relocation.getQuarkusVersion();

            String newCoordinates = (relocation.getGroupId() != null ? relocation.getGroupId() + ":" : "") +
                    (relocation.getArtifactId() != null ? relocation.getArtifactId() : originalArtifactId) +
                    (relocation.getVersion() != null ? ":" + relocation.getVersion() : "");

            Path relocationDirectory = Path.of(originalArtifactId);
            Path relocationPom = relocationDirectory.resolve("pom.xml");
            try {
                if (!Files.exists(relocationDirectory)) {
                    Files.createDirectory(relocationDirectory);
                }

                Files.writeString(relocationPom, String.format(RELOCATION_POM_TEMPLATE,
                        originalArtifactId,
                        newGroupId,
                        newArtifactId,
                        newVersion,
                        quarkusVersion
                ));

                migrationGuides.computeIfAbsent(quarkusVersion, qv -> new MigrationGuideAsciidoc(qv))
                        .addModule(originalArtifactId, newCoordinates);

                relocations.computeIfAbsent(quarkusVersion, qv -> new ArrayList<>())
                        .add(relocation);

                modules.add(originalArtifactId);
            } catch (IOException e) {
                LOG.error("Error writing relocation for " + originalArtifactId, e);
            }
        }

        if (modules.isEmpty()) {
            return;
        }

        for (MigrationGuideAsciidoc migrationGuideAsciidoc : migrationGuides.values()) {
            migrationGuideAsciidoc.dump();
        }

        for (Entry<String, List<Relocation>> relocationsEntry : relocations.entrySet()) {
            StringBuilder recipes = new StringBuilder();
            recipes.append("---\n" +
                    "type: specs.openrewrite.org/v1beta/recipe\n" +
                    "name: io.quarkus.updates.core.quarkus" + relocationsEntry.getKey().replace(".", "") + ".Relocations\n" +
                    "recipeList:\n");
            for (Relocation relocation : relocationsEntry.getValue()) {
                recipes.append("  - org.openrewrite.java.dependencies.ChangeDependency:\n" +
                         "      oldGroupId: io.quarkus\n" +
                         "      oldArtifactId: " + relocation.getOriginalArtifactId() + "\n");
                if (relocation.getGroupId() != null && !"io.quarkus".equals(relocation.getGroupId())) {
                    recipes.append("      newGroupId: " + relocation.getGroupId() +"\n");
                }
                recipes.append("      newArtifactId: " + relocation.getArtifactId() + "\n");
                if (relocation.getVersion() != null) {
                    recipes.append("      newVersion: " + relocation.getVersion() +"\n");
                }
            }

            LOG.info("Recipes to include for " + relocationsEntry.getKey() + "\n\n" + recipes + "\n\n");
        }

        try {
            Path parentPom = Path.of("pom.xml");
            String parentPomContent = Files.readString(parentPom);
            String modulesToInsert = "";
            for (String module : modules) {
                String moduleNode = "<module>" + module + "</module>";
                if (parentPomContent.contains(moduleNode)) {
                    continue;
                }

                modulesToInsert += "        " + moduleNode + "\n";
            }

            if (!modulesToInsert.isBlank()) {
                Files.writeString(parentPom, parentPomContent.replace("    </modules>", modulesToInsert + "    </modules>"));
            }
        } catch (IOException e) {
            LOG.error("Unable to insert modules in the parent", e);
        }
    }

    private static class Relocation {

        private final String originalArtifactId;
        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String quarkusVersion;

        private Relocation(String originalArtifactId, String groupId, String artifactId, String version, String quarkusVersion) {
            this.originalArtifactId = originalArtifactId;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.quarkusVersion = quarkusVersion;
        }

        public static Relocation ofArtifactId(String originalArtifactId, String artifactId, String quarkusVersion) {
            return new Relocation(originalArtifactId, null, artifactId, null, quarkusVersion);
        }

        public static Relocation ofGroupId(String originalArtifactId, String groupId, String quarkusVersion) {
            return new Relocation(originalArtifactId, groupId, null, null, quarkusVersion);
        }

        public static Relocation of(String originalArtifactId, String groupId, String artifactId, String quarkusVersion) {
            return new Relocation(originalArtifactId, groupId, artifactId, null, quarkusVersion);
        }

        public static Relocation of(String originalArtifactId, String groupId, String artifactId, String version, String quarkusVersion) {
            return new Relocation(originalArtifactId, groupId, artifactId, version, quarkusVersion);
        }

        public String getOriginalArtifactId() {
            return originalArtifactId;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getVersion() {
            return version;
        }

        public String getQuarkusVersion() {
            return quarkusVersion;
        }
    }

    private static class MigrationGuideAsciidoc {

        private String quarkusVersion;
        private String publicAsciidoc = "";
        private String extensionAsciidoc = "";

        public MigrationGuideAsciidoc(String quarkusVersion) {
            this.quarkusVersion = quarkusVersion;
        }

        public void addModule(String originalArtifactId, String newCoordinates) {
            if (originalArtifactId.endsWith("-deployment")
                    || originalArtifactId.endsWith("-spi")
                    || originalArtifactId.contains("-spi-")
                    || originalArtifactId.endsWith("-common")) {
                extensionAsciidoc += "\n|" + originalArtifactId + "\n" +
                        "|" + newCoordinates + "\n";
            } else {
                publicAsciidoc += "\n|" + originalArtifactId + "\n" +
                        "|" + newCoordinates + "\n";
            }
        }

        public void dump() {
            LOG.info("Asciidoc table(s) to include in the migration guide for " + quarkusVersion + " - https://github.com/quarkusio/quarkus/wiki/Migration-Guide-" + quarkusVersion);

            if (!publicAsciidoc.isBlank()) {
                publicAsciidoc = "|===\n" +
                        "|Old name |New name\n" + publicAsciidoc + "|===";
                LOG.info("- Asciidoc table for publicly consumed modules:\n" + publicAsciidoc + "\n\n");
            }
            if (!extensionAsciidoc.isBlank()) {
                extensionAsciidoc = "|===\n" +
                        "|Old name |New name\n" + extensionAsciidoc + "|===";
                LOG.info("- Asciidoc table for extension developers:\n" + extensionAsciidoc + "\n\n");
            }
        }
    }
}
