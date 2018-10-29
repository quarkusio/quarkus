package org.jboss.protean.arc;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

public class WeldMain {

    static WeldContainer weld;

    static {
        // This is needed for graal ahead-of-time compilation
        weld = new Weld().initialize();
        // Dynamic class loading is not supported in substratevm
        // We need to generate/load all proxies eagerly
        weld.select(Generator.class).get();
        weld.select(GeneratedStringObserver.class).get();
        weld.select(GeneratedStringProducer.class).get();
    }

    public static void main(String[] args) {
        weld.select(Generator.class).get().run();
        weld.shutdown();
    }

}
