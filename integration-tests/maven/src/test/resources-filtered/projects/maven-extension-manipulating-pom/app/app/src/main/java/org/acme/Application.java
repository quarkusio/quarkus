package org.acme;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;

import org.acme.greeter.Greeter;

@QuarkusMain
public class Application implements QuarkusApplication {

    @Inject
    Greeter greeter;

    @Override
    public int run(String... args) throws Exception {
        String msg = greeter.getGreeting();
        if(args.length > 0) {
            msg += ", " + args[0] + "!";
        }
        Log.info(msg);
        return 0;
    }
}
