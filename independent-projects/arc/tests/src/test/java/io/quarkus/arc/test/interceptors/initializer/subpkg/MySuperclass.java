package io.quarkus.arc.test.interceptors.initializer.subpkg;

import jakarta.inject.Inject;

public class MySuperclass {
    public boolean publicInitializerCalled;
    public boolean protectedInitializerCalled;
    public boolean packagePrivateInitializerCalled;
    public boolean privateInitializerCalled;

    @Inject
    public void publicInject(MyDependency ignored) {
        publicInitializerCalled = true;
    }

    @Inject
    protected void protectedInject(MyDependency ignored) {
        protectedInitializerCalled = true;
    }

    @Inject
    void packagePrivateInject(MyDependency ignored) {
        packagePrivateInitializerCalled = true;
    }

    @Inject
    private void privateInject(MyDependency ignored) {
        privateInitializerCalled = true;
    }
}
