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

package io.quarkus.example.datasource;

import java.sql.Connection;
import java.sql.Statement;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sql.DataSource;

@ApplicationScoped
public class DatasourceSetup {

    @Inject
    DataSource dataSource;

    @PostConstruct
    public void setup() throws Exception {

        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {
                try {
                    statement.execute("drop table a");
                    statement.execute("drop table tx");
                } catch (Exception ignored) {

                }
                statement.execute("create table a (b int)");
                statement.execute("create table tx (b int)");
            }
        }
    }

    public void doInit() {

    }
}
