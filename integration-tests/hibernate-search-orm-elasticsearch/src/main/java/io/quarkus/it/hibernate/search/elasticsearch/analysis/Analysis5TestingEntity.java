package io.quarkus.it.hibernate.search.elasticsearch.analysis;

import javax.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class Analysis5TestingEntity extends AnalysisTestingEntityBase {
    public Analysis5TestingEntity() {
    }

    public Analysis5TestingEntity(String text) {
        super(text);
    }

    @FullTextField(analyzer = "index-level-analyzer-5")
    @Override
    public String getText() {
        return super.getText();
    }
}
