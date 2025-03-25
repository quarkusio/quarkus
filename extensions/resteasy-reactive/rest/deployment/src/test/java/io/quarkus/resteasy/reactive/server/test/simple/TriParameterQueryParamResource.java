package io.quarkus.resteasy.reactive.server.test.simple;

import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.Parameter;
import org.jboss.resteasy.reactive.RestQuery;

@Path("/tri-parameter-query")
public class TriParameterQueryParamResource {

    public static class ParameterContainer {
        @RestQuery
        Parameter<String> string;

        @RestQuery
        Parameter<Integer> integer;

        @RestQuery
        Parameter<List<String>> stringList;
        @RestQuery
        Parameter<List<Integer>> integerList;
        @RestQuery
        Parameter<String[]> stringArray;
        @RestQuery
        Parameter<Integer[]> integerArray;
        @RestQuery
        Parameter<int[]> intArray;
    }

    @Path("string")
    @GET
    public String string(@RestQuery Parameter<String> name) {
        return "absent: " + name.isAbsent()
                + ", cleared: " + name.isCleared()
                + ", set: " + name.isSet()
                + ", value: " + name.getValue();
    }

    @Path("container/string")
    @GET
    public String string(ParameterContainer param) {
        return "absent: " + param.string.isAbsent()
                + ", cleared: " + param.string.isCleared()
                + ", set: " + param.string.isSet()
                + ", value: " + param.string.getValue();
    }

    @Path("integer")
    @GET
    public String integer(@RestQuery Parameter<Integer> name) {
        return "absent: " + name.isAbsent()
                + ", cleared: " + name.isCleared()
                + ", set: " + name.isSet()
                + ", value: " + name.getValue();
    }

    @Path("container/integer")
    @GET
    public String integer(ParameterContainer param) {
        return "absent: " + param.integer.isAbsent()
                + ", cleared: " + param.integer.isCleared()
                + ", set: " + param.integer.isSet()
                + ", value: " + param.integer.getValue();
    }

    @Path("string-list")
    @GET
    public String stringList(@RestQuery Parameter<List<String>> name) {
        return "absent: " + name.isAbsent()
                + ", cleared: " + name.isCleared()
                + ", set: " + name.isSet()
                + ", value: " + name.getValue();
    }

    @Path("container/string-list")
    @GET
    public String stringList(ParameterContainer param) {
        return "absent: " + param.stringList.isAbsent()
                + ", cleared: " + param.stringList.isCleared()
                + ", set: " + param.stringList.isSet()
                + ", value: " + param.stringList.getValue();
    }

    @Path("integer-list")
    @GET
    public String integerList(@RestQuery Parameter<List<Integer>> name) {
        return "absent: " + name.isAbsent()
                + ", cleared: " + name.isCleared()
                + ", set: " + name.isSet()
                + ", value: " + name.getValue();
    }

    @Path("container/integer-list")
    @GET
    public String integerList(ParameterContainer param) {
        return "absent: " + param.integerList.isAbsent()
                + ", cleared: " + param.integerList.isCleared()
                + ", set: " + param.integerList.isSet()
                + ", value: " + param.integerList.getValue();
    }

    @Path("string-array")
    @GET
    public String stringArray(@RestQuery Parameter<String[]> name) {
        return "absent: " + name.isAbsent()
                + ", cleared: " + name.isCleared()
                + ", set: " + name.isSet()
                + ", value: " + Arrays.toString(name.getValue());
    }

    @Path("container/string-array")
    @GET
    public String stringArray(ParameterContainer param) {
        return "absent: " + param.stringArray.isAbsent()
                + ", cleared: " + param.stringArray.isCleared()
                + ", set: " + param.stringArray.isSet()
                + ", value: " + Arrays.toString(param.stringArray.getValue());
    }

    @Path("integer-array")
    @GET
    public String integerArray(@RestQuery Parameter<Integer[]> name) {
        return "absent: " + name.isAbsent()
                + ", cleared: " + name.isCleared()
                + ", set: " + name.isSet()
                + ", value: " + Arrays.toString(name.getValue());
    }

    @Path("container/integer-array")
    @GET
    public String integerArray(ParameterContainer param) {
        return "absent: " + param.integerArray.isAbsent()
                + ", cleared: " + param.integerArray.isCleared()
                + ", set: " + param.integerArray.isSet()
                + ", value: " + Arrays.toString(param.integerArray.getValue());
    }

    @Path("int-array")
    @GET
    public String intArray(@RestQuery Parameter<int[]> name) {
        return "absent: " + name.isAbsent()
                + ", cleared: " + name.isCleared()
                + ", set: " + name.isSet()
                + ", value: " + Arrays.toString(name.getValue());
    }

    @Path("container/int-array")
    @GET
    public String intArray(ParameterContainer param) {
        return "absent: " + param.intArray.isAbsent()
                + ", cleared: " + param.intArray.isCleared()
                + ", set: " + param.intArray.isSet()
                + ", value: " + Arrays.toString(param.intArray.getValue());
    }
}
