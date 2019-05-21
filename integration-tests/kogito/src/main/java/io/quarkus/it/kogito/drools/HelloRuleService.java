package io.quarkus.it.kogito.drools;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.kie.kogito.rules.RuleUnit;
import org.kie.kogito.rules.impl.SessionMemory;

@ApplicationScoped
public class HelloRuleService {

    @Inject
    @Named("simpleKS")
    RuleUnit<SessionMemory> ruleUnit;

    public String run() {

        Result result = new Result();
        Person mark = new Person("Mark", 37);
        Person edson = new Person("Edson", 35);
        Person mario = new Person("Mario", 40);

        SessionMemory memory = new SessionMemory();
        memory.add(result);
        memory.add(mark);
        memory.add(edson);
        memory.add(mario);

        ruleUnit.evaluate(memory);

        return result.toString();
    }
}
