package org.acme;

import io.quarkus.arc.profile.IfBuildProfile;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.io.FileUtils;
import org.apache.commons.collections4.MultiSet;

public interface HelloService {
    String name();

    default String classFounds() {
        String result = "";
        try {
            result += FileUtils.class.getSimpleName();
        } catch (NoClassDefFoundError e) {
            result += "?";
        }
        result += "/";
        try {
            result += MultiSet.class.getSimpleName();
        } catch (NoClassDefFoundError e) {
            result += "?";
        }
        return result;
    }

    @IfBuildProfile("foo")
    @ApplicationScoped
    class HelloServiceFoo implements HelloService{

        @Override
        public String name() {
            return "from foo";
        }
    }

    @IfBuildProfile("bar")
    @ApplicationScoped
    class HelloServiceBar implements HelloService{

        @Override
        public String name() {
            return "from bar";
        }
    }
}
