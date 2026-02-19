package io.quarkus.resteasy.reactive.server.test.simple;

import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.Parameter;
import org.jboss.resteasy.reactive.RestForm;

@Path("/tri-parameter-form")
public class TriParameterFormParamResource {

    public static class ParameterContainer {
        @RestForm
        Parameter<String> string;

        @RestForm
        Parameter<Integer> integer;

        @RestForm
        Parameter<List<String>> stringList;
        @RestForm
        Parameter<List<Integer>> integerList;
        @RestForm
        Parameter<String[]> stringArray;
        @RestForm
        Parameter<Integer[]> integerArray;
        @RestForm
        Parameter<int[]> intArray;
    }

    @Path("string")
    @POST
    public String string(@RestForm Parameter<String> name) {
        return "absent: " + name.isAbsent()
                + ", cleared: " + name.isCleared()
                + ", set: " + name.isSet()
                + ", value: " + name.getValue();
    }

    @Path("container/string")
    @POST
    public String string(ParameterContainer param) {
        return "absent: " + param.string.isAbsent()
                + ", cleared: " + param.string.isCleared()
                + ", set: " + param.string.isSet()
                + ", value: " + param.string.getValue();
    }

    @Path("integer")
    @POST
    public String integer(@RestForm Parameter<Integer> name) {
        return "absent: " + name.isAbsent()
                + ", cleared: " + name.isCleared()
                + ", set: " + name.isSet()
                + ", value: " + name.getValue();
    }

    @Path("container/integer")
    @POST
    public String integer(ParameterContainer param) {
        return "absent: " + param.integer.isAbsent()
                + ", cleared: " + param.integer.isCleared()
                + ", set: " + param.integer.isSet()
                + ", value: " + param.integer.getValue();
    }

    @Path("string-list")
    @POST
    public String stringList(@RestForm Parameter<List<String>> name) {
        return "absent: " + name.isAbsent()
                + ", cleared: " + name.isCleared()
                + ", set: " + name.isSet()
                + ", value: " + name.getValue();
    }

    @Path("container/string-list")
    @POST
    public String stringList(ParameterContainer param) {
        return "absent: " + param.stringList.isAbsent()
                + ", cleared: " + param.stringList.isCleared()
                + ", set: " + param.stringList.isSet()
                + ", value: " + param.stringList.getValue();
    }

    @Path("integer-list")
    @POST
    public String integerList(@RestForm Parameter<List<Integer>> name) {
        return "absent: " + name.isAbsent()
                + ", cleared: " + name.isCleared()
                + ", set: " + name.isSet()
                + ", value: " + name.getValue();
    }

    @Path("container/integer-list")
    @POST
    public String integerList(ParameterContainer param) {
        return "absent: " + param.integerList.isAbsent()
                + ", cleared: " + param.integerList.isCleared()
                + ", set: " + param.integerList.isSet()
                + ", value: " + param.integerList.getValue();
    }

    @Path("string-array")
    @POST
    public String stringArray(@RestForm Parameter<String[]> name) {
        return "absent: " + name.isAbsent()
                + ", cleared: " + name.isCleared()
                + ", set: " + name.isSet()
                + ", value: " + Arrays.toString(name.getValue());
    }

    @Path("container/string-array")
    @POST
    public String stringArray(ParameterContainer param) {
        return "absent: " + param.stringArray.isAbsent()
                + ", cleared: " + param.stringArray.isCleared()
                + ", set: " + param.stringArray.isSet()
                + ", value: " + Arrays.toString(param.stringArray.getValue());
    }

    @Path("integer-array")
    @POST
    public String integerArray(@RestForm Parameter<Integer[]> name) {
        return "absent: " + name.isAbsent()
                + ", cleared: " + name.isCleared()
                + ", set: " + name.isSet()
                + ", value: " + Arrays.toString(name.getValue());
    }

    @Path("container/integer-array")
    @POST
    public String integerArray(ParameterContainer param) {
        return "absent: " + param.integerArray.isAbsent()
                + ", cleared: " + param.integerArray.isCleared()
                + ", set: " + param.integerArray.isSet()
                + ", value: " + Arrays.toString(param.integerArray.getValue());
    }

    @Path("int-array")
    @POST
    public String intArray(@RestForm Parameter<int[]> name) {
        return "absent: " + name.isAbsent()
                + ", cleared: " + name.isCleared()
                + ", set: " + name.isSet()
                + ", value: " + Arrays.toString(name.getValue());
    }

    @Path("container/int-array")
    @POST
    public String intArray(ParameterContainer param) {
        return "absent: " + param.intArray.isAbsent()
                + ", cleared: " + param.intArray.isCleared()
                + ", set: " + param.intArray.isSet()
                + ", value: " + Arrays.toString(param.intArray.getValue());
    }
}
