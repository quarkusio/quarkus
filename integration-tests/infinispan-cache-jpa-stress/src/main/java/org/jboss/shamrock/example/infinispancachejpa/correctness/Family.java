package io.quarkus.example.infinispancachejpa.correctness;

import java.util.Set;

public interface Family {

    int getId();

    String getName();

    void setName(String name);

    Set<? extends Member> getMembers();

    boolean addMember(Member member);

}
