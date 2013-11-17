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

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.nesscomputing.jdbc.wrappers.ApplicationNameWrapper;
import com.nesscomputing.jdbc.wrappers.ClientInfoWrapper;
import com.nesscomputing.jdbc.wrappers.CloseableWrapper;
import com.nesscomputing.jdbc.wrappers.ConnectionWrapper;
import com.nesscomputing.jdbc.wrappers.CreateArrayOfWrapper;
import com.nesscomputing.logging.Log;

import org.skife.jdbi.v2.IDBI;

/**
 * Install this to provide a single pooled DataSource.  The database name is used to select configuration
 * options from the Config infrastructure.  In particular the following properties will be picked up, with
 * later opens overriding earlier options.<br/>
 * The JDBC URI is specified as
 * <code>
 * db.dbName.uri=jdbc:...
 * </code> in the configuration<br/>
 *
 * C3P0 options are in:
 * <pre>
 * c3p0.properties
 * db.defaults.pool
 * db.dbName.pool
 * </pre>
 * DB driver options are in:
 * <pre>
 * db.defaults.ds
 * db.dbName.ds
 * </pre>
 *
 * For example, you might specify <code>db.user.pool.maxPoolSize=50</code>
 * @author steven
 */
public class DatabaseModule extends AbstractModule
{
    private static final Set<String> BLACKLIST = ImmutableSet.of("defaults");

    private static final Log LOG = Log.findLog();

    private final String dbName;
    private final Annotation annotation;

    public DatabaseModule(@Nonnull final String dbName)
    {
        this(dbName, Names.named(dbName));
    }

    public DatabaseModule(@Nonnull final String dbName,
                          @Nonnull final Annotation annotation)
    {
        Preconditions.checkArgument(dbName != null, "the database name must not be null!");
        Preconditions.checkArgument(annotation != null, "the database annotation must not be null!");
        Preconditions.checkArgument(!BLACKLIST.contains(dbName), "%s is not a valid pool name", dbName);

        this.dbName = dbName;
        this.annotation = annotation;

        // Check for UTC early. UTC is necessary for all our services.
        DatabaseChecker.fixUTCTimezone();
    }

    @Override
    protected void configure()
    {
        LOG.info("DataSource [%s] is using pool configuration [%s]", annotation, dbName);
        bind(DataSource.class).annotatedWith(annotation).toProvider(new C3P0DataSourceProvider(dbName, annotation)).in(Scopes.SINGLETON);
        bind(IDBI.class).annotatedWith(annotation).toProvider(new IDBIProvider(annotation)).in(Scopes.SINGLETON);

        NessSqlWrapperBinder.bindDataSourceWrapper(binder(), annotation).to(ApplicationNameWrapper.class).in(Scopes.SINGLETON);
        NessSqlWrapperBinder.bindDataSourceWrapper(binder(), annotation).to(CloseableWrapper.class).in(Scopes.SINGLETON);
        NessSqlWrapperBinder.bindDataSourceWrapper(binder(), annotation).toInstance(new ConnectionWrapper(annotation));
        NessSqlWrapperBinder.bindConnectionWrapper(binder(), annotation).to(CreateArrayOfWrapper.class).in(Scopes.SINGLETON);
        NessSqlWrapperBinder.bindConnectionWrapper(binder(), annotation).to(ClientInfoWrapper.class).in(Scopes.SINGLETON);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(dbName, annotation);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DatabaseModule that = (DatabaseModule) obj;

        return Objects.equal(this.dbName, that.dbName)
                        && Objects.equal(this.annotation, that.annotation);
    }
}
