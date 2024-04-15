package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.List;

public class Frog {

    // check no cycle when field has the same type as the class which declares it
    private Frog partner;

    private List<Pond> ponds;

    public Frog getPartner() {
        return partner;
    }

    public void setPartner(Frog partner) {
        this.partner = partner;
    }

    public List<Pond> getPonds() {
        return ponds;
    }

    public void setPonds(List<Pond> ponds) {
        this.ponds = ponds;
    }

}
