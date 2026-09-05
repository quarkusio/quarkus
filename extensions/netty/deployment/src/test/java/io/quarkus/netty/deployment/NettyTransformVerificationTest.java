package io.quarkus.netty.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.SplittableRandom;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Runs inside Quarkus (bytecode transforms applied) and compares the behavior of the
 * transformed Netty classes against the reference data captured by {@link NettyOriginalBehaviorDumpTest}.
 * <p>
 * This test verifies that our bytecode rewrites produce the same observable behavior as the
 * original MethodHandle-based code. It checks both the method return values AND the internal
 * state (MethodHandle fields should be null, has* guards should be true) to ensure the optimized
 * code path is actually taken.
 */
public class NettyTransformVerificationTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withEmptyApplication();

    private static final String PD0 = "io.netty.util.internal.PlatformDependent0";
    private static final Path DUMP_FILE = Path.of("target", "netty-original-behavior.properties");

    private static Map<String, String> originalBehavior;

    @BeforeAll
    static void loadOriginalBehavior() throws Exception {
        assumeTrue(Files.exists(DUMP_FILE),
                "Reference data file from NettyOriginalBehaviorDumpTest not found at " + DUMP_FILE
                        + ". This test must run after NettyOriginalBehaviorDumpTest.");

        originalBehavior = new HashMap<>();
        for (String line : Files.readAllLines(DUMP_FILE)) {
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq > 0) {
                originalBehavior.put(line.substring(0, eq), line.substring(eq + 1));
            }
        }
    }

    @Test
    void methodHandleFieldsAreNull() throws Exception {
        Class<?> pd0Class = Class.forName(PD0);

        for (String fieldName : new String[] {
                "ALIGN_SLICE", "OFFSET_SLICE", "ABSOLUTE_PUT_BUFFER",
                "ABSOLUTE_PUT_ARRAY", "SPLITTABLE_RANDOM_NEXT_BYTES" }) {
            Field field = pd0Class.getDeclaredField(fieldName);
            field.setAccessible(true);
            assertThat(field.get(null))
                    .as("MethodHandle field %s should be null (clinit patch should have skipped the lookup)", fieldName)
                    .isNull();
        }

        Field isVirtualField = pd0Class.getDeclaredField("IS_VIRTUAL_THREAD_METHOD_HANDLE");
        isVirtualField.setAccessible(true);
        assertThat(isVirtualField.get(null))
                .as("IS_VIRTUAL_THREAD_METHOD_HANDLE should be null (getIsVirtualThreadMethodHandle returns null)")
                .isNull();
    }

    @Test
    void hasMethodsReturnTrue() throws Exception {
        Class<?> pd0Class = Class.forName(PD0);

        for (String methodName : new String[] {
                "hasAlignSliceMethod", "hasOffsetSliceMethod",
                "hasAbsolutePutBufferMethod", "hasAbsolutePutArrayMethod" }) {
            Method method = pd0Class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            boolean result = (boolean) method.invoke(null);
            assertThat(result)
                    .as("%s must return true so the optimized path is used", methodName)
                    .isTrue();
            assertThat(String.valueOf(result))
                    .as("%s should match original behavior", methodName)
                    .isEqualTo(originalBehavior.get(methodName));
        }
    }

    @Test
    void isVirtualThread() throws Exception {
        Class<?> pd0Class = Class.forName(PD0);
        Method isVirtualThread = pd0Class.getDeclaredMethod("isVirtualThread", Thread.class);
        isVirtualThread.setAccessible(true);

        boolean platformResult = (boolean) isVirtualThread.invoke(null, Thread.currentThread());
        assertThat(String.valueOf(platformResult))
                .isEqualTo(originalBehavior.get("isVirtualThread.platform"));

        Thread.ofVirtual().start(() -> {
            try {
                boolean virtualResult = (boolean) isVirtualThread.invoke(null, Thread.currentThread());
                assertThat(String.valueOf(virtualResult))
                        .isEqualTo(originalBehavior.get("isVirtualThread.virtual"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).join();

        boolean nullResult = (boolean) isVirtualThread.invoke(null, (Thread) null);
        assertThat(String.valueOf(nullResult))
                .isEqualTo(originalBehavior.get("isVirtualThread.null"));
    }

    @Test
    void alignSlice() throws Exception {
        Class<?> pd0Class = Class.forName(PD0);
        Method alignSlice = pd0Class.getDeclaredMethod("alignSlice", ByteBuffer.class, int.class);
        alignSlice.setAccessible(true);

        ByteBuffer buffer = ByteBuffer.allocateDirect(64);
        ByteBuffer sliced = (ByteBuffer) alignSlice.invoke(null, buffer, 8);

        assertThat(String.valueOf(sliced.capacity()))
                .isEqualTo(originalBehavior.get("alignSlice.capacity"));
        assertThat(String.valueOf(sliced.position()))
                .isEqualTo(originalBehavior.get("alignSlice.position"));
        assertThat(String.valueOf(sliced.limit()))
                .isEqualTo(originalBehavior.get("alignSlice.limit"));
    }

    @Test
    void offsetSlice() throws Exception {
        Class<?> pd0Class = Class.forName(PD0);
        Method offsetSlice = pd0Class.getDeclaredMethod("offsetSlice", ByteBuffer.class, int.class, int.class);
        offsetSlice.setAccessible(true);

        ByteBuffer buffer = ByteBuffer.allocateDirect(64);
        for (int i = 0; i < 64; i++) {
            buffer.put(i, (byte) i);
        }
        ByteBuffer sliced = (ByteBuffer) offsetSlice.invoke(null, buffer, 10, 20);

        assertThat(String.valueOf(sliced.capacity()))
                .isEqualTo(originalBehavior.get("offsetSlice.capacity"));
        assertThat(String.valueOf(sliced.position()))
                .isEqualTo(originalBehavior.get("offsetSlice.position"));
        assertThat(String.valueOf(sliced.limit()))
                .isEqualTo(originalBehavior.get("offsetSlice.limit"));
        assertThat(String.valueOf(sliced.get(0)))
                .isEqualTo(originalBehavior.get("offsetSlice.byte0"));
        assertThat(String.valueOf(sliced.get(19)))
                .isEqualTo(originalBehavior.get("offsetSlice.byte19"));
    }

    @Test
    void absolutePutBuffer() throws Exception {
        Class<?> pd0Class = Class.forName(PD0);
        Method absolutePut = pd0Class.getDeclaredMethod("absolutePut",
                ByteBuffer.class, int.class, ByteBuffer.class, int.class, int.class);
        absolutePut.setAccessible(true);

        ByteBuffer dst = ByteBuffer.allocateDirect(32);
        ByteBuffer src = ByteBuffer.allocateDirect(16);
        for (int i = 0; i < 16; i++) {
            src.put(i, (byte) (100 + i));
        }
        ByteBuffer returnedDst = (ByteBuffer) absolutePut.invoke(null, dst, 4, src, 2, 8);
        byte[] dstBytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            dstBytes[i] = returnedDst.get(4 + i);
        }

        assertThat(bytesToString(dstBytes))
                .isEqualTo(originalBehavior.get("absolutePutBuffer.bytes"));
    }

    @Test
    void absolutePutArray() throws Exception {
        Class<?> pd0Class = Class.forName(PD0);
        Method absolutePut = pd0Class.getDeclaredMethod("absolutePut",
                ByteBuffer.class, int.class, byte[].class, int.class, int.class);
        absolutePut.setAccessible(true);

        ByteBuffer dst = ByteBuffer.allocateDirect(32);
        byte[] src = new byte[16];
        for (int i = 0; i < 16; i++) {
            src[i] = (byte) (50 + i);
        }
        ByteBuffer returnedDst = (ByteBuffer) absolutePut.invoke(null, dst, 2, src, 3, 10);
        byte[] dstBytes = new byte[10];
        for (int i = 0; i < 10; i++) {
            dstBytes[i] = returnedDst.get(2 + i);
        }

        assertThat(bytesToString(dstBytes))
                .isEqualTo(originalBehavior.get("absolutePutArray.bytes"));
    }

    @Test
    void splittableRandomNextBytes() throws Exception {
        Class<?> pd0Class = Class.forName(PD0);
        Method splittableRandomNextBytes = pd0Class.getDeclaredMethod("splittableRandomNextBytes",
                SplittableRandom.class, byte[].class);
        splittableRandomNextBytes.setAccessible(true);

        SplittableRandom rng = new SplittableRandom(42);
        byte[] data = new byte[16];
        splittableRandomNextBytes.invoke(null, rng, data);

        assertThat(bytesToString(data))
                .isEqualTo(originalBehavior.get("splittableRandomNextBytes.bytes"));
    }

    @Test
    void maxDirectMemory() throws Exception {
        Class<?> pdClass = Class.forName("io.netty.util.internal.PlatformDependent");
        Method maxDirectMemory = pdClass.getDeclaredMethod("maxDirectMemory");
        maxDirectMemory.setAccessible(true);
        long value = (long) maxDirectMemory.invoke(null);

        assertThat(value)
                .as("maxDirectMemory should be positive")
                .isGreaterThan(0);
        assertThat(String.valueOf(value))
                .isEqualTo(originalBehavior.get("maxDirectMemory"));
    }

    private static String bytesToString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(bytes[i]);
        }
        return sb.toString();
    }
}
