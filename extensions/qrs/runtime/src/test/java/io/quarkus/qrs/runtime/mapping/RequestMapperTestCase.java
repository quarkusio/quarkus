package io.quarkus.qrs.runtime.mapping;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RequestMapperTestCase {

    @Test
    public void testPathMapper() {

        RequestMapper<String> mapper = mapper("/id", "/id/{param}", "/bar/{p1}/{p2}", "/bar/{p1}");
        mapper.dump();

        RequestMapper.RequestMatch<String> result = mapper.map("/bar/34/44");
        Assertions.assertEquals("/bar/{p1}/{p2}", result.value);
        Assertions.assertEquals("34", result.pathParamValues.get("p1"));
        Assertions.assertEquals("44", result.pathParamValues.get("p2"));
        Assertions.assertNull(mapper.map("/foo"));
        Assertions.assertEquals("/id", mapper.map("/id").value);
        result = mapper.map("/id/34");
        Assertions.assertEquals("/id/{param}", result.value);
        Assertions.assertEquals("34", result.pathParamValues.get("param"));
        result = mapper.map("/id/34/");
        Assertions.assertNull(result);
        result = mapper.map("/bar/34");
        Assertions.assertEquals("/bar/{p1}", result.value);
        Assertions.assertEquals("34", result.pathParamValues.get("p1"));

    }

    RequestMapper<String> mapper(String... vals) {
        List<RequestMapper.RequestPath<String>> list = new ArrayList<>();
        for (String i : vals) {
            list.add(new RequestMapper.RequestPath<>(new URITemplate(i), i));
        }
        return new RequestMapper<>(list);
    }

}
