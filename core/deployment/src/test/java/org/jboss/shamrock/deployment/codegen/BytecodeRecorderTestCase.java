package org.jboss.shamrock.deployment.codegen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Consumer;

import org.jboss.protean.gizmo.TestClassLoader;
import org.jboss.shamrock.deployment.ClassOutput;
import org.jboss.shamrock.runtime.StartupContext;
import org.jboss.shamrock.runtime.StartupTask;
import org.junit.Assert;
import org.junit.Test;

public class BytecodeRecorderTestCase {

    private static final String TEST_CLASS = "com.shamrock.test.GenClass";

    @Test
    public void testPrimitiveParams() throws Exception {
        runTest(recorder -> {
            TestTemplate template = recorder.getRecordingProxy(TestTemplate.class);
            template.primitiveParams(true, (byte) 1, 'a', (short) 2, 3, 4, 5, 6);
        }, true, (byte) 1, 'a', (short) 2, 3, (long) 4, (float) 5, (double) 6);
    }

    @Test
    public void testBoxedPrimitiveParams() throws Exception {
        runTest(recorder -> {
            TestTemplate template = recorder.getRecordingProxy(TestTemplate.class);
            template.boxedPrimitiveParams(true, (byte) 1, 'a', (short) 2, 3, (long) 4, (float) 5, (double) 6);
        }, true, (byte) 1, 'a', (short) 2, 3, (long) 4, (float) 5, (double) 6);
    }

    @Test
    public void testPrimitiveArrayTypes() throws Exception {
        runTest(recorder -> {
            TestTemplate template = recorder.getRecordingProxy(TestTemplate.class);
            template.intArray(4, 5, 6);
        }, (Object) new int[]{4, 5, 6});
        runTest(recorder -> {
            TestTemplate template = recorder.getRecordingProxy(TestTemplate.class);
            template.doubleArray(4, 5, 6);
        }, (Object) new double[]{4, 5, 6});
    }

    @Test
    public void testCollections() throws Exception {
        runTest(recorder -> {
            TestTemplate template = recorder.getRecordingProxy(TestTemplate.class);
            template.list(new ArrayList<>(Arrays.asList(4, 5, 6)));
        }, Arrays.asList(4, 5, 6));
        runTest(recorder -> {
            TestTemplate template = recorder.getRecordingProxy(TestTemplate.class);
            template.set(new HashSet<>(Arrays.asList(4, 5, 6)));
        }, new HashSet<>(Arrays.asList(4, 5, 6)));
        runTest(recorder -> {
            TestTemplate template = recorder.getRecordingProxy(TestTemplate.class);
            template.map(new HashMap<>(Collections.singletonMap("a", "b")));
        }, Collections.singletonMap("a", "b"));
    }

    @Test
    public void testJavaBean() throws Exception {
        runTest(recorder -> {
            TestTemplate template = recorder.getRecordingProxy(TestTemplate.class);
            template.bean(new TestJavaBean("A string", 99));
        }, new TestJavaBean("A string", 99));
    }

    @Test
    public void testSubstitution() throws Exception {
        runTest(recorder -> {
            recorder.registerSubstitution(NonSerializable.class, NonSerializable.Serialized.class, NonSerializable.Substitution.class);
            TestTemplate template = recorder.getRecordingProxy(TestTemplate.class);
            template.bean(new NonSerializable("A string", 99));
        }, new NonSerializable("A string", 99));
    }

    void runTest(Consumer<BytecodeRecorder> generator, Object... expected) throws Exception {
        TestTemplate.RESULT.clear();
        TestClassLoader tcl = new TestClassLoader(getClass().getClassLoader());
        try (BytecodeRecorderImpl recorder = new BytecodeRecorderImpl(tcl, TEST_CLASS, StartupTask.class, new TestClassOutput(tcl))) {
            generator.accept(recorder);
        }
        StartupTask task = (StartupTask) tcl.loadClass(TEST_CLASS).newInstance();
        task.deploy(new StartupContext());
        Assert.assertEquals(expected.length, TestTemplate.RESULT.size());
        for (Object i : expected) {
            if (i.getClass().isArray()) {
                if (i instanceof int[]) {
                    Assert.assertArrayEquals((int[]) i, (int[]) TestTemplate.RESULT.poll());
                } else if (i instanceof double[]) {
                    Assert.assertArrayEquals((double[]) i, (double[]) TestTemplate.RESULT.poll(), 0);
                } else {
                    throw new RuntimeException("not implemented");
                }
            } else {
                Assert.assertEquals(i, TestTemplate.RESULT.poll());
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
