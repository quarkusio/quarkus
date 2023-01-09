package io.quarkus.hibernate.orm.envers;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@Entity
@RevisionEntity
public class MyListenerlessRevisionEntity {

    @Id
    @RevisionNumber
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "myRevisionEntitySeq")
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @RevisionTimestamp
    private Date revisionTimestamp;

    private String listenerValue;

    public Long getId() {
        return id;
    }

    public Date getRevisionTimestamp() {
        return revisionTimestamp;
    }

    public void setRevisionTimestamp(Date revisionTimestamp) {
        this.revisionTimestamp = revisionTimestamp;
    }

    public String getListenerValue() {
        return listenerValue;
    }

    public void setListenerValue(String listenerValue) {
        this.listenerValue = listenerValue;
    }

}
