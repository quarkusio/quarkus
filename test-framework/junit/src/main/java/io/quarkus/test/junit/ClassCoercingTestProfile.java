package io.quarkus.test.junit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO maybe use this in the resource maker too?

public class ClassCoercingTestProfile implements QuarkusTestProfile {
    private final QuarkusTestProfile profile;
    private final Object uncast;

    public ClassCoercingTestProfile(Object uncast) {
        this.uncast = uncast;
        if (uncast instanceof QuarkusTestProfile) {
            this.profile = (QuarkusTestProfile) uncast;
        } else {
            this.profile = null;
        }
    }

    private Object invokeReflectively(String methodName) {
        // TODO instance variable these?
        try {
            Method method = uncast.getClass().getMethod(methodName);
            return method.invoke(uncast);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        if (profile != null) {
            return profile.getEnabledAlternatives();
        } else {
            return (Set<Class<?>>) invokeReflectively("getEnabledAlternatives");
        }
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        if (profile != null) {
            return profile.getConfigOverrides();
        } else {
            return (Map<String, String>) invokeReflectively("getConfigOverrides");
        }
    }

    @Override
    public String getConfigProfile() {
        if (profile != null) {
            return profile.getConfigProfile();
        } else {
            return (String) invokeReflectively("getConfigProfile");
        }
    }

    @Override
    public List<TestResourceEntry> testResources() {
        // TODO this is not safe, because testResources will be in the wrong class, so we would need to wrap them as well
        if (profile != null) {
            return profile.testResources();
        } else {
            return (List<TestResourceEntry>) invokeReflectively("testResources");
        }
    }

    @Override
    public boolean disableGlobalTestResources() {
        if (profile != null) {
            return profile.disableGlobalTestResources();
        } else {
            return (boolean) invokeReflectively("disableGlobalTestResources");
        }
    }

    @Override
    public Set<String> tags() {
        if (profile != null) {
            return profile.tags();
        } else {
            return (Set<String>) invokeReflectively("tags");
        }
    }

    @Override
    public String[] commandLineParameters() {
        if (profile != null) {
            return profile.commandLineParameters();
        } else {
            return (String[]) invokeReflectively("commandLineParameters");
        }
    }

    @Override
    public boolean runMainMethod() {
        if (profile != null) {
            return profile.runMainMethod();
        } else {
            return (boolean) invokeReflectively("runMainMethod");
        }
    }

    @Override
    public boolean disableApplicationLifecycleObservers() {
        if (profile != null) {
            return profile.disableApplicationLifecycleObservers();
        } else {
            return (boolean) invokeReflectively("disableApplicationLifecycleObservers");
        }
    }

}
