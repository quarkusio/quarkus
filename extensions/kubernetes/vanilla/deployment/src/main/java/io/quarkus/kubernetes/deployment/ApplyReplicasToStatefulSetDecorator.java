package io.quarkus.kubernetes.deployment;

import io.dekorate.doc.Description;
import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpecFluent;

@Description("Apply the number of replicas to the StatefulSetSpec.")
public class ApplyReplicasToStatefulSetDecorator extends NamedResourceDecorator<StatefulSetSpecFluent> {
    private final int replicas;

    public ApplyReplicasToStatefulSetDecorator(int replicas) {
        this(ANY, replicas);
    }

    public ApplyReplicasToStatefulSetDecorator(String statefulSetName, int replicas) {
        super(statefulSetName);
        this.replicas = replicas;
    }

    @Override
    public void andThenVisit(StatefulSetSpecFluent statefulSetSpec, ObjectMeta resourceMeta) {
        if (this.replicas > 0) {
            statefulSetSpec.withReplicas(this.replicas);
        }

    }
}
