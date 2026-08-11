package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/nested-parameterized-type")
public class NestedParameterizedTypeResource {

    public static class ServiceStatus {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * Each field nests a parameterized type inside another parameterized type, so the generated
     * deserializer must preserve both levels of generics. The endpoints below dereference the
     * innermost element as a {@code ServiceStatus}, which is where a lost type argument surfaces
     * as a {@code ClassCastException} on a {@code LinkedHashMap}.
     */
    public static class Request {
        private Map<String, List<ServiceStatus>> mapOfLists;
        private List<List<ServiceStatus>> listOfLists;
        private List<Map<String, ServiceStatus>> listOfMaps;
        private Optional<List<ServiceStatus>> optionalOfList;

        public Map<String, List<ServiceStatus>> getMapOfLists() {
            return mapOfLists;
        }

        public void setMapOfLists(Map<String, List<ServiceStatus>> mapOfLists) {
            this.mapOfLists = mapOfLists;
        }

        public List<List<ServiceStatus>> getListOfLists() {
            return listOfLists;
        }

        public void setListOfLists(List<List<ServiceStatus>> listOfLists) {
            this.listOfLists = listOfLists;
        }

        public List<Map<String, ServiceStatus>> getListOfMaps() {
            return listOfMaps;
        }

        public void setListOfMaps(List<Map<String, ServiceStatus>> listOfMaps) {
            this.listOfMaps = listOfMaps;
        }

        public Optional<List<ServiceStatus>> getOptionalOfList() {
            return optionalOfList;
        }

        public void setOptionalOfList(Optional<List<ServiceStatus>> optionalOfList) {
            this.optionalOfList = optionalOfList;
        }
    }

    @POST
    @Path("/map-of-lists")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String mapOfLists(Request request) {
        return request.getMapOfLists().get("group").get(0).getName();
    }

    @POST
    @Path("/list-of-lists")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String listOfLists(Request request) {
        return request.getListOfLists().get(0).get(0).getName();
    }

    @POST
    @Path("/list-of-maps")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String listOfMaps(Request request) {
        return request.getListOfMaps().get(0).get("key").getName();
    }

    @POST
    @Path("/optional-of-list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String optionalOfList(Request request) {
        return request.getOptionalOfList().orElseThrow().get(0).getName();
    }
}
