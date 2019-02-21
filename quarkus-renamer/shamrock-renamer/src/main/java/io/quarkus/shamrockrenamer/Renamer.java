package io.quarkus.shamrockrenamer;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Renamer {

    static final Map<String, String> REPLACEMENTS = new LinkedHashMap<>();
    private static final String ROOT_PACKAGE = "io";
    private static final String NAME = "quarkus";
    private static final String CAP_NAME = "Quarkus";


    static {
        REPLACEMENTS.put("org_jboss_shamrock", ROOT_PACKAGE + "_" + NAME);
        REPLACEMENTS.put("org.jboss.shamrock", ROOT_PACKAGE + "." + NAME);
        REPLACEMENTS.put("protean-shamrock", NAME);
        REPLACEMENTS.put("shamrock", NAME);
        REPLACEMENTS.put("Shamrock", CAP_NAME);
        REPLACEMENTS.put("protean", NAME);
        REPLACEMENTS.put("Protean", CAP_NAME);
        REPLACEMENTS.put("org.jboss.quarkus.gizmo", "org.jboss.protean.gizmo");
    }

    static final Set<String> TEXT_FILES = new HashSet<>(Arrays.asList("java", "xml", "md", "txt", "adoc", "yml", "json", "ftl", "groovy", "properties", "html", "MD"));

    /**
     * This is the renamer. It is a weird mix of java and shell, based on what seems to be the least work
     *
     * @param args
     * @throws Exception
     */
    public static void main(String... args) throws Exception {
        String path = args[0];
        Path root = Paths.get(path);
        Map<String, String> renames = new LinkedHashMap<>();
        run(root, "git", "clean", "-dfx");
        fixMetaInfoServices(root, renames);

        Files.walkFileTree(root, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir.getFileName().toString().startsWith(".") ||
                        dir.getFileName().toString().equals("quarkus-renamer")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String name = file.getFileName().toString();
                int idx = name.lastIndexOf('.');
                if(name.equals("Dockerfile") || name.equals("WORKSPACE")) {
                    replaceContents(file);
                    return FileVisitResult.CONTINUE;
                }
                if (idx == -1) {
                    return FileVisitResult.CONTINUE;
                }
                String ext = name.substring(idx + 1);
                if (TEXT_FILES.contains(ext)) {
                    replaceContents(file);
                }
                renameIfRequired(root, file, renames);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });

        run(root, "git", "commit", "-a", "-m", "Rename Shamrock to " + CAP_NAME + ", change file contents");
        for(Map.Entry<String, String> e : renames.entrySet()) {
            run(root, "git", "mv", e.getKey(), e.getValue());
        }
        fixPackages(root);
        renameDirectories(root);
        run(root, "git", "commit", "-a", "-m", "Rename Shamrock to " + CAP_NAME + ", rename files");
    }


    private static void fixMetaInfoServices(Path root, Map<String, String> renames) throws Exception {
        List<Path> serviceFiles = Files.walk(root).filter((f) -> {
            if (!Files.isDirectory(f)) {
                if (f.getParent().getParent().getFileName().toString().equals("META-INF") &&
                        f.getParent().getFileName().toString().equals("services")) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
        for (Path i : serviceFiles) {
            replaceContents(i);
            renameIfRequired(root, i, renames);
        }
    }

    private static void renameIfRequired(Path root, Path i, Map<String, String> renames) throws IOException {
        String oldFileName = i.getFileName().toString();
        String newName = runStringReplace(oldFileName);
        if (!newName.equals(oldFileName)) {
            renames.put(i.toAbsolutePath().toString(), i.getParent().resolve(newName).toAbsolutePath().toString());
        }
    }

    private static void run(Path root, String... args) {
        try {
            //System.out.println(Arrays.asList(args));
            int result = new ProcessBuilder(args)
                    .directory(root.toFile())
                    .inheritIO()
                    .start()
                    .waitFor();
            if(result != 0) {
                System.out.println(result);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String runStringReplace(String contents) {
        String ret = contents;
        for (Map.Entry<String, String> i : REPLACEMENTS.entrySet()) {
            ret = ret.replaceAll(i.getKey(), i.getValue());
        }
        return ret;
    }

    static void replaceContents(Path path) throws IOException {
        String contents = readFile(path);
        byte[] newContents = runStringReplace(contents).getBytes(StandardCharsets.UTF_8);
        try (FileOutputStream out = new FileOutputStream(path.toFile())) {
            out.write(newContents);
        }
    }

    static String readFile(Path path) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8000];
        int r;
        try (FileInputStream in = new FileInputStream(path.toFile())) {
            while ((r = in.read(buf)) > 0) {
                out.write(buf, 0, r);
            }
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void fixPackages(Path root) throws Exception {
        List<Path> dirs = Files.walk(root).filter((f) -> {
            if (Files.isDirectory(f)) {
                if ((f.getFileName().toString().equals("shamrock") ||
                        f.getFileName().toString().equals("protean")) &&
                        f.getParent().getFileName().toString().equals("jboss")) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
        for (Path i : dirs) {
            Path target = i.getParent().getParent().getParent().resolve(ROOT_PACKAGE).resolve(NAME);
            Files.createDirectories(target.getParent());
            run(root, "git", "mv",i.toAbsolutePath().toString(), target.toAbsolutePath().toString());
        }
    }

    private static void renameDirectories(Path root) throws Exception {
        List<Path> dirs = Files.walk(root).filter((f) -> {
            if (f.toAbsolutePath().toString().equals(root.toAbsolutePath().toString())) {
                return false;
            }
            if (Files.isDirectory(f)) {
                if (f.getFileName().toString().contains("shamrock") ||
                        f.getFileName().toString().contains("protean")) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
        Collections.reverse(dirs);
        for (Path i : dirs) {
            String newName = i.getFileName().toString().replaceAll("shamrock", NAME).replaceAll("protean", NAME);
            Path target = i.getParent().resolve(newName);
            run(root, "git", "mv",i.toAbsolutePath().toString(), target.toAbsolutePath().toString());
        }
    }
}
