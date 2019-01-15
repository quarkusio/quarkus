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

package org.jboss.shamrock.example.panache;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

import org.jboss.panache.Model;
import org.jboss.panache.NotReallyJpa;
import org.jboss.panache.RxModel;

@NotReallyJpa
@Entity
public class RxPerson extends RxModel<RxPerson> {

    public String name;
//    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    public SequencedAddress address;
    public Status status;

    public void describeFully(StringBuilder sb) {
        sb.append( "Person with id=" ).append( id ).append( ", name='" ).append( name ).append("', status='").append(status).append( "', address { " );
//        address.describeFully( sb );
        sb.append( " }" );
    }
}
