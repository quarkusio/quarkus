package io.quarkus.deployment.recording;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

import io.quarkus.deployment.ClassOutput;
import io.quarkus.gizmo.TestClassLoader;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.StartupContext;
import io.quarkus.runtime.StartupTask;

public class BytecodeRecorderTestCase {

    private static final String TEST_CLASS = "com.quarkus.test.GenClass";

    @Test
    public void testPrimitiveParams() throws Exception {
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.primitiveParams(true, (byte) 1, 'a', (short) 2, 3, 4, 5, 6);
        }, true, (byte) 1, 'a', (short) 2, 3, (long) 4, (float) 5, (double) 6);
    }

    @Test
    public void testBoxedPrimitiveParams() throws Exception {
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.boxedPrimitiveParams(true, (byte) 1, 'a', (short) 2, 3, (long) 4, (float) 5, (double) 6);
        }, true, (byte) 1, 'a', (short) 2, 3, (long) 4, (float) 5, (double) 6);
    }

    @Test
    public void testPrimitiveArrayTypes() throws Exception {
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.intArray(4, 5, 6);
        }, (Object) new int[] { 4, 5, 6 });
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.doubleArray(4, 5, 6);
        }, (Object) new double[] { 4, 5, 6 });
    }

    @Test
    public void testCollections() throws Exception {
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.list(new ArrayList<>(Arrays.asList(4, 5, 6)));
        }, Arrays.asList(4, 5, 6));
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.set(new HashSet<>(Arrays.asList(4, 5, 6)));
        }, new HashSet<>(Arrays.asList(4, 5, 6)));
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.map(new HashMap<>(Collections.singletonMap("a", "b")));
        }, Collections.singletonMap("a", "b"));
    }

    @Test
    public void testEmptyCollections() throws Exception {
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.object(Collections.emptyList());
        }, new ArrayList<>());
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.object(Collections.emptyMap());
        }, new HashMap<>());
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.object(Collections.emptySet());
        }, new HashSet<>());
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.object(Collections.emptyNavigableMap());
        }, new LinkedHashMap<>());
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.object(Collections.emptySortedMap());
        }, new TreeMap<>());
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.object(Collections.emptySortedSet());
        }, new TreeSet<>());
    }

    @Test
    public void testSingletonCollections() throws Exception {
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.object(Collections.singletonList(1));
        }, new ArrayList<>(Arrays.asList(1)));
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.object(Collections.singletonMap(1, 2));
        }, new HashMap<>(Collections.singletonMap(1, 2)));
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.object(Collections.singleton(1));
        }, new HashSet<>(Collections.singleton(1)));
    }

    @Test
    public void testJavaBean() throws Exception {
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.bean(new TestJavaBean("A string", 99));
        }, new TestJavaBean("A string", 99));
    }

    @Test
    public void testLargeCollection() throws Exception {

        List<TestJavaBean> beans = new ArrayList<>();
        for (int i = 0; i < 10000; ++i) {
            beans.add(new TestJavaBean("A string", 99));
        }

        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.list(beans);
        }, beans);
    }

    @Test
    public void testLargeArray() throws Exception {

        List<TestJavaBean> beans = new ArrayList<>();
        for (int i = 0; i < 1000; ++i) {
            beans.add(new TestJavaBean("A string", 99));
        }

        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.array(beans.toArray());
        }, (Object) beans.toArray());
    }

    @Test
    public void testLargeNumberOfInvocations() throws Exception {
        List<TestJavaBean> beans = new ArrayList<>();
        for (int i = 0; i < 10000; ++i) {
            beans.add(new TestJavaBean("A string", 99));
        }

        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            for (TestJavaBean i : beans) {
                recorder.bean(i);
            }
        }, beans.toArray());
    }

    @Test
    public void testSubstitution() throws Exception {
        runTest(generator -> {
            generator.registerSubstitution(NonSerializable.class, NonSerializable.Serialized.class,
                    NonSerializable.Substitution.class);
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.bean(new NonSerializable("A string", 99));
        }, new NonSerializable("A string", 99));
    }

    @Test
    public void testNewInstance() throws Exception {
        runTest(generator -> {
            RuntimeValue<TestJavaBean> instance = generator.newInstance(TestJavaBean.class.getName());
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.add(instance);
            recorder.add(instance);
            recorder.result(instance);
        }, new TestJavaBean(null, 2));
    }

    @Test
    public void testRecordingProxyToStringNotNull() {
        TestClassLoader tcl = new TestClassLoader(getClass().getClassLoader());
        BytecodeRecorderImpl generator = new BytecodeRecorderImpl(tcl, false, TEST_CLASS);
        TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
        Assert.assertNotNull(recorder.toString());
        Assert.assertTrue(recorder.toString().contains("$$RecordingProxyProxy"));
    }

    @Test
    public void testObjects() throws Exception {
        Optional<String> quarkusOptional = Optional.of("quarkus");
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.object(quarkusOptional);
        }, quarkusOptional);
        Optional<?> emptyOptional = Optional.empty();
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.object(emptyOptional);
        }, emptyOptional);
        URL url = new URL("https://quarkus.io");
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.object(url);
        }, url);
        LaunchMode launchMode = LaunchMode.TEST;
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.object(launchMode);
        }, launchMode);
        Duration duration = Duration.ofSeconds(30);
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.object(duration);
        }, duration);
    }

    void runTest(Consumer<BytecodeRecorderImpl> generator, Object... expected) throws Exception {
        TestRecorder.RESULT.clear();
        TestClassLoader tcl = new TestClassLoader(getClass().getClassLoader());
        BytecodeRecorderImpl recorder = new BytecodeRecorderImpl(tcl, false, TEST_CLASS);
        generator.accept(recorder);
        recorder.writeBytecode(new TestClassOutput(tcl));

        StartupTask task = (StartupTask) tcl.loadClass(TEST_CLASS).newInstance();
        task.deploy(new StartupContext());
        Assert.assertEquals(expected.length, TestRecorder.RESULT.size());
        for (Object i : expected) {
            if (i.getClass().isArray()) {
                if (i instanceof int[]) {
                    Assert.assertArrayEquals((int[]) i, (int[]) TestRecorder.RESULT.poll());
                } else if (i instanceof double[]) {
                    Assert.assertArrayEquals((double[]) i, (double[]) TestRecorder.RESULT.poll(), 0);
                } else if (i instanceof Object[]) {
                    Assert.assertArrayEquals((Object[]) i, (Object[]) TestRecorder.RESULT.poll());
                } else {
                    throw new RuntimeException("not implemented");
                }
            } else {
                Assert.assertEquals(i, TestRecorder.RESULT.poll());
            }
        }
    }

    private static class TestClassOutput implements ClassOutput {
        private final TestClassLoader tcl;

        public TestClassOutput(TestClassLoader tcl) {
            this.tcl = tcl;
        }

        @Override
        public void writeClass(boolean applicationClass, String className, byte[] data) throws IOException {
            tcl.write(className, data);
        }

        @Override
        public void writeResource(String name, byte[] data) throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}
