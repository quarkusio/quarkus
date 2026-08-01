package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/nested-parameterized")
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

    public static class MapOfListsRequest {
        private Map<String, List<ServiceStatus>> mapOfLists;

        public Map<String, List<ServiceStatus>> getMapOfLists() {
            return mapOfLists;
        }

        public void setMapOfLists(Map<String, List<ServiceStatus>> mapOfLists) {
            this.mapOfLists = mapOfLists;
        }
    }

    public static class ListOfListsRequest {
        private List<List<ServiceStatus>> listOfLists;

        public List<List<ServiceStatus>> getListOfLists() {
            return listOfLists;
        }

        public void setListOfLists(List<List<ServiceStatus>> listOfLists) {
            this.listOfLists = listOfLists;
        }
    }

    public static class ListOfMapsRequest {
        private List<Map<String, ServiceStatus>> listOfMaps;

        public List<Map<String, ServiceStatus>> getListOfMaps() {
            return listOfMaps;
        }

        public void setListOfMaps(List<Map<String, ServiceStatus>> listOfMaps) {
            this.listOfMaps = listOfMaps;
        }
    }

    public static class OptionalListRequest {
        private Optional<List<ServiceStatus>> optionalList;

        public Optional<List<ServiceStatus>> getOptionalList() {
            return optionalList;
        }

        public void setOptionalList(Optional<List<ServiceStatus>> optionalList) {
            this.optionalList = optionalList;
        }
    }

    @POST
    @Path("/map-of-lists")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String mapOfLists(MapOfListsRequest request) {
        return request.getMapOfLists().get("group").get(0).getName();
    }

    @POST
    @Path("/list-of-lists")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String listOfLists(ListOfListsRequest request) {
        return request.getListOfLists().get(0).get(0).getName();
    }

    @POST
    @Path("/list-of-maps")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String listOfMaps(ListOfMapsRequest request) {
        return request.getListOfMaps().get(0).get("key").getName();
    }

    @POST
    @Path("/optional-list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String optionalList(OptionalListRequest request) {
        return request.getOptionalList().orElseThrow().get(0).getName();
    }
}
