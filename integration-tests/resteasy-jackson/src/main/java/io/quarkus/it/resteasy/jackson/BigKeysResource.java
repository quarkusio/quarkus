package io.quarkus.it.resteasy.jackson;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/bigkeys")
public class BigKeysResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
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
