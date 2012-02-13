package com.nesscomputing.jdbc;

import java.lang.annotation.Annotation;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.configuration.tree.OverrideCombiner;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.mchange.v2.c3p0.DataSources;
import com.nesscomputing.config.Config;
import com.nesscomputing.config.util.ImmutableConfiguration;
import com.nesscomputing.lifecycle.LifecycleStage;
import com.nesscomputing.lifecycle.guice.AbstractLifecycleProvider;
import com.nesscomputing.lifecycle.guice.LifecycleAction;
import com.nesscomputing.logging.Log;

public class C3P0DataSourceProvider extends AbstractLifecycleProvider<DataSource> implements Provider<DataSource>
{
    public static final String PREFIX = "ness.db.";
    public static final String DEFAULTS_PREFIX = "ness.db.defaults";

    private static final Log LOG = Log.findLog();

    private Config config;

    private Function<DataSource, DataSource> dataSourceWrapper = Functions.identity();

    private final String dbName;
    private final DatabaseConfig dbConfig;
    private final Properties dbProperties;
    private final Annotation annotation;
    private final String propertiesPrefix;

    C3P0DataSourceProvider(final String dbName, @Nullable final DatabaseConfig dbConfig, final Properties dbProperties, final Annotation annotation)
    {
        this.dbName = dbName;
        this.dbConfig = dbConfig;
        this.dbProperties = dbProperties;
        this.annotation = annotation;
        this.propertiesPrefix = PREFIX + dbName;

        addAction(LifecycleStage.STOP_STAGE, new LifecycleAction<DataSource>() {
                @Override
                public void performAction(final DataSource dataSource)
                {
                    LOG.info("Destroying datasource %s", dbName);
                    try {
                        DataSources.destroy(dataSource);
                    }
                    catch (SQLException e) {
                        LOG.error(e, "Could not destroy pool %s", dbName);
                    }
                }
            });
    }

    @Inject(optional=true)
    void setUpConfig(final Config config)
    {
        this.config = config;
    }

    @Inject
    void getWrappers(final Injector injector)
    {
        final Binding<Set<Function<DataSource, DataSource>>> datasourceBindings = injector.getExistingBinding(Key.get(new TypeLiteral<Set<Function<DataSource, DataSource>>> () { }, annotation));
        if (datasourceBindings != null) {
            for (Function<DataSource, DataSource> fn : datasourceBindings.getProvider().get()) {
                dataSourceWrapper = Functions.compose(dataSourceWrapper, fn);
            }
        }
    }

    @Override
    public DataSource internalGet()
    {
        try {
            DataSource pool = createC3P0Pool();
            DatabaseChecker.checkPool(pool);
            return dataSourceWrapper.apply(pool);
        } catch (SQLException e) {
            throw new ProvisionException(String.format("Could not start DB pool %s", dbName), e);
        }
    }

    private DataSource createC3P0Pool() throws SQLException
    {
        Preconditions.checkState(config != null, "Config object was never injected!");

        final DatabaseConfig databaseConfig;

        if (dbConfig == null) {
            LOG.info("Creating datasource %s via C3P0", dbName);
            final DatabaseConfig dbConfig = config.getBean(DatabaseConfig.class, ImmutableMap.of("dbName", dbName));
            databaseConfig = dbConfig;
        }
        else {
            LOG.info("Using preset URI %s for %s via C3P0", dbConfig.getDbUri(), dbName);
            databaseConfig = dbConfig;
        }

        final Properties poolProps = getProperties("pool");
        LOG.info("Setting pool properties for %s to %s", dbName, poolProps);

        final Properties driverProps = getProperties("ds");
        LOG.info("Setting driver properties for %s to %s", dbName, driverProps);

        final DataSource dataSource = DataSources.pooledDataSource(DataSources.unpooledDataSource(databaseConfig.getDbUri().toString(), driverProps), poolProps);
        return dataSource;
    }

    private Properties getProperties(final String suffix)
    {
        if (config != null) {
            final CombinedConfiguration cc = new CombinedConfiguration(new OverrideCombiner());
            cc.addConfiguration(config.getConfiguration(propertiesPrefix + "." + suffix));
            cc.addConfiguration(config.getConfiguration(DEFAULTS_PREFIX + "." + suffix));

            if (dbProperties != null) {
                // Allow setting of internal defaults by using "ds.xxx" and "pool.xxx" if a properties
                // object is present.
                cc.addConfiguration(new ImmutableConfiguration(ConfigurationConverter.getConfiguration(dbProperties).subset(suffix)));
            }

            return ConfigurationConverter.getProperties(cc);
        }
        else {
            return new Properties();
        }
    }
}
