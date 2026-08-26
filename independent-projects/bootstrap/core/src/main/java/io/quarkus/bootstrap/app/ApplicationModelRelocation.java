package io.quarkus.bootstrap.app;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import io.quarkus.bootstrap.util.PropertyUtils;

/**
 * Rewrites the absolute filesystem paths in a serialized {@link io.quarkus.bootstrap.model.ApplicationModel}
 * so that the same model, produced from two different checkouts or with two different local repositories,
 * serializes to the same bytes.
 * <p>
 * A serialized model embeds absolute paths in a handful of places: the module directories of the project
 * itself, its build files and source/output directories, and the resolved location of every dependency
 * inside the local Maven repository or the Gradle dependency cache. Whoever fingerprints that file by
 * content - Gradle's build cache being the motivating case - ends up with a key that depends on where the
 * project happens to sit on disk.
 * <p>
 * On write, each path that lies under one of the {@link Root roots} is replaced by a token plus the
 * remainder relative to that root:
 *
 * <pre>
 * /home/ci/slot-3/app/build/classes  -&gt;  ${quarkus.project.dir}/build/classes
 * /home/ci/.m2/repository/foo.jar    -&gt;  ${quarkus.local.repo}/foo.jar
 * </pre>
 *
 * The roots that were used are recorded in the serialized model under {@value #RELOCATION_ROOTS}. On read,
 * each token is substituted back with the corresponding root <em>as it is in the reading environment</em>,
 * falling back to the recorded value when the reader does not know that root. A model is therefore usable
 * both in the environment that produced it and in one where the project or the repository has moved.
 * <p>
 * Both directions are no-ops when no roots apply, and a model written without the {@value #RELOCATION_ROOTS}
 * entry - by an older version, or with relocation disabled - is read back unchanged, so the format stays
 * compatible in both directions.
 */
public class ApplicationModelRelocation {

    /**
     * Key under which the roots used to relocate a model are recorded in the serialized model.
     */
    public static final String RELOCATION_ROOTS = "relocation-roots";

    /**
     * System property to turn relocation off, in case a consumer depends on the absolute paths being
     * written literally.
     */
    public static final String RELOCATION_DISABLED_PROP = "quarkus.bootstrap.application-model.relocation.disabled";

    private static final String TOKEN_PREFIX = "${";
    private static final String TOKEN_SUFFIX = "}";

    private ApplicationModelRelocation() {
    }

    /**
     * A directory that paths in a model are expressed relative to.
     *
     * @param name name of the root, used to build the token that replaces it
     * @param path location of the root
     */
    public record Root(String name, Path path) {

        String token() {
            return TOKEN_PREFIX + name + TOKEN_SUFFIX;
        }
    }

    /**
     * Whether relocation is enabled. It is unless {@value #RELOCATION_DISABLED_PROP} is set to {@code true}.
     */
    public static boolean isEnabled() {
        return !Boolean.getBoolean(RELOCATION_DISABLED_PROP);
    }

    /**
     * Replaces the absolute paths in a model's map representation with tokens, and records the names of
     * the roots that were used so that {@link #absolutize(Map, Collection)} knows the model is relocated.
     *
     * @param model map representation of an application model
     * @param roots roots to express paths relative to
     * @return a copy of the model with relocated paths, or the model itself if there is nothing to relocate
     */
    public static Map<String, Object> relocate(Map<String, Object> model, Collection<Root> roots) {
        if (model == null || roots.isEmpty() || !isEnabled()) {
            return model;
        }
        // a longer root first, so that a root nested inside another one wins
        final List<Root> ordered = new ArrayList<>(roots);
        ordered.sort((a, b) -> Integer.compare(b.path().toString().length(), a.path().toString().length()));

        final Map<String, Object> relocated = new LinkedHashMap<>(mapValue(model, s -> tokenize(s, ordered)));
        // only the names of the roots are recorded, never their locations: a recorded location would be
        // exactly the checkout-dependent content this whole exercise removes from the file. The reader
        // resolves each name against its own environment.
        final List<Object> recordedRoots = new ArrayList<>(ordered.size());
        for (Root root : ordered) {
            recordedRoots.add(root.name());
        }
        relocated.put(RELOCATION_ROOTS, recordedRoots);

        // the root of the build is the one root a reader cannot derive, so record how far above the
        // project directory it sits; that offset is the same wherever the project is checked out
        final Path projectDir = pathOf(ordered, PROJECT_DIR_ROOT);
        final Path rootDir = pathOf(ordered, ROOT_DIR_ROOT);
        if (projectDir != null && rootDir != null) {
            final int depth = depthBetween(rootDir, projectDir);
            if (depth >= 0) {
                relocated.put(ROOT_DIR_DEPTH, String.valueOf(depth));
            }
        }
        return relocated;
    }

