/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.jboss.logmanager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class StringListHandler extends ExtHandler {
    private final List<String> messages = new ArrayList<String>();

    @Override
    protected void doPublish(final ExtLogRecord record) {
        super.doPublish(record);
        messages.add(record.getFormattedMessage());
    }

    public String getMessage(final int index) {
        return messages.get(index);
    }

    public int size() {
        return messages.size();
    }

    @Override
    public void close() throws SecurityException {
        super.close();
        messages.clear();
    }
}
