package io.quarkus.arc.test.interceptors.methodargs;

import jakarta.inject.Singleton;
import java.util.List;

@Singleton
@Simple
public class ExampleResource {

    public String create(List<String> strings) {
        return String.join(",", strings);
    }

    String otherCreate(PackagePrivate packagePrivate) {
        return packagePrivate.toString();
    }

    static class PackagePrivate {

        @Override
        public String toString() {
            return "PackagePrivate{}";
        }
    }

}
