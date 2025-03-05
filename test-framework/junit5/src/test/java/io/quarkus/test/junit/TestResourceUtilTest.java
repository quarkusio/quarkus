package io.quarkus.test.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class TestResourceUtilTest {

    // Basic sense check, since most of the heavy lifting is done by TestResourceManager#getReloadGroupIdentifier
    @Test
    public void testReloadGroupIdentifierIsEqualForTestsWithNoResources() {
        String identifier1 = TestResourceUtil.getReloadGroupIdentifier(TestClass.class, ProfileClass.class);
        String identifier2 = TestResourceUtil.getReloadGroupIdentifier(TestClass.class, AnotherProfileClass.class);
        assertEquals(identifier2, identifier1);
    }

    @Test
    public void testReloadGroupIdentifierIsEqualForTestsWithIdenticalResources() {
        String identifier1 = TestResourceUtil.getReloadGroupIdentifier(TestClass.class, ProfileClassWithResources.class);
        String identifier2 = TestResourceUtil.getReloadGroupIdentifier(TestClass.class, AnotherProfileClassWithResources.class);
        assertEquals(identifier2, identifier1);
    }

    @Test
    public void testReloadGroupIdentifierIsEqualForTestsWithDifferentResources() {
        String identifier1 = TestResourceUtil.getReloadGroupIdentifier(TestClass.class, ProfileClassWithResources.class);
        String identifier2 = TestResourceUtil.getReloadGroupIdentifier(TestClass.class, ProfileClass.class);
        assertNotEquals(identifier2, identifier1);
    }
}

class TestClass {

}

class ProfileClass implements QuarkusTestProfile {

    public ProfileClass() {
    }
}

class AnotherProfileClass implements QuarkusTestProfile {

    public AnotherProfileClass() {
    }
}

class ProfileClassWithResources implements QuarkusTestProfile {

    public ProfileClassWithResources() {
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return Collections.singletonList(
                new TestResourceEntry(
                        Dummy.class, Map.of()));
    }
}

class AnotherProfileClassWithResources implements QuarkusTestProfile {

    public AnotherProfileClassWithResources() {
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return Collections.singletonList(
                new TestResourceEntry(
                        Dummy.class, Map.of()));
    }
}

abstract class Dummy implements QuarkusTestResourceLifecycleManager {
}