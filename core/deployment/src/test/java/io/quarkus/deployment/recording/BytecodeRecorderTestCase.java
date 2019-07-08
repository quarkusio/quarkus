package io.quarkus.deployment.recording;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

import io.quarkus.deployment.ClassOutput;
import io.quarkus.gizmo.TestClassLoader;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.StartupContext;
import io.quarkus.runtime.StartupTask;

public class BytecodeRecorderTestCase {

    private static final String TEST_CLASS = "com.quarkus.test.GenClass";

    @Test
    public void testPrimitiveParams() throws Exception {
        runTest(recorder -> {
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.primitiveParams(true, (byte) 1, 'a', (short) 2, 3, 4, 5, 6);
        }, true, (byte) 1, 'a', (short) 2, 3, (long) 4, (float) 5, (double) 6);
    }

    @Test
    public void testBoxedPrimitiveParams() throws Exception {
        runTest(recorder -> {
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.boxedPrimitiveParams(true, (byte) 1, 'a', (short) 2, 3, (long) 4, (float) 5, (double) 6);
        }, true, (byte) 1, 'a', (short) 2, 3, (long) 4, (float) 5, (double) 6);
    }

    @Test
    public void testPrimitiveArrayTypes() throws Exception {
        runTest(recorder -> {
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.intArray(4, 5, 6);
        }, (Object) new int[] { 4, 5, 6 });
        runTest(recorder -> {
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.doubleArray(4, 5, 6);
        }, (Object) new double[] { 4, 5, 6 });
    }

    @Test
    public void testCollections() throws Exception {
        runTest(recorder -> {
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.list(new ArrayList<>(Arrays.asList(4, 5, 6)));
        }, Arrays.asList(4, 5, 6));
        runTest(recorder -> {
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.set(new HashSet<>(Arrays.asList(4, 5, 6)));
        }, new HashSet<>(Arrays.asList(4, 5, 6)));
        runTest(recorder -> {
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.map(new HashMap<>(Collections.singletonMap("a", "b")));
        }, Collections.singletonMap("a", "b"));
    }

    @Test
    public void testEmptyCollections() throws Exception {
        runTest(recorder -> {
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.object(Collections.emptyList());
        }, new ArrayList<>());
        runTest(recorder -> {
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.object(Collections.emptyMap());
        }, new HashMap<>());
        runTest(recorder -> {
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.object(Collections.emptySet());
        }, new HashSet<>());
        runTest(recorder -> {
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.object(Collections.emptyNavigableMap());
        }, new LinkedHashMap<>());
        runTest(recorder -> {
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.object(Collections.emptySortedMap());
        }, new TreeMap<>());
        runTest(recorder -> {
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.object(Collections.emptySortedSet());
        }, new TreeSet<>());
    }

    @Test
    public void testSingletonCollections() throws Exception {
        runTest(recorder -> {
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.object(Collections.singletonList(1));
        }, new ArrayList<>(Arrays.asList(1)));
        runTest(recorder -> {
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.object(Collections.singletonMap(1, 2));
        }, new HashMap<>(Collections.singletonMap(1, 2)));
        runTest(recorder -> {
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.object(Collections.singleton(1));
        }, new HashSet<>(Collections.singleton(1)));
    }

    @Test
    public void testJavaBean() throws Exception {
        runTest(recorder -> {
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.bean(new TestJavaBean("A string", 99));
        }, new TestJavaBean("A string", 99));
    }

    @Test
    public void testLargeCollection() throws Exception {

        List<TestJavaBean> beans = new ArrayList<>();
        for (int i = 0; i < 10000; ++i) {
            beans.add(new TestJavaBean("A string", 99));
        }

        runTest(recorder -> {
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.list(beans);
        }, beans);
    }

    @Test
    public void testLargeArray() throws Exception {

        List<TestJavaBean> beans = new ArrayList<>();
        for (int i = 0; i < 1000; ++i) {
            beans.add(new TestJavaBean("A string", 99));
        }

        runTest(recorder -> {
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.array(beans.toArray());
        }, (Object) beans.toArray());
    }

    @Test
    public void testLargeNumberOfInvocations() throws Exception {
        List<TestJavaBean> beans = new ArrayList<>();
        for (int i = 0; i < 10000; ++i) {
            beans.add(new TestJavaBean("A string", 99));
        }

        runTest(recorder -> {
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            for (TestJavaBean i : beans) {
                template.bean(i);
            }
        }, beans.toArray());
    }

    @Test
    public void testSubstitution() throws Exception {
        runTest(recorder -> {
            recorder.registerSubstitution(NonSerializable.class, NonSerializable.Serialized.class,
                    NonSerializable.Substitution.class);
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.bean(new NonSerializable("A string", 99));
        }, new NonSerializable("A string", 99));
    }

    @Test
    public void testNewInstance() throws Exception {
        runTest(recorder -> {
            RuntimeValue<TestJavaBean> instance = recorder.newInstance(TestJavaBean.class.getName());
            TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
            template.add(instance);
            template.add(instance);
            template.result(instance);
        }, new TestJavaBean(null, 2));
    }

    @Test
    public void testRecordingProxyToStringNotNull() {
        TestClassLoader tcl = new TestClassLoader(getClass().getClassLoader());
        BytecodeRecorderImpl recorder = new BytecodeRecorderImpl(tcl, false, TEST_CLASS);
        TestRecorder template = recorder.getRecordingProxy(TestRecorder.class);
        Assert.assertNotNull(template.toString());
        Assert.assertTrue(template.toString().contains("$$RecordingProxyProxy"));
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
