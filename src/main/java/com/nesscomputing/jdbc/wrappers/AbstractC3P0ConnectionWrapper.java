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
import java.util.Set;


import com.google.common.base.Function;
import com.google.inject.Singleton;
import com.mchange.v2.c3p0.C3P0ProxyConnection;
import com.nesscomputing.jdbc.wrappers.AbstractProxyInvocationHandler.MethodWrapper;
import com.nesscomputing.logging.Log;

@Singleton
public abstract class AbstractC3P0ConnectionWrapper implements Function<Connection, Connection>
{
    private static final Log LOG = Log.findLog();

    private final Set<Method> interceptedMethods;

    public AbstractC3P0ConnectionWrapper(final Set<Method> interceptedMethods) throws NoSuchMethodException
    {
        this.interceptedMethods = interceptedMethods;
    }

    /**
     * Returns a proxy for a C3P0 connection. Intercepts {@link java.sql.Connection#createArrayOf(String, Object[])}.
     */
    @Override
    public Connection apply(final Connection connection)
    {
        return (Connection) Proxy.newProxyInstance(connection.getClass().getClassLoader(),
                                                   connection.getClass().getInterfaces(),
                                                   new C3P0ConnectionInvocationHandler(connection));
    }

    class C3P0ConnectionInvocationHandler extends AbstractProxyInvocationHandler
    {
        private C3P0ConnectionInvocationHandler(final Connection connection)
        {
            super(connection);
        }

        @Override
        protected MethodWrapper createMethodWrapper(final Class<?> objectClass, final Object proxy, final Method method, final Object[] args)
        {
            if (interceptedMethods.contains(method)) {
                if (proxy instanceof C3P0ProxyConnection) {
                    return new C3P0Wrapper(method);
                }
                else {
                    LOG.warn("No C3P0 pool found, falling back to connection method!");
                }
            }
            return super.createMethodWrapper(objectClass, proxy, method, args);
        }

        @Override
        protected boolean ignore(final Method method)
        {
            return interceptedMethods.contains(method);
        }

    }

    static class C3P0Wrapper implements MethodWrapper
    {
        private final Method method;

        C3P0Wrapper(final Method method)
        {
            this.method = method;
        }

        @Override
        public Object invoke(final Object proxy, final Object[] args) throws Exception
        {
            // If that cast fails, something is very, very wrong anyway...
            final C3P0ProxyConnection proxyConnection = C3P0ProxyConnection.class.cast(proxy);
            return proxyConnection.rawConnectionOperation(method, C3P0ProxyConnection.RAW_CONNECTION,
                                                          args == null ? new Object[0] : args); // Bug in C3P0 rawConnectionOperation work-around.
        }

        @Override
        public String toString()
        {
            return method.toString();
        }
    }
}
