//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS org.jboss.forge.roaster:roaster-jdt:2.22.2.Final
//DEPS org.eclipse.collections:eclipse-collections:10.4.0
//DEPS org.yaml:snakeyaml:1.27
//DEPS io.fabric8:maven-model-helper:16

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import io.fabric8.maven.Maven;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaDocCapable;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.yaml.snakeyaml.Yaml;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "builditemdoc", mixinStandardHelpOptions = true, version = "builditemdoc 0.1",
        description = "builditemdoc made with jbang")
class quarkusbuilditemdoc implements Callable<Integer> {

    @CommandLine.Option(names = {"--outputFile"})
    public Path outputFile;

    @CommandLine.Option(names = {"--dirs"}, required = true, arity = "1..*")
    public List<Path> paths;

    private PrintStream out = System.out;

    public static void main(String... args) {
        int exitCode = new CommandLine(new quarkusbuilditemdoc()).execute(args);
        System.exit(exitCode);
    }

    // jbang .github/quarkusbuilditemdoc.java --outputFile target/asciidoc/generated/config/quarkus-all-build-items.adoc --dirs core/deployment core/test-extension extensions
    @Override
    public Integer call() throws Exception {
        if (outputFile != null) {
            Files.createDirectories(outputFile.getParent());
            out = new PrintStream(Files.newOutputStream(outputFile));
        }
        final Multimap<String, Pair<Path, JavaClassSource>> multimap = collect();
        Map<String, String> names = extractNames(Paths.get("."), multimap.keySet());
        // Print Core first
        {
            printTableHeader(names.remove("Core"));
            for (Pair<Path, JavaClassSource> source : multimap.get("Core")) {
                printTableRow(source);
            }
            printTableFooter();
        }
        names.forEach((key, name) -> {
            printTableHeader(name);
            for (Pair<Path, JavaClassSource> source : multimap.get(key)) {
                printTableRow(source);
            }
            printTableFooter();
        });
        return 0;
    }

    private String getJavaDoc(JavaDocCapable<?> source) {
        if (!source.hasJavaDoc()) {
            return "<i>No Javadoc found</i>";
        }
        return source.getJavaDoc().getText();
    }

    private Multimap<String, Pair<Path, JavaClassSource>> collect() throws IOException {
        MutableMultimap<String, Pair<Path, JavaClassSource>> multimap = Multimaps.mutable.sortedSet
                .with(Comparator.comparing(o -> o.getTwo().getName()));
        String line;
        for (Path path : paths) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith("BuildItem.java")) {
                        process(multimap, file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return multimap;
    }

    private void process(MutableMultimap<String, Pair<Path, JavaClassSource>> multimap, Path path) throws IOException {
        JavaClassSource source = Roaster.parse(JavaClassSource.class, path.toFile());
        // Ignore deprecated annotations and non-public classes
        if (!source.hasAnnotation(Deprecated.class) && source.isPublic()) {
            String name;
            Path pom = findPom(path);
            if (pom != null) {
                name = Maven.readModel(pom).getName();
            } else {
                String pathString = path.toString();
                int spiIdx = pathString.indexOf("/spi/src");
                int runtimeIdx = pathString.indexOf("/runtime/src");
                int deploymentIdx = pathString.indexOf("/deployment/src");
                int idx = Math.max(Math.max(spiIdx, runtimeIdx), deploymentIdx);
                int extensionsIdx = pathString.indexOf("extensions/");
                int startIdx = 0;
                if (extensionsIdx != -1) {
                    startIdx = extensionsIdx + 11;
                }
                if (idx == -1) {
                    name = pathString.substring(startIdx, pathString.indexOf("/", startIdx + 1));
                } else {
                    name = pathString.substring(startIdx, idx);
                }
            }
            // sanitize name
            name = name.replace("Quarkus - ", "")
                    .replace(" - Deployment", "");
            Pair<Path, JavaClassSource> pair = Tuples.pair(path, source);
            multimap.put(name, pair);
        }
    }

    private Path findPom(Path path) {
        Path pom = null;
        Path parent = path;
        while (pom == null && (parent = parent.getParent()) != null) {
            Path resolve = parent.resolve("pom.xml");
            if (Files.exists(resolve)) {
                pom = resolve;
            }
        }
        return pom;
    }


    private Map<String, String> extractNames(Path root, Iterable<String> extensionDirs) throws IOException {
        Map<String, String> names = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Yaml yaml = new Yaml();
        for (String extension : extensionDirs) {
            Path yamlPath = root.resolve("extensions/" + extension + "/runtime/src/main/resources/META-INF/quarkus-extension.yaml");
            if (Files.exists(yamlPath)) {
                try (InputStream is = Files.newInputStream(yamlPath)) {
                    Map<String, String> map = yaml.load(is);
                    names.put(extension, map.get("name"));
                }
            } else {
                names.put(extension, extension);
            }
        }
        return names;
    }

    private void printTableHeader(String title) {
        out.println("== " + title);
        out.println("[%header,cols=2*]");
        out.println("|===");
        out.println("|Class Name |Attributes ");
    }

    private void printTableRow(Pair<Path, JavaClassSource> pair) {
        //TODO: Use tagged version?
        Path root = Paths.get(".").toAbsolutePath().normalize();
        String link = "https://github.com/quarkusio/quarkus/blob/main/" + root.relativize(pair.getOne().normalize());
        JavaClassSource source = pair.getTwo();
        String className = source.getQualifiedName();
        String attributes = buildAttributes(source);
        String description = getJavaDoc(source);

        out.println("a| " + link + "[`" + className + "`, window=\"_blank\"] :: +++" +
                            javadocToHTML(description) + "+++");
        out.println("a| " + attributes);
    }

    private String buildAttributes(JavaClassSource source) {
        StringBuilder sb = new StringBuilder();
        for (FieldSource<JavaClassSource> field : source.getFields()) {
            if (field.isStatic()) {
                continue;
            }
            sb.append("`" + field.getType().getName() + " " + field.getName() + "` :: ");
            sb.append("+++" + javadocToHTML(getJavaDoc(field)) + "+++");
            sb.append("\n");
        }
        return sb.length() == 0 ? "None" : sb.toString();
    }

    private void printTableFooter() {
        out.println("|===");
    }

    private String javadocToHTML(String content) {
        return content
                .replaceAll("\\{?@see ", "<pre>")
                .replaceAll("\\{?@code ", "<pre>")
                .replaceAll("\\{?@link ", "<pre>")
                .replaceAll(" ?}", "</pre>");
    }

    private String javadocToAsciidoc(String content) {
        return content
                .replaceAll("<p>", "\n")
                .replaceAll("</p>", "\n")
                .replaceAll("\\{?@see ", "```")
                .replaceAll("\\{?@code ", "```")
                .replaceAll("\\{?@link ", "```")
                .replaceAll("<pre>", "```\n")
                .replaceAll("</pre>", "\n```")
                .replaceAll(" ?}", "```");
    }


}
