package com.nesscomputing.jdbc;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.name.Names;
import com.nesscomputing.config.Config;
import com.nesscomputing.jdbc.DatabaseModule;

public class DatabaseModuleTest
{
    public static final String DATABASE_NAME = "demo";
    private Injector injector = null;

    @Before
    public void setUp()
    {
        final Config fakeConfig = Config.getFixedConfig("ness.db.defaults.pool.acquireIncrement", "5",
                                                        "ness.db.demo.uri", "jdbc:h2:mem:demo",
                                                        "ness.db.demo.pool.maxPoolSize", "50");

        Assert.assertNull(injector);
        injector = Guice.createInjector(Stage.PRODUCTION,
                                        new DatabaseModule(DATABASE_NAME),
                                        new Module() {
                                            @Override
                                            public void configure(final Binder binder) {
                                                binder.bind(Config.class).toInstance(fakeConfig);
                                                binder.requireExplicitBindings();
                                                binder.disableCircularProxies();
                                            }
                                        });
    }

    @After
    public void tearDown()
    {
        Assert.assertNotNull(injector);
        injector = null;
    }

    @Test
    public void testSimpleDb() throws Exception
    {
        final DataSource ds = injector.getInstance(Key.get(DataSource.class, Names.named(DATABASE_NAME)));

        // Yeah, these tests are not particularly good.  However c3p0 hasn't changed toString format in
        // forever, and the only bad thing that will happen is that the test starts failing.  And
        // it *does* test that the thing works...

        // Check that C3P0 picked up c3p0.properties
        assertTrue(ds.toString().contains("acquireRetryDelay -> 50"));

        // db.user.pool.maxPoolSize
        assertTrue(ds.toString().contains("maxPoolSize -> 50"));

        // db.defaults.pool.acquireIncrement
        assertTrue(ds.toString().contains("acquireIncrement -> 5"));

        Connection conn = null;

        // Just check that it works.  It will spin up a h2 memory db, and throw an exception if anything
        // goes wrong
        try {
            conn = ds.getConnection();
            Assert.assertNotNull(conn);
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    @Test
    public void testProxies() throws Exception
    {
        final DataSource dataSource = injector.getInstance(Key.get(DataSource.class, Names.named(DATABASE_NAME)));
        Assert.assertNotNull(dataSource);
        Assert.assertTrue(Proxy.isProxyClass(dataSource.getClass()));

        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            Assert.assertNotNull(connection);
            Assert.assertTrue(Proxy.isProxyClass(connection.getClass()));
        }
        finally {
            if (connection != null) {
                connection.close();
            }
        }
    }
}
