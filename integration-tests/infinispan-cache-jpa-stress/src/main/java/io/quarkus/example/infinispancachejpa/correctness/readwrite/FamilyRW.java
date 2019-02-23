/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package io.quarkus.example.infinispancachejpa.correctness.readwrite;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import io.quarkus.example.infinispancachejpa.correctness.Family;
import io.quarkus.example.infinispancachejpa.correctness.Member;

@Entity
public final class FamilyRW implements Family {

    @Id
    @GeneratedValue
    private int id;
    private String name;
    private String secondName;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "family", orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<MemberRW> members;
    @Version
    private int version;

    public FamilyRW(String name) {
        this.name = name;
        this.secondName = null;
        this.members = new HashSet<>();
        this.id = 0;
        this.version = 0;
    }

    protected FamilyRW() {
        this.name = null;
        this.secondName = null;
        this.members = new HashSet<>();
        this.id = 0;
        this.version = 0;
    }

    public String getName() {
        return name;
    }

    public Set<? extends Member> getMembers() {
        return members;
    }

    public String getSecondName() {
        return secondName;
    }

    public void setSecondName(String secondName) {
        this.secondName = secondName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMembers(Set<MemberRW> members) {
        if (members == null) {
            this.members = new HashSet<>();
        } else {
            this.members = members;
        }
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public boolean addMember(Member member) {
        return members.add((MemberRW) member);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        FamilyRW family = (FamilyRW) o;

        // members must not be in the comparison since we would end up in infinite recursive call
        if (id != family.id)
            return false;
        if (version != family.version)
            return false;
        if (name != null ? !name.equals(family.name) : family.name != null)
            return false;
        if (secondName != null ? !secondName.equals(family.secondName) : family.secondName != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (secondName != null ? secondName.hashCode() : 0);
        result = 31 * result + id;
        result = 31 * result + version;
        return result;
    }

    @Override
    public String toString() {
        return "Family{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", secondName='" + secondName + '\'' +
                ", members=" + members +
                ", version=" + version +
                '}';
    }

}
