package org.jboss.shamrock.deployment;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.IndexView;

public class BeanArchiveProcessor implements ResourceProcessor {

    @Inject
    private BeanArchiveIndex beanArchiveIndex;

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        List<IndexView> indexes = new ArrayList<>();
        for(ApplicationArchive archive : archiveContext.getAllApplicationArchives()) {
            //TODO: this should not really be in core
            if(archive.getChildPath("META-INF/beans.xml") != null) {
                indexes.add(archive.getIndex());
            } else if(archive.getChildPath("WEB-INF/beans.xml") != null) {
                //TODO: how to handle WEB-INF?
                indexes.add(archive.getIndex());
            }
        }
        beanArchiveIndex.setIndex(CompositeIndex.create(indexes));
    }

    @Override
    public int getPriority() {
        //we want this to run early
        return -1000;
    }
}
