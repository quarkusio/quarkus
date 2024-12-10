package io.quarkus.it.testsupport.commandmode;

import jakarta.inject.Inject;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

/*
 * Because this app co-exists in a module with a QuarkusIntegrationTest, it needs to not be on the default path.
 * Otherwise, this application is executed by the QuarkusIntegrationTest and exits early, causing test failures elsewhere.
 */
@QuarkusMain(name = "test")
public class MainApp implements QuarkusApplication {

    @Inject
    CdiBean myBean;

    @Override
    public int run(String... args) throws Exception {
        System.out.println("The bean is " + myBean.myMethod());
        return 0;
    }
}