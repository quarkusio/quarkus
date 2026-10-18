package org.acme.multirelease;

public class FutureVersionOnly {
    private final FutureVersionDep dep = new FutureVersionDep();

    public String version() {
        return dep.value();
    }
}