    /**
     * Resolves the tokens in a model's map representation back to absolute paths.
     * <p>
     * Roots are resolved entirely from the reading environment, so a model remains usable after the
     * project or the local repository has moved. A token whose root the reader does not know is left as
     * it is, rather than resolved to a location that would be wrong here.
     *
     * @param model map representation of an application model
     * @param roots roots as they are in the reading environment
     * @return a copy of the model with absolute paths, or the model itself if it was not relocated
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> absolutize(Map<String, Object> model, Collection<Root> roots) {
        if (model == null) {
            return null;
        }
        if (!(model.get(RELOCATION_ROOTS) instanceof Collection)) {
            // not a relocated model, nothing to resolve
            return model;
        }

        final Map<String, String> resolved = new LinkedHashMap<>();
        for (Root root : roots) {
            resolved.put(root.name(), root.path().toString());
        }
        // a reader that recovered the project directory but was not told the root of the build can
        // rebuild it from the recorded offset, rather than leaving sibling modules unresolved
        if (!resolved.containsKey(ROOT_DIR_ROOT)) {
            final String projectDir = resolved.get(PROJECT_DIR_ROOT);
            final Object depth = model.get(ROOT_DIR_DEPTH);
            if (projectDir != null && depth != null) {
                final Path rootDir = parentOf(Path.of(projectDir), depth.toString());
                if (rootDir != null) {
                    resolved.put(ROOT_DIR_ROOT, rootDir.toString());
                }
            }
        }

        final Map<String, Object> absolute = new LinkedHashMap<>(mapValue(model, s -> resolve(s, resolved)));
        absolute.remove(RELOCATION_ROOTS);
        absolute.remove(ROOT_DIR_DEPTH);
        return absolute;
    }

    /**
     * Name of the root standing for the local Maven repository, where Maven and the CLI resolve
     * dependencies to.
     */
    public static final String LOCAL_REPO_ROOT = "quarkus.local.repo";

    /**
     * Name of the root standing for the Gradle home, whose dependency cache Gradle resolves to.
     */
    public static final String GRADLE_USER_HOME_ROOT = "quarkus.gradle.user.home";

    /**
     * The roots that can be derived from the environment alone, and are therefore available to any
     * reader of a model without having to be told about them: the local Maven repository and the Gradle
     * home. Both honour the standard overrides and fall back to their conventional locations under the
     * user home.
     * <p>
     * Callers that know more - a build tool knows where the project is - are expected to add their own
     * roots to these.
     */
    public static List<Root> environmentRoots() {
        final List<Root> roots = new ArrayList<>(2);
        addRoot(roots, LOCAL_REPO_ROOT, System.getProperty("maven.repo.local"),
                () -> Path.of(PropertyUtils.getUserHome(), ".m2", "repository"));
        addRoot(roots, GRADLE_USER_HOME_ROOT, System.getenv("GRADLE_USER_HOME"),
                () -> Path.of(PropertyUtils.getUserHome(), ".gradle"));
        return roots;
    }

    /**
     * Name of the root standing for the build/output directory of the module a model belongs to.
     */
    public static final String BUILD_DIR_ROOT = "quarkus.build.dir";

    /**
     * Name of the root standing for the project directory of the module a model belongs to.
     */
    public static final String PROJECT_DIR_ROOT = "quarkus.project.dir";

    /**
     * Name of the root standing for the root directory of a multi-module build.
     * <p>
     * Unlike the other roots, this one cannot be derived by a reader on its own, so it is recorded as
     * the number of directory levels between the project directory and the root of the build - a
     * relative offset that survives relocation - rather than being guessed or written out absolutely.
     *
     * @see #ROOT_DIR_DEPTH
     */
    public static final String ROOT_DIR_ROOT = "quarkus.root.dir";

    /**
     * Key under which the distance from the project directory up to the root of the build is recorded,
     * so that {@link #ROOT_DIR_ROOT} can be resolved by a reader that only recovered the project
     * directory from the model's location.
     */
    public static final String ROOT_DIR_DEPTH = "relocation-root-dir-depth";

