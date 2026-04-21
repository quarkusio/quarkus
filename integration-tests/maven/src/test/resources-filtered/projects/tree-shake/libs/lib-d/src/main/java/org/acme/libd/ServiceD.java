package org.acme.libd;

public class ServiceD {
    public String process() {
        return "ServiceD:" + new HelperD().help();
    }
}
