package org.acme.libb;

public class ServiceB {
    public String process() {
        return "ServiceB:" + new HelperB().help();
    }
}
