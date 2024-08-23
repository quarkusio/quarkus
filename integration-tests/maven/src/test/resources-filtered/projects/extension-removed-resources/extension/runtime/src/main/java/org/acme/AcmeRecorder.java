package org.acme;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

import java.util.List;

@Recorder
public class AcmeRecorder {

    public void recordWords(BeanContainer beanContainer, List<String> words, String classLoaderName) {
        var bean = beanContainer.beanInstance(RecordedWords.class);
        bean.setWords(words);
        bean.setClassLoaderName(classLoaderName);
    }
}