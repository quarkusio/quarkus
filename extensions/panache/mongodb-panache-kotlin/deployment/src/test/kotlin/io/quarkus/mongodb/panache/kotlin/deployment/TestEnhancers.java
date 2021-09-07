package io.quarkus.mongodb.panache.kotlin.deployment;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.StringJoiner;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import io.quarkus.gizmo.Gizmo;
import io.quarkus.mongodb.panache.kotlin.PanacheMongoCompanionBase;
import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepositoryBase;
import io.quarkus.panache.common.deployment.ByteCodeType;
import io.quarkus.panache.common.impl.GenerateBridge;
import io.quarkus.test.QuarkusUnitTest;

@SuppressWarnings("unchecked")
public class TestEnhancers {
    public static final ByteCodeType CLASS = new ByteCodeType(Class.class);
    public static final ByteCodeType OBJECT_ID = new ByteCodeType(ObjectId.class);
    public static final ByteCodeType LONG = new ByteCodeType(long.class);
    public static final ByteCodeType GENERATE_BRIDGE = new ByteCodeType(GenerateBridge.class);
    public static final Pattern LABEL = Pattern.compile("^L(\\d)$");
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Book.class)
                    .addClasses(Student.class)
                    .addClasses(StudentRepository.class));

    @Test
    public void testCompanion() throws Exception {
        evaluate(new ByteCodeType(Book.Companion.getClass()), new ByteCodeType(Book.class), PanacheMongoCompanionBase.class, OBJECT_ID);
    }

    @Test
    public void testRepository() throws Exception {
        evaluate(new ByteCodeType(StudentRepository.class), new ByteCodeType(Student.class), PanacheMongoRepositoryBase.class, LONG);
    }

    private void evaluate(ByteCodeType testClass, ByteCodeType entityType, Class<?> baseType, ByteCodeType idType) throws Exception {
        ByteCodeType object = new ByteCodeType(Object.class);
        Map<String, BytecodeMethod> localMethods = findLocalMethods(testClass);

        Map<String, MethodNode> bridgeMethods = getBridgeMethods(baseType);
        List<String> missing = bridgeMethods.keySet().stream()
                .map(key -> {
                    if (key.contains("ById")) {
                        key = key.replace(object.descriptor(), idType.descriptor());
                        if (key.contains("findById")) {
                            key = key.substring(0, key.lastIndexOf(')') + 1) + entityType.descriptor();
                        }
                    }
                    return "target_" + key;
                })
                .filter(key -> localMethods.get(key) == null)
                .map(key -> String.format("%nCouldn't find %s in %s", key, testClass.dotName()))
                .collect(Collectors.toList());
        assertTrue(missing.isEmpty(), missing.toString());

        localMethods.entrySet().stream()
                .filter(e -> e.getKey().startsWith("target_"))
                .forEach(entry -> {
                    String key = entry.getKey().substring(7);
                    BytecodeMethod generatedMethod = localMethods.get(key);
                    BytecodeMethod targetMethod = entry.getValue();
                    try {
                        validate(key, generatedMethod.body, targetMethod.body);
                    } catch (NullPointerException e) {
                        throw new RuntimeException("could not find " + key);
                    }
                });
    }

    @NotNull
    private Map<String, BytecodeMethod> findLocalMethods(ByteCodeType byteCodeType) throws Exception {
        Printer printer = new Textifier();
        String path = "/" + byteCodeType.internalName() + ".class";
        InputStream inputStream = config.getArchiveProducer().get().get(path).getAsset().openStream();
        new ClassReader(inputStream).accept(new TraceClassVisitor(
                new ClassNode(Gizmo.ASM_API_VERSION), printer, null), ClassReader.EXPAND_FRAMES);
        Map<String, BytecodeMethod> methods = new HashMap<>();
        ListIterator<Object> iterator = printer.text.listIterator();
        while (iterator.hasNext()) {
            Object t = iterator.next();
            if (t instanceof String) {
                String line = (String) t;
                if (line.trim().startsWith("// access flags") && !line.contains("synthetic bridge")) {
                    Object next = iterator.next();
                    if (next instanceof List) {
                        BytecodeMethod method = BytecodeMethod.parse(line, (List<Object>) next);
                        methods.put(method.name, method);
                    } else {
                        iterator.previous();
                    }
                }
            }
        }
        return methods;
    }

    private Map<String, MethodNode> getBridgeMethods(Class<?> type) throws IOException {
        ClassNode node = new ClassNode(Gizmo.ASM_API_VERSION);
        new ClassReader(type.getName())
                .accept(node, ClassReader.SKIP_FRAMES);
        Map<String, MethodNode> map = new HashMap<>();
        node.methods.stream()
                .filter(m -> m.visibleAnnotations != null && m.visibleAnnotations.stream()
                        .map(a -> a.desc)
                        .collect(toList()).contains(GENERATE_BRIDGE.descriptor()))
                .forEach(m -> map.put(m.name + m.desc, m));
        return map;
    }

    private void validate(String key, List<Object> actual, List<Object> expected) {
        assertEquals(cleanUp(expected), cleanUp(actual), format("%s does not match", key));
    }

    /**
     * we don't generate labels, e.g., like the compiler does (it's sourcemap related) so we need to elide some lines
     * to account for the difference.
     *
     * @return
     */
    private List<Object> cleanUp(List<Object> input) {
        List<Object> results = new ArrayList<>(input);
        ListIterator<Object> iterator = results.listIterator();
        while (iterator.hasNext()) {
            Object next = iterator.next();
            if (next instanceof String) {
                String line = ((String) next).trim();
                if (line.startsWith("LINENUMBER") || LABEL.matcher(line).matches()) {
                    iterator.remove();
                } else if (line.startsWith("LOCALVARIABLE")) {
                    iterator.set(line.replaceAll(" L\\d.*", "\n"));
                } else if (line.startsWith("IFNONNULL") || line.startsWith("LDC") && line.contains("\\u2026")) {
                    iterator.set(line.substring(0, line.indexOf(" ") + 1) + "\n");
                }
            }
        }
        return results;
    }

    private static class BytecodeMethod {
        private final List<Object> body;
        private final String name;

        public BytecodeMethod(String name, List<Object> body) {
            this.name = name;
            this.body = body;
        }

        public static BytecodeMethod parse(String input, List<Object> body) {
            String[] split = input.trim().split("\\n");

            String[] nameSplit = split[split.length - 1].trim().split(" ");
            String name = nameSplit[nameSplit.length - 1];

            return new BytecodeMethod(name, body);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", BytecodeMethod.class.getSimpleName() + "[", "]")
                    .add("name = " + name)
                    .add("\nbody = " + body.toString())
                    .toString();
        }
    }
}
