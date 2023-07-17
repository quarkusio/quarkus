package io.quarkus.commandmode;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class BothTypesMain implements QuarkusApplication {
    @Override
    public int run(String... args) throws Exception {
        System.out.println("QUARKUS MAIN");
        return 10;
    }

    public static void main(String... args) {
        System.out.println("STATIC MAIN");
        Quarkus.run(BothTypesMain.class, args);
    }
}
