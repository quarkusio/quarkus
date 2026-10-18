package io.quarkus.funqy.test;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class NestedCollection {
    private Map<String, Integer> intMap;
    private Map<Integer, String> intKeyMap;
    private Map<String, Simple> simpleMap;
    private List<String> stringList;
    private List<Simple> simpleList;
    private Set<Integer> intSet;
    private Set<Simple> simpleSet;

    public Set<Integer> getIntSet() {
        return intSet;
    }

    public void setIntSet(Set<Integer> intSet) {
        this.intSet = intSet;
    }

    public Set<Simple> getSimpleSet() {
        return simpleSet;
    }

    public void setSimpleSet(Set<Simple> simpleSet) {
        this.simpleSet = simpleSet;
    }

    public Map<Integer, String> getIntKeyMap() {
        return intKeyMap;
    }

    public void setIntKeyMap(Map<Integer, String> intKeyMap) {
        this.intKeyMap = intKeyMap;
    }

    public Map<String, Integer> getIntMap() {
        return intMap;
    }

    public void setIntMap(Map<String, Integer> intMap) {
        this.intMap = intMap;
    }

    public Map<String, Simple> getSimpleMap() {
        return simpleMap;
    }

    public void setSimpleMap(Map<String, Simple> simpleMap) {
        this.simpleMap = simpleMap;
    }

    public List<String> getStringList() {
        return stringList;
    }

    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
    }

    public List<Simple> getSimpleList() {
        return simpleList;
    }

    public void setSimpleList(List<Simple> simpleList) {
        this.simpleList = simpleList;
    }
}
