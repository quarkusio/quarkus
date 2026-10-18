package io.quarkus.docs.generation;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.JavadocComment;

import io.fabric8.maven.Maven;

public class QuarkusBuildItemDoc {

    static {
        StaticJavaParser.getParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    }

    public Path outputFile;
    public List<Path> paths;
    public String gitRef = "main";

    private PrintStream out = System.out;

    // target/asciidoc/generated/config/quarkus-all-build-items.adoc core/deployment core/test-extension extensions [version]
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Must specify output file (first) followed by at least one source directory");
            System.exit(1);
        }
        QuarkusBuildItemDoc buildItemDoc = new QuarkusBuildItemDoc();

        buildItemDoc.outputFile = Path.of(args[0]);

        int sourceArgCount = args.length;
        String lastArg = args[args.length - 1];
        if (!Files.isDirectory(Path.of(lastArg))) {
            sourceArgCount--;
            if (!lastArg.endsWith("-SNAPSHOT")) {
                buildItemDoc.gitRef = lastArg;
            }
        }
        buildItemDoc.paths = Arrays.stream(args, 1, sourceArgCount)
                .map(Path::of)
                .collect(Collectors.toList());

        try {
            buildItemDoc.run();
        } catch (Exception e) {
            System.err.println("Exception occurred while trying to collect build item documentation");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void run() throws Exception {
        if (outputFile != null) {
            Files.createDirectories(outputFile.getParent());
            out = new PrintStream(Files.newOutputStream(outputFile));
        }
        final Multimap<String, Pair<Path, ClassOrInterfaceDeclaration>> multimap = collect();
        Map<String, String> names = extractNames(Paths.get("."), multimap.keySet());
        // Print Core first
        {
            printTableHeader(names.remove("Core"));
            for (Pair<Path, ClassOrInterfaceDeclaration> source : multimap.get("Core")) {
                printTableRow(source);
            }
            printTableFooter();
        }
        names.forEach((key, name) -> {
            printTableHeader(name);
            for (Pair<Path, ClassOrInterfaceDeclaration> source : multimap.get(key)) {
                printTableRow(source);
            }
            printTableFooter();
        });
    }

    private String getJavaDoc(Optional<JavadocComment> javadocComment) {
        if (javadocComment.isEmpty()) {
            return "<i>No Javadoc found</i>";
        }
        return cleanJavadocComment(javadocComment.get().getContent());
    }

    static String cleanJavadocComment(String rawContent) {
        return rawContent.lines()
                .map(line -> line.replaceFirst("^\\s*\\*\\s?", ""))
                .collect(Collectors.joining("\n"))
                .strip();
    }

    private Multimap<String, Pair<Path, ClassOrInterfaceDeclaration>> collect() throws IOException {
        MutableMultimap<String, Pair<Path, ClassOrInterfaceDeclaration>> multimap = Multimaps.mutable.sortedSet
                .with(Comparator.comparing(o -> o.getTwo().getNameAsString()));
        for (Path path : paths) {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
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

    private void process(MutableMultimap<String, Pair<Path, ClassOrInterfaceDeclaration>> multimap, Path path)
            throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(path);
        Optional<ClassOrInterfaceDeclaration> primaryType = cu.findFirst(ClassOrInterfaceDeclaration.class);
        if (primaryType.isEmpty()) {
            return;
        }
        ClassOrInterfaceDeclaration classDecl = primaryType.get();
        // Ignore deprecated annotations and non-public classes
        if (classDecl.getAnnotationByClass(Deprecated.class).isPresent() || !classDecl.isPublic()) {
            return;
        }
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
        Pair<Path, ClassOrInterfaceDeclaration> pair = Tuples.pair(path, classDecl);
        multimap.put(name, pair);
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
        Load yaml = new Load(LoadSettings.builder().build());
        for (String extension : extensionDirs) {
            Path yamlPath = root
                    .resolve("extensions/" + extension + "/runtime/src/main/resources/META-INF/quarkus-extension.yaml");
            if (Files.exists(yamlPath)) {
                try (InputStream is = Files.newInputStream(yamlPath)) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> map = (Map<String, String>) yaml.loadFromInputStream(is);
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
        out.println("[.configuration-reference,cols=2*]");
        out.println("|===");
        out.println("h|Class Name\nh|Attributes \n\n");
    }

    private void printTableRow(Pair<Path, ClassOrInterfaceDeclaration> pair) {
        Path root = Paths.get(".").toAbsolutePath().normalize();
        String link = "https://github.com/quarkusio/quarkus/blob/" + gitRef + "/" + root.relativize(pair.getOne().normalize());
        ClassOrInterfaceDeclaration classDecl = pair.getTwo();
        String className = classDecl.getFullyQualifiedName().orElse(classDecl.getNameAsString());
        String attributes = buildAttributes(classDecl);
        String description = getJavaDoc(classDecl.getJavadocComment());
        String baseBuildItemText = classDecl.isAbstract()
                ? "icon:building[title=Non-instantiatable Build Item (can be inherited from)]"
                : "";

        String linkToClass = String.format("%s[`%s`, window=\"_blank\"]", link, className);

        out.println(String.format("\n\na|%s %s\n[.description]\n--\n%s\n-- a|%s",
                baseBuildItemText,
                linkToClass,
                javadocToAsciidoc(description),
                attributes));
    }

    private String buildAttributes(ClassOrInterfaceDeclaration classDecl) {
        StringBuilder sb = new StringBuilder();
        for (FieldDeclaration field : classDecl.getFields()) {
            if (field.isStatic()) {
                continue;
            }
            for (VariableDeclarator variable : field.getVariables()) {
                String fieldJavadoc = getJavaDoc(field.getJavadocComment());
                sb.append(String.format("`%s %s` \n\n%s\n\n",
                        variable.getType().asString(),
                        variable.getNameAsString(),
                        javadocToAsciidoc(fieldJavadoc)));
            }
        }
        return sb.length() == 0 ? "None" : sb.toString();
    }

    private void printTableFooter() {
        out.println("|===");
    }

    private static final Pattern ANCHOR_PATTERN = Pattern.compile(
            "(?s)<a\\s+href=\\s*\"([^\"]*?)\"\\s*>(.*?)</a>");

    String javadocToAsciidoc(String content) {
        String result = content
                .replaceAll("<p> *", "\n")
                .replaceAll("</p> *", "\n")
                .replaceAll("<br> *", "\n")
                .replaceAll("\\{?@(link|see|code) ([^}]*)}", "`$2`")
                .replaceAll("(?m)^@see ", "See ")
                .replaceAll("<pre>", "\n[source]\n----\n")
                .replaceAll("</pre>", "\n----\n")
                .replaceAll("<h2>", "\n[discrete]\n== ")
                .replaceAll("</h2> *", "\n\n")
                .replaceAll("</?i>", "_")
                .replaceAll("</?ul> *", "\n")
                .replaceAll("<li>", "\n* ")
                .replaceAll("</li> *", "\n\n")
                .replaceAll("</?tt>", "`");
        return convertAnchors(result);
    }

    private static String convertAnchors(String content) {
        Matcher matcher = ANCHOR_PATTERN.matcher(content);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String url = matcher.group(1).strip();
            String text = matcher.group(2).replaceAll("\\s+", " ").strip();
            matcher.appendReplacement(sb, Matcher.quoteReplacement(url + "[" + text + ",window=_blank]"));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
