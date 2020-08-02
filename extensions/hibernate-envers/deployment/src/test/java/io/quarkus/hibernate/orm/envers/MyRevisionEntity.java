package io.quarkus.hibernate.orm.envers;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@Entity
@RevisionEntity(MyRevisionListener.class)
public class MyRevisionEntity {

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
