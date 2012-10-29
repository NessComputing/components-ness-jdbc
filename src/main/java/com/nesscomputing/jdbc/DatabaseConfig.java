/**
 * Copyright (C) 2012 Ness Computing, Inc.
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
package com.nesscomputing.jdbc;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.DefaultNull;

public abstract class DatabaseConfig
{
    @Config({"ness.db.${dbName}.provider", "ness.db.defaults.provider"})
    @Default("C3P0")
    public DatabaseProviderType getProviderType() {
        return DatabaseProviderType.C3P0;
    }

    @Config({"ness.db.${dbName}.uri","ness.db.defaults.uri"})
    @DefaultNull
    public abstract URI getDbUri();

    @Config({"ness.db.${dbName}.schema-uri", "ness.db.defaults.schema-uri"})
    @Default("classpath:/")
    public URI getSchemaUri() {
        return URI.create("classpath:/");
    }

    @Config({"ness.db.${dbName}.schema", "ness.db.defaults.schema"})
    @Default("")
    public List<String> getSchemas() {
        return Collections.emptyList();
    }
}
