package io.quarkus.deployment.dev.testing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import io.quarkus.test.junit.QuarkusTestProfile;

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

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        if (profile != null) {
            return profile.getEnabledAlternatives();
        } else {
            // TODO instance variable these?
            try {
                Method method = uncast.getClass().getMethod("getEnabledAlternatives");
                return (Set<Class<?>>) method.invoke(uncast);
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

        }
    }

}
