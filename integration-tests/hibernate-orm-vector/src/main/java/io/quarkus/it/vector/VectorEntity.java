package io.quarkus.it.vector;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
public class VectorEntity {

    @Id
    private Long id;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 3)
    private float[] embedding;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 3)
    private double[] preciseEmbedding;

    public VectorEntity() {
    }

    public VectorEntity(Long id, float[] embedding) {
        this(id, embedding, null);
    }

    public VectorEntity(Long id, float[] embedding, double[] preciseEmbedding) {
        this.id = id;
        this.embedding = embedding;
        this.preciseEmbedding = preciseEmbedding;
    }

    public Long getId() {
        return id;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public double[] getPreciseEmbedding() {
        return preciseEmbedding;
    }
}
