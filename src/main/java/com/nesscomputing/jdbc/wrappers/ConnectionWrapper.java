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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Maps;
import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.nesscomputing.jdbc.wrappers.AbstractProxyInvocationHandler.MethodInterceptor;


/**
 * Adds wrappers to all handed out connections. These in turn can be wrapped into other interceptors.
 */
@Singleton
public final class ConnectionWrapper implements Function<DataSource, DataSource>
{
    private final Map<Method, MethodInterceptor<Connection>> interceptors = Maps.newHashMap();
    private final Annotation annotation;

    private Function<Connection, Connection> connectionWrapper = Functions.identity();

    public ConnectionWrapper(final Annotation annotation)
    {
        this.annotation = annotation;

        // Intercept all attempts to hand out a connection.
        final MethodInterceptor<Connection> connectionInterceptor = new ConnectionInterceptor();
        try {
            // @{link DataSource#getConnection()}
            interceptors.put(DataSource.class.getMethod("getConnection", new Class<?>[0]), connectionInterceptor);
            // @{link DataSource#getConnection(String username, String password)}
            interceptors.put(DataSource.class.getMethod("getConnection", new Class<?>[] {String.class, String.class}), connectionInterceptor);
        }
        catch (NoSuchMethodException nsme) {
            throw new ExceptionInInitializerError(nsme);
        }
    }

    @Inject
    void getWrappers(final Injector injector)
    {
        final Binding<Set<Function<Connection, Connection>>> connectionBindings = injector.getExistingBinding(Key.get(new TypeLiteral<Set<Function<Connection, Connection>>> () { }, annotation));
        if (connectionBindings != null) {
            for (Function<Connection, Connection> fn : connectionBindings.getProvider().get()) {
                connectionWrapper = Functions.compose(connectionWrapper, fn);
            }
        }
    }

    /**
     * Returns a proxy for a data source. All methods on the datasource object are passed through the proxy. In addition, the
     * DataSource also implements {@link java.io.Closeable}.
     */
    @Override
    public DataSource apply(@Nullable final DataSource dataSource)
    {
        if (dataSource == null) {
            return null;
        }
        else {
            return (DataSource) Proxy.newProxyInstance(dataSource.getClass().getClassLoader(),
                                                       dataSource.getClass().getInterfaces(),
                                                       new ConnectionWrapperInvocationHandler(dataSource));
        }
    }

    class ConnectionWrapperInvocationHandler extends AbstractProxyInvocationHandler
    {
        ConnectionWrapperInvocationHandler(final DataSource dataSource)
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
     * Wrap a connection object by applying all the wrappers.
     */
    class ConnectionInterceptor implements MethodInterceptor<Connection>
    {
        @Override
        public Connection intercept(final Connection parent) throws SQLException
        {
            final Connection c = (Connection) parent;

            if (c == null) {
                throw new IllegalStateException("null target in method interceptor");
            }

            return connectionWrapper.apply(c);
        }
    }
}
