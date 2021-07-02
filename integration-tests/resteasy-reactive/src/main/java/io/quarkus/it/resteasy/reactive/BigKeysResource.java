package io.quarkus.it.resteasy.reactive;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/bigkeys")
public class BigKeysResource {

    @POST
    public Payload post(Payload payload) {
        return payload;
    }

    public static class Payload {

        private Map<BigDecimal, String> bdMap;

        private Map<BigInteger, String> biMap;

        public Map<BigDecimal, String> getBdMap() {
            return bdMap;
        }

        public void setBdMap(Map<BigDecimal, String> bdMap) {
            this.bdMap = bdMap;
        }

        public Map<BigInteger, String> getBiMap() {
            return biMap;
        }

        public void setBiMap(Map<BigInteger, String> biMap) {
            this.biMap = biMap;
        }
    }
}
