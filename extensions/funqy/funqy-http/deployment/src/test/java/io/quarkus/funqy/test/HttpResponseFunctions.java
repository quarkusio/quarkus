package io.quarkus.funqy.test;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.runtime.bindings.http.HttpResponse;

public class HttpResponseFunctions {

    public static final String NOT_FOUND_MESSAGE = "NOT FOUND";

    @Funq
    public HttpResponse voidFunqResponseSuccess(String name) {
        Greeting greeting = new Greeting();
        greeting.setMessage("Hi " + name);
        greeting.setName(name);
        return new HttpResponse(greeting);
    }

    @Funq
    public HttpResponse voidFunqResponseCreated(String name) {
        Greeting greeting = new Greeting();
        greeting.setMessage("Created user successfully");
        greeting.setName(name);
        return new HttpResponse(201, greeting);
    }

    @Funq
    public HttpResponse voidFunqResponseNotFound() {
        return new HttpResponse(404, NOT_FOUND_MESSAGE);
    }
}