package io.quarkus.test.component;

final class Conditions {

    private Conditions() {
    }

    static boolean isFacadeLoaderUsed() {
        try {
            ComponentLauncherSessionListener.class.getClassLoader()
                    .loadClass("io.quarkus.test.junit.classloading.FacadeClassLoader");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static boolean isContinuousTestingDiscovery() {
        return Boolean.parseBoolean(System.getProperty("quarkus.continuous-tests-discovery"));
    }

}
