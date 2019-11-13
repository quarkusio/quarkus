package io.quarkus.test.restassured;

import java.lang.reflect.Type;

import javax.enterprise.inject.spi.CDI;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.path.json.mapper.factory.DefaultJackson2ObjectMapperFactory;

/**
 * A RestAssured Jackson2ObjectMapperFactory that pulls the ObjectMapper from CDI
 * and delegates to the parent if no CDI bean is found
 *
 * It is important that this class not be referenced directly anywhere in the code so the dependencies
 * on RestAssured and Jackson can remain optional
 */
public class QuarkusJackson2ObjectMapperFactory extends DefaultJackson2ObjectMapperFactory {

    @Override
    public ObjectMapper create(Type type, String s) {
        ObjectMapper result = CDI.current().select(ObjectMapper.class).get();
        if (result == null) {
            super.create(type, s);
        }
        return result;
    }
}
