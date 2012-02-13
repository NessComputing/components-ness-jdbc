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
import java.sql.Statement;
import java.util.Map;

import javax.sql.DataSource;


import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.nesscomputing.jdbc.wrappers.AbstractProxyInvocationHandler.MethodInterceptor;
import com.nesscomputing.logging.Log;

/**
 * Postgres specific interceptor that selects a schema before handing out a connection object.
 */
public class SchemaSelectorWrapper implements Function<DataSource, DataSource>
{
    private static final Log LOG = Log.findLog();

    private final Map<Method, MethodInterceptor<Connection>> interceptors = Maps.newHashMap();

    public SchemaSelectorWrapper(final String schemaName)
    {
        final MethodInterceptor<Connection> schemaChangeInterceptor = new SchemaSelectorInterceptor(schemaName);

        try {
            // Intercept all attempts to hand out a connection.
            // @{link DataSource#getConnection()}
            interceptors.put(DataSource.class.getMethod("getConnection", new Class<?>[0]), schemaChangeInterceptor);
            // @{link DataSource#getConnection(String username, String password)}
            interceptors.put(DataSource.class.getMethod("getConnection", new Class<?>[] {String.class, String.class}), schemaChangeInterceptor);
        }
        catch (NoSuchMethodException nsme) {
            throw new ExceptionInInitializerError(nsme);
        }
    }

    @Override
    public DataSource apply(final DataSource dataSource)
    {
        return (DataSource) Proxy.newProxyInstance(dataSource.getClass().getClassLoader(),
                                                   dataSource.getClass().getInterfaces(),
                                                   new SchemaSelectorInvocationHandler(dataSource));
    }

    class SchemaSelectorInvocationHandler extends AbstractProxyInvocationHandler
    {
        SchemaSelectorInvocationHandler(final DataSource dataSource)
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

    static class SchemaSelectorInterceptor implements MethodInterceptor<Connection>
    {
        private final String schemaStatement;

        SchemaSelectorInterceptor(final String schemaName)
        {
            this.schemaStatement = String.format("SET search_path TO '%s'", schemaName);
        }

        @Override
        public Connection intercept(final Connection connection) throws SQLException
        {
            Preconditions.checkState(connection != null, "connection is null!");

            final Statement stmt = connection.createStatement();
            try {
                stmt.execute(schemaStatement);
                LOG.trace(schemaStatement);
            } finally {
                stmt.close();
            }

            return connection;
        }
    }
}
