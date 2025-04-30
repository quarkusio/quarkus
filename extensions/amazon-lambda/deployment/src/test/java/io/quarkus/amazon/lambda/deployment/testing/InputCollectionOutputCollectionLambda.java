package io.quarkus.amazon.lambda.deployment.testing;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.quarkus.amazon.lambda.deployment.testing.model.InputPerson;
import io.quarkus.amazon.lambda.deployment.testing.model.OutputPerson;

public class InputCollectionOutputCollectionLambda implements RequestHandler<List<InputPerson>, List<OutputPerson>> {

    @Override
    public List<OutputPerson> handleRequest(List<InputPerson> people, Context context) {

        List<OutputPerson> outputPeople = new ArrayList<>();
        people.forEach((person) -> {
            outputPeople.add(new OutputPerson(person.getName()));
        });

        return outputPeople;
    }
}
