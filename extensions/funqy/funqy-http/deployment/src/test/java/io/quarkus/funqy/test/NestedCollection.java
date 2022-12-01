package io.quarkus.funqy.test;

import java.util.List;
import java.util.Map;

public class NestedCollection {
    private Map<String, Integer> intMap;
    private Map<Integer, String> intKeyMap;
    private Map<String, Simple> simpleMap;
    private List<String> stringList;
    private List<Simple> simpleList;

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
