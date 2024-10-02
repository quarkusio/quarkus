package io.quarkus.amazon.lambda.deployment.testing;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.quarkus.amazon.lambda.deployment.testing.model.InputPerson;
import io.quarkus.amazon.lambda.deployment.testing.model.OutputPerson;

public abstract class AbstractInputCollectionOutputCollection implements RequestHandler<List<InputPerson>, List<OutputPerson>> {

    @Override
    public List<OutputPerson> handleRequest(List<InputPerson> inputPeronList, Context context) {
        List<OutputPerson> personList = new ArrayList<>();
        inputPeronList.forEach(person -> personList.add(new OutputPerson(person.getName())));
        return personList;
    }
}
