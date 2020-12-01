package org.acme;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class {main.class-name} implements QuarkusApplication {

    @Override
    public int run(String... args) throws Exception {
        final String name = args.length > 0 ? String.join(" ", args) : "{greeting.default-name}";
        System.out.println("{greeting.message} " + name);
        return 0;
    }
}
