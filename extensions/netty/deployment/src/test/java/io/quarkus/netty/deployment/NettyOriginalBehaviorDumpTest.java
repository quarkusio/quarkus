package io.quarkus.netty.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

/**
 * Plain JUnit test (no Quarkus) that runs against the ORIGINAL (untransformed) Netty classes.
 * It captures method results to a file so that {@link NettyTransformVerificationTest} can compare
 * them against the bytecode-transformed versions.
 * <p>
 * This test must run before {@link NettyTransformVerificationTest}.
 * Maven Surefire runs tests alphabetically by default, and since this class name sorts before
 * "NettyTransformVerification", the ordering is naturally correct.
 */
public class NettyOriginalBehaviorDumpTest {

    static final Path DUMP_FILE = Path.of("target", "netty-original-behavior.properties");

    private static final String PD0 = "io.netty.util.internal.PlatformDependent0";

    @Test
    void dumpOriginalBehavior() throws Exception {
        Map<String, String> results = new LinkedHashMap<>();

        Class<?> pd0Class = Class.forName(PD0);

        dumpIsVirtualThread(pd0Class, results);
        dumpHasMethods(pd0Class, results);
        dumpAlignSlice(pd0Class, results);
        dumpOffsetSlice(pd0Class, results);
        dumpAbsolutePutBuffer(pd0Class, results);
        dumpAbsolutePutArray(pd0Class, results);
        dumpSplittableRandomNextBytes(pd0Class, results);
        dumpMaxDirectMemory(results);

        StringBuilder sb = new StringBuilder();
        sb.append("# Original Netty behavior captured by NettyOriginalBehaviorDumpTest\n");
        for (Map.Entry<String, String> entry : results.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }
        Files.writeString(DUMP_FILE, sb.toString());

        assertThat(DUMP_FILE).exists();
        assertThat(results).isNotEmpty();
    }

    private void dumpIsVirtualThread(Class<?> pd0Class, Map<String, String> results) throws Exception {
        Method isVirtualThread = pd0Class.getDeclaredMethod("isVirtualThread", Thread.class);
        isVirtualThread.setAccessible(true);

        boolean platformResult = (boolean) isVirtualThread.invoke(null, Thread.currentThread());
        results.put("isVirtualThread.platform", String.valueOf(platformResult));

        Thread.ofVirtual().start(() -> {
            try {
                boolean virtualResult = (boolean) isVirtualThread.invoke(null, Thread.currentThread());
                results.put("isVirtualThread.virtual", String.valueOf(virtualResult));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).join();

        boolean nullResult = (boolean) isVirtualThread.invoke(null, (Thread) null);
        results.put("isVirtualThread.null", String.valueOf(nullResult));
    }

    private void dumpHasMethods(Class<?> pd0Class, Map<String, String> results) throws Exception {
        for (String methodName : new String[] {
                "hasAlignSliceMethod", "hasOffsetSliceMethod",
                "hasAbsolutePutBufferMethod", "hasAbsolutePutArrayMethod" }) {
            Method method = pd0Class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            results.put(methodName, String.valueOf(method.invoke(null)));
        }
    }

    private void dumpAlignSlice(Class<?> pd0Class, Map<String, String> results) throws Exception {
        Method alignSlice = pd0Class.getDeclaredMethod("alignSlice", ByteBuffer.class, int.class);
        alignSlice.setAccessible(true);

        ByteBuffer buffer = ByteBuffer.allocateDirect(64);
        ByteBuffer sliced = (ByteBuffer) alignSlice.invoke(null, buffer, 8);
        results.put("alignSlice.capacity", String.valueOf(sliced.capacity()));
        results.put("alignSlice.position", String.valueOf(sliced.position()));
        results.put("alignSlice.limit", String.valueOf(sliced.limit()));
    }

    private void dumpOffsetSlice(Class<?> pd0Class, Map<String, String> results) throws Exception {
        Method offsetSlice = pd0Class.getDeclaredMethod("offsetSlice", ByteBuffer.class, int.class, int.class);
        offsetSlice.setAccessible(true);

        ByteBuffer buffer = ByteBuffer.allocateDirect(64);
        for (int i = 0; i < 64; i++) {
            buffer.put(i, (byte) i);
        }
        ByteBuffer sliced = (ByteBuffer) offsetSlice.invoke(null, buffer, 10, 20);
        results.put("offsetSlice.capacity", String.valueOf(sliced.capacity()));
        results.put("offsetSlice.position", String.valueOf(sliced.position()));
        results.put("offsetSlice.limit", String.valueOf(sliced.limit()));
        results.put("offsetSlice.byte0", String.valueOf(sliced.get(0)));
        results.put("offsetSlice.byte19", String.valueOf(sliced.get(19)));
    }

    private void dumpAbsolutePutBuffer(Class<?> pd0Class, Map<String, String> results) throws Exception {
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
        results.put("absolutePutBuffer.bytes", bytesToString(dstBytes));
    }

    private void dumpAbsolutePutArray(Class<?> pd0Class, Map<String, String> results) throws Exception {
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
        results.put("absolutePutArray.bytes", bytesToString(dstBytes));
    }

    private void dumpSplittableRandomNextBytes(Class<?> pd0Class, Map<String, String> results) throws Exception {
        Method splittableRandomNextBytes = pd0Class.getDeclaredMethod("splittableRandomNextBytes",
                SplittableRandom.class, byte[].class);
        splittableRandomNextBytes.setAccessible(true);

        SplittableRandom rng = new SplittableRandom(42);
        byte[] data = new byte[16];
        splittableRandomNextBytes.invoke(null, rng, data);
        results.put("splittableRandomNextBytes.bytes", bytesToString(data));
    }

    private void dumpMaxDirectMemory(Map<String, String> results) throws Exception {
        Class<?> pdClass = Class.forName("io.netty.util.internal.PlatformDependent");
        Method maxDirectMemory = pdClass.getDeclaredMethod("maxDirectMemory");
        maxDirectMemory.setAccessible(true);
        long value = (long) maxDirectMemory.invoke(null);
        results.put("maxDirectMemory", String.valueOf(value));
    }

    static String bytesToString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(bytes[i]);
        }
        return sb.toString();
    }

    static byte[] bytesFromString(String str) {
        String[] parts = str.split(",");
        byte[] result = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Byte.parseByte(parts[i].trim());
        }
        return result;
    }
}
