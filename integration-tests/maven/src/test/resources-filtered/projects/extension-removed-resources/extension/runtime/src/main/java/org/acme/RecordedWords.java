package org.acme;

import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class RecordedWords {

    private List<String> words;
    private String classLoaderName;

    void setWords(List<String> words) {
        this.words = words;
    }

    public List<String> getWords() {
        return words;
    }

    void setClassLoaderName(String classLoaderName) {
        this.classLoaderName = classLoaderName;
    }

    public String getClassLoaderName() {
        return classLoaderName;
    }
}