package io.quarkus.amazon.lambda.deployment.testing;

import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class PersonListLambda implements RequestHandler<List<Person>, String> {

    @Override
    public String handleRequest(List<Person> people, Context context) {
        return people.stream().map(Person::getName).collect(Collectors.joining(" "));
    }
}
