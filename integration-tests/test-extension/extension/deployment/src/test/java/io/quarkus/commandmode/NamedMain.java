package io.quarkus.commandmode;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain(name = "somename")
public class NamedMain {

    public static void main(String... args) {
        Quarkus.run(NamedApp.class, args);
    }

    public static class NamedApp implements QuarkusApplication {

        @Override
        public int run(String... args) throws Exception {
            System.out.println("Hello Named");
            return 100;
        }
    }
}
