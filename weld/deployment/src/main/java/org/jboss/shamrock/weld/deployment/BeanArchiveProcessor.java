package org.jboss.shamrock.weld.deployment;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.IndexView;
import org.jboss.shamrock.deployment.ApplicationArchive;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;

public class BeanArchiveProcessor implements ResourceProcessor {

    @Inject
    private BeanArchiveIndex beanArchiveIndex;

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        List<IndexView> indexes = new ArrayList<>();
        for(ApplicationArchive archive : archiveContext.getAllApplicationArchives()) {
            if(archive.getChildPath("META-INF/beans.xml") != null) {
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
