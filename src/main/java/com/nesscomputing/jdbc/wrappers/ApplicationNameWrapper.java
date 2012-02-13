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
package com.nesscomputing.jdbc.wrappers;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;


import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import com.nesscomputing.jdbc.wrappers.AbstractProxyInvocationHandler.MethodInterceptor;
import com.nesscomputing.logging.Log;


/**
 * Set the application name in the database connection.
 */
@Singleton
public final class ApplicationNameWrapper implements Function<DataSource, DataSource>
{
    private static final Log LOG = Log.findLog();

    private final Map<Method, MethodInterceptor<Connection>> interceptors = Maps.newHashMap();
    private final MethodInterceptor<Connection> applicationNameInterceptor = new ApplicationNameInterceptor();

    public ApplicationNameWrapper() throws NoSuchMethodException
    {
        // Intercept all attempts to hand out a connection.
        // @{link DataSource#getConnection()}
        interceptors.put(DataSource.class.getMethod("getConnection", new Class<?>[0]), applicationNameInterceptor);
        // @{link DataSource#getConnection(String username, String password)}
        interceptors.put(DataSource.class.getMethod("getConnection", new Class<?>[] {String.class, String.class}), applicationNameInterceptor);
    }

    /**
     * Returns a proxy for a data source. All methods on the datasource object are passed through the proxy. In addition, the
     * DataSource also implements {@link java.io.Closeable}.
     */
    @Override
    public DataSource apply(final DataSource dataSource)
    {
        return (DataSource) Proxy.newProxyInstance(dataSource.getClass().getClassLoader(),
                                                   dataSource.getClass().getInterfaces(),
                                                   new ApplicationNameInvocationHandler(dataSource));
    }

    class ApplicationNameInvocationHandler extends AbstractProxyInvocationHandler
    {
        ApplicationNameInvocationHandler(final DataSource dataSource)
        {
            super(dataSource);
        }

        @Override
        protected Object intercept(final Method method, final Object object) throws Throwable
        {
            final MethodInterceptor<Connection> interceptor = interceptors.get(method);
            return (interceptor == null) ? object : interceptor.intercept(Connection.class.cast(object));
        }
    }

    /**
     * Intercept calls to getConnection() which returns a {@link java.sql.Connection} object. Use this connection
     * to set the name of the application on PostgreSQL 9.0 or better.
     */
    static class ApplicationNameInterceptor implements MethodInterceptor<Connection>
    {
        private static final AtomicLong ID = new AtomicLong();
        private static final AtomicBoolean BROKEN = new AtomicBoolean(false);

        @Override
        public Connection intercept(final Connection connection) throws SQLException
        {
            if (connection == null) {
                throw new IllegalStateException("null target in method interceptor");
            }

            final long id = ID.incrementAndGet();
            if (!BROKEN.get()) {
                try {
                    connection.setClientInfo("ApplicationName", Long.toString(id));
                }
                catch (Exception e) {
                    LOG.trace(e, "While setting application name");
                    BROKEN.set(true);
                }
            }
            return connection;
        }
    }
}