    /**
     * The roots a serialized model can be resolved against knowing nothing but where the file is: the
     * environment roots, plus the project and build directories recovered from the model's own location.
     * <p>
     * A build tool writes the model into a fixed place inside the build directory - {@code
     * <project>/build/quarkus/application-model/<name>.dat} for Gradle, {@code <project>/target/...} for
     * Maven - so a reader handed only the path, such as a forked test or dev JVM, can still resolve the
     * project-relative tokens. A caller that knows the actual directories should pass them instead.
     *
     * @param modelFile location of the serialized model
     * @return roots to resolve the model's relocation tokens against
     */
    public static List<Root> rootsForModelAt(Path modelFile) {
        final List<Root> roots = new ArrayList<>(environmentRoots());
        final Path modelDir = modelFile == null ? null : modelFile.toAbsolutePath().getParent();
        // <build-dir>/quarkus/application-model/<name>.dat
        final Path quarkusDir = modelDir == null ? null : modelDir.getParent();
        final Path buildDir = quarkusDir == null ? null : quarkusDir.getParent();
        if (buildDir == null) {
            return roots;
        }
        roots.add(new Root(BUILD_DIR_ROOT, buildDir));
        final Path projectDir = buildDir.getParent();
        if (projectDir != null) {
            roots.add(new Root(PROJECT_DIR_ROOT, projectDir));
        }
        // deliberately no ROOT_DIR_ROOT: the root of the build cannot be recovered from the model's
        // location, and guessing the project directory would silently resolve a sibling module's jar
        // to a path nested under this module. A caller that knows the root passes it explicitly.
        return roots;
    }

    private static void addRoot(List<Root> roots, String name, String configured, Supplier<Path> fallback) {
        Path path;
        try {
            path = configured == null || configured.isBlank() ? fallback.get() : Path.of(configured);
            path = path.toAbsolutePath().normalize();
        } catch (RuntimeException e) {
            // an unusable root is simply one fewer path that can be relocated
            return;
        }
        roots.add(new Root(name, path));
    }

    private static Path pathOf(List<Root> roots, String name) {
        for (Root root : roots) {
            if (root.name().equals(name)) {
                return root.path();
            }
        }
        return null;
    }

    /**
     * The number of directory levels from {@code child} up to {@code ancestor}, or -1 if {@code child}
     * is not under {@code ancestor}.
     */
    private static int depthBetween(Path ancestor, Path child) {
        if (!child.startsWith(ancestor)) {
            return -1;
        }
        return ancestor.relativize(child).getNameCount();
    }

    /**
     * Walks {@code levels} directories up from {@code path}, or null if that is not possible.
     */
    private static Path parentOf(Path path, String levels) {
        final int count;
        try {
            count = Integer.parseInt(levels);
        } catch (NumberFormatException e) {
            return null;
        }
        Path result = path;
        for (int i = 0; i < count && result != null; i++) {
            result = result.getParent();
        }
        return result;
    }

    private static String tokenize(String value, List<Root> roots) {
        for (Root root : roots) {
            final String prefix = root.path().toString();
            if (isUnder(value, prefix)) {
                // separators are normalised so that a model is rendered identically across operating systems
                return root.token() + value.substring(prefix.length()).replace('\\', '/');
            }
        }
        return value;
    }

    private static String resolve(String value, Map<String, String> roots) {
        if (!value.startsWith(TOKEN_PREFIX)) {
            return value;
        }
        final int end = value.indexOf(TOKEN_SUFFIX);
        if (end < 0) {
            return value;
        }
        final String name = value.substring(TOKEN_PREFIX.length(), end);
        final String root = roots.get(name);
        if (root == null) {
            return value;
        }
        final String relative = value.substring(end + TOKEN_SUFFIX.length());
        if (relative.isEmpty()) {
            return root;
        }
        return root + relative.replace('/', File.separatorChar);
    }

    private static boolean isUnder(String value, String prefix) {
        if (prefix.isEmpty() || !value.startsWith(prefix)) {
            return false;
        }
        if (value.length() == prefix.length()) {
            return true;
        }
        final char next = value.charAt(prefix.length());
        return next == '/' || next == '\\';
    }

    /**
     * Applies a transformation to every string in a nested map/collection structure, returning a new
     * structure and leaving the original untouched.
     */
    @SuppressWarnings("unchecked")
    private static Object transform(Object value, UnaryOperator<String> transform) {
        if (value instanceof Map) {
            return mapValue((Map<String, Object>) value, transform);
        }
        if (value instanceof Collection) {
            final Collection<Object> source = (Collection<Object>) value;
            final List<Object> result = new ArrayList<>(source.size());
            for (Object element : source) {
                result.add(transform(element, transform));
            }
            return result;
        }
        if (value instanceof String s) {
            return transform.apply(s);
        }
        return value;
    }

    private static Map<String, Object> mapValue(Map<String, Object> map,
            UnaryOperator<String> transform) {
        final Map<String, Object> result = new LinkedHashMap<>(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.put(entry.getKey(), transform(entry.getValue(), transform));
        }
        return result;
    }
}
