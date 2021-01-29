package org.acme;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class HelloCommando implements QuarkusApplication {

    @Override
    public int run(String... args) throws Exception {
        final String name = args.length > 0 ? String.join(" ", args) : "Commando";
        System.out.println("Hello " + name);
        return 0;
    }
}
