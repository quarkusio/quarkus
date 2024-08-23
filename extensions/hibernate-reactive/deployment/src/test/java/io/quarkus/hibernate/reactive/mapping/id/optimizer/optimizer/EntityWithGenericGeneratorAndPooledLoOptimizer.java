package io.quarkus.hibernate.reactive.mapping.id.optimizer.optimizer;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

@Entity
public class EntityWithGenericGeneratorAndPooledLoOptimizer {

    @Id
    @GeneratedValue(generator = "gen_gen_pooled_lo")
    @GenericGenerator(name = "gen_gen_pooled_lo", type = SequenceStyleGenerator.class, parameters = @Parameter(name = OptimizableGenerator.OPT_PARAM, value = "pooled-lo"))
    Long id;

    public EntityWithGenericGeneratorAndPooledLoOptimizer() {
    }

}
