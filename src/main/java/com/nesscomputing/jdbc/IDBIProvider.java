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

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.collections.CollectionUtils;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.ResultSetMapperFactory;
import org.skife.jdbi.v2.TimingCollector;
import org.skife.jdbi.v2.logging.Log4JLog;
import org.skife.jdbi.v2.tweak.ArgumentFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;

/**
 * Bind an IDBI instance with a given annotation, using a DataSource with the same
 * annotation as the backing for it.
 */
public class IDBIProvider implements Provider<IDBI> {
    private Injector injector;
    private TimingCollector timingCollector = null;
    private Set<ArgumentFactory<?>> argumentFactories = null;
    private Set<ResultSetMapperFactory> resultSetMapperFactories = null;
    private final Annotation annotation;

    public IDBIProvider(Annotation annotation) {
        this.annotation = annotation;
    }

    @Inject
    void setInjector(final Injector injector) {
        this.injector = injector;
    }

    @Inject(optional=true)
    void setTimingCollector(final TimingCollector timingCollector)
    {
        this.timingCollector = timingCollector;
    }

    @Inject(optional=true)
    void setArgumentFactories(final Set<ArgumentFactory<?>> argumentFactories)
    {
        this.argumentFactories = argumentFactories;
    }

    @Inject(optional=true)
    void setResultSetMapperFactories(final Set<ResultSetMapperFactory> resultSetMapperFactories)
    {
        this.resultSetMapperFactories = resultSetMapperFactories;
    }

    @Override
    public IDBI get() {
        DBI dbi = new DBI(injector.getInstance(Key.get(DataSource.class, annotation)));
        dbi.setSQLLog(new Log4JLog());

        if (timingCollector != null) {
            dbi.setTimingCollector(timingCollector);
        }

        if (!CollectionUtils.isEmpty(argumentFactories)) {
            for (ArgumentFactory<?> argumentFactory : argumentFactories) {
                dbi.registerArgumentFactory(argumentFactory);
            }
        }

        if (!CollectionUtils.isEmpty(resultSetMapperFactories)) {
            for (ResultSetMapperFactory resultSetMapperFactory : resultSetMapperFactories) {
                dbi.registerMapper(resultSetMapperFactory);
            }
        }

        return dbi;
    }
}
