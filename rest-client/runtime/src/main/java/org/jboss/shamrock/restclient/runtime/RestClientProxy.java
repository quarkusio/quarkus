/**
 * Copyright 2018 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.shamrock.restclient.runtime;

import javax.ws.rs.client.Client;

/**
 * This interface is implemented by every proxy created by {@link io.smallrye.restclient.RestClientBuilderImpl}.
 *
 * @author Martin Kouba
 */
public interface RestClientProxy {


    /**
     *
     * @return the underlying {@link Client} instance
     */
    Client getClient();

}
