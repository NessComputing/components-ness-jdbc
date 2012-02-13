package com.nesscomputing.jdbc;

import java.lang.annotation.Annotation;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.skife.jdbi.v2.IDBI;


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
    private final DatabaseConfig dbConfig;
    private final Properties dbProperties;
    private final Annotation annotation;

    public DatabaseModule(@Nonnull String dbName)
    {
        this(dbName, null, new Properties(), Names.named(dbName));
    }

    public DatabaseModule(@Nonnull String dbName,
                          @Nonnull final Annotation annotation)
    {
        this(dbName, null, new Properties(), annotation);
    }

    public DatabaseModule(@Nonnull String dbName,
                          @Nullable final DatabaseConfig dbConfig,
                          @Nonnull final Properties dbProperties,
                          @Nonnull Annotation annotation)
    {
        Preconditions.checkArgument(dbName != null, "the database name must not be null!");
        if (dbConfig != null) {
            Preconditions.checkArgument(dbConfig.getDbUri() != null, "the preset database URI must not be null!");
        }
        Preconditions.checkArgument(dbProperties != null, "the database properties must not be null!");
        Preconditions.checkArgument(annotation != null, "the database annotation must not be null!");
        Preconditions.checkArgument(!BLACKLIST.contains(dbName), "%s is not a valid pool name", dbName);

        this.dbName = dbName;
        this.dbConfig = dbConfig;
        this.dbProperties = dbProperties;
        this.annotation = annotation;

        // Check for UTC early. UTC is necessary for all our services.
        DatabaseChecker.fixUTCTimezone();
    }

    @Override
    protected void configure()
    {
        LOG.info("DataSource [%s] is using pool configuration [%s]", annotation, dbName);
        bind(DataSource.class).annotatedWith(annotation).toProvider(new C3P0DataSourceProvider(dbName, dbConfig, dbProperties, annotation)).in(Scopes.SINGLETON);
        bind(IDBI.class).annotatedWith(annotation).toProvider(new IDBIProvider(annotation)).in(Scopes.SINGLETON);

        NessSqlWrapperBinder.bindDataSourceWrapper(binder(), annotation).to(ApplicationNameWrapper.class).in(Scopes.SINGLETON);
        NessSqlWrapperBinder.bindDataSourceWrapper(binder(), annotation).to(CloseableWrapper.class).in(Scopes.SINGLETON);
        NessSqlWrapperBinder.bindDataSourceWrapper(binder(), annotation).toInstance(new ConnectionWrapper(annotation));
        NessSqlWrapperBinder.bindConnectionWrapper(binder(), annotation).to(CreateArrayOfWrapper.class).in(Scopes.SINGLETON);
        NessSqlWrapperBinder.bindConnectionWrapper(binder(), annotation).to(ClientInfoWrapper.class).in(Scopes.SINGLETON);
    }
}
