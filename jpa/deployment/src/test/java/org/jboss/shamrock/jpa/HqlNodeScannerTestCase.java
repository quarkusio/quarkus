/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

//not a real test, but you can use it to
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
