package org.jboss.shamrock.jpa;

import java.io.FileInputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.hibernate.hql.internal.ast.tree.Node;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.Ignore;
import org.junit.Test;

//not a real test, but you can use to to
//get all not implementation that need to be added for reflection
@Ignore
public class HqlNodeScannerTestCase {

    @Test
    public void generateAllNodes() throws Exception {
        URL url = Node.class.getResource("Node.class");
        String jar = url.getPath().substring(5, url.getPath().lastIndexOf("!"));
        System.out.println(jar);
        Indexer indexer = new Indexer();
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(jar))) {
            ZipEntry e = in.getNextEntry();
            while (e != null) {
                if (e.getName().endsWith(".class")) {
                    indexer.index(in);
                }
                e = in.getNextEntry();
            }
        }
        Index index = indexer.complete();
        for (ClassInfo i : index.getAllKnownSubclasses(DotName.createSimple(Node.class.getName()))) {
            System.out.println("simpleConstructor(" + i.name() + ".class);");
        }
    }

}
