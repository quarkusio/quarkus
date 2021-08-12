package io.quarkus.deployment.recording;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.TestClassLoader;
import io.quarkus.gizmo.ClassOutput;
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

        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.bean(new TestJavaBeanSubclass("A string", 99, "PUT"));
        }, new TestJavaBeanSubclass("A string", 99, "PUT"));
    }

    @Test
    public void testValidationFails() throws Exception {
        Assertions.assertThrows(RuntimeException.class, () -> {
            runTest(generator -> {
                TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
                ValidationFails validationFails = new ValidationFails();
                validationFails.setName("Stuart Douglas");
                recorder.object(validationFails);
            });
        });
    }

    @Test
    public void testRelaxedValidationSucceeds() throws Exception {
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            ValidationFails validationFails = new ValidationFails();
            validationFails.setName("Stuart Douglas");
            recorder.relaxedObject(validationFails);
        }, new ValidationFails("Stuart Douglas"));
    }

    @Test
    public void testIgnoredProperties() throws Exception {
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            IgnoredProperties ignoredProperties = new IgnoredProperties();
            ignoredProperties.setNotIgnored("Shows up");
            ignoredProperties.setIgnoredField("Does not show up");
            recorder.ignoredProperties(ignoredProperties);
        }, new IgnoredProperties("Shows up", null));
    }

    @Test
    public void testJavaBeanWithEmbeddedReturnValue() throws Exception {
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            TestJavaBean newBean = new TestJavaBean("A string", 99);
            newBean.setSupplier(recorder.stringSupplier("Runtime String"));
            recorder.bean(newBean);
        }, new TestJavaBean("A string", 99, new Supplier<String>() {
            @Override
            public String get() {
                return "Runtime String";
            }
        }));
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
    public void testUnmodifiableMapWithinAMap() throws Exception {
        Map<Integer, Map<Integer, TestJavaBean>> outerMap = new HashMap<>();
        outerMap.put(1, Collections.unmodifiableMap(
                Collections.singletonMap(1, new TestJavaBean())));

        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.map(outerMap);
        }, outerMap);
    }

    @Test
    public void testUnmodifiableListWithinAMap() throws Exception {
        Map<Integer, List<TestJavaBean>> map = new HashMap<>();
        map.put(1, Collections.unmodifiableList(Collections.singletonList(new TestJavaBean())));

        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.map(map);
        }, map);
    }

    @Test
    public void testUnmodifiableSetWithinAMap() throws Exception {
        Map<Integer, Set<TestJavaBean>> map = new HashMap<>();
        map.put(1, Collections.unmodifiableSet(Collections.singleton(new TestJavaBean())));

        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.map(map);
        }, map);
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
    public void testRecordableConstructor() throws Exception {
        runTest(generator -> {
            TestConstructorBean bean = new TestConstructorBean("John", "Citizen");
            bean.setAge(30);
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.bean(bean);
        }, new TestConstructorBean("John", "Citizen").setAge(30));
    }

    @Test
    public void testRecordingProxyToStringNotNull() {
        TestClassLoader tcl = new TestClassLoader(getClass().getClassLoader());
        BytecodeRecorderImpl generator = new BytecodeRecorderImpl(tcl, false, TEST_CLASS);
        TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
        assertNotNull(recorder.toString());
        assertTrue(recorder.toString().contains("$$RecordingProxyProxy"));
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
        Optional<TestJavaBean> optionalWithCustomClass = Optional.of(new TestJavaBean());
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            recorder.object(optionalWithCustomClass);
        }, optionalWithCustomClass);
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

    // Boxed booleans whose getter starts with `is`, in particular, used to be ignored.
    @Test
    public void testJavaBeanWithBoolean() throws Exception {
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            TestJavaBeanWithBoolean newBean = new TestJavaBeanWithBoolean(true, true, true);
            recorder.bean(newBean);
        }, new TestJavaBeanWithBoolean(true, true, true));
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            TestJavaBeanWithBoolean newBean = new TestJavaBeanWithBoolean(false, false, false);
            recorder.bean(newBean);
        }, new TestJavaBeanWithBoolean(false, false, false));
        runTest(generator -> {
            TestRecorder recorder = generator.getRecordingProxy(TestRecorder.class);
            TestJavaBeanWithBoolean newBean = new TestJavaBeanWithBoolean(true, null, null);
            recorder.bean(newBean);
        }, new TestJavaBeanWithBoolean(true, null, null));
    }

    void runTest(Consumer<BytecodeRecorderImpl> generator, Object... expected) throws Exception {
        TestRecorder.RESULT.clear();
        TestClassLoader tcl = new TestClassLoader(getClass().getClassLoader());
        BytecodeRecorderImpl recorder = new BytecodeRecorderImpl(tcl, false, TEST_CLASS);
        generator.accept(recorder);
        recorder.writeBytecode(new TestClassOutput(tcl));

        StartupTask task = (StartupTask) tcl.loadClass(TEST_CLASS).getDeclaredConstructor().newInstance();
        task.deploy(new StartupContext());
        assertEquals(expected.length, TestRecorder.RESULT.size());
        for (Object i : expected) {
            if (i.getClass().isArray()) {
                if (i instanceof int[]) {
                    assertArrayEquals((int[]) i, (int[]) TestRecorder.RESULT.poll());
                } else if (i instanceof double[]) {
                    assertArrayEquals((double[]) i, (double[]) TestRecorder.RESULT.poll(), 0);
                } else if (i instanceof Object[]) {
                    assertArrayEquals((Object[]) i, (Object[]) TestRecorder.RESULT.poll());
                } else {
                    throw new RuntimeException("not implemented");
                }
            } else {
                assertEquals(i, TestRecorder.RESULT.poll());
            }
        }
    }

    private static class TestClassOutput implements ClassOutput {
        private final TestClassLoader tcl;

        public TestClassOutput(TestClassLoader tcl) {
            this.tcl = tcl;
        }

        @Override
        public void write(String s, byte[] bytes) {
            tcl.write(s, bytes);
        }
    }
}
