package io.quarkus.arc.test.interceptors.methodargs;

import java.util.List;
import javax.inject.Singleton;

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
