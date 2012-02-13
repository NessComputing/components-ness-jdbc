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

import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import com.google.inject.Singleton;


/**
 * Fakes "closeable" on all datasources and acts on it accordingly.
 */
@Singleton
public final class CloseableWrapper implements Function<DataSource, DataSource>
{
    private final Method closeableCloseMethod;

    public CloseableWrapper() throws NoSuchMethodException
    {
        this.closeableCloseMethod = Closeable.class.getMethod("close", new Class<?>[0]);
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

            Set<Class<?>> interfaces = Sets.newHashSet(dataSource.getClass().getInterfaces());
            interfaces.add(Closeable.class);
            return (DataSource) Proxy.newProxyInstance(dataSource.getClass().getClassLoader(),
                                                       interfaces.toArray(new Class<?>[interfaces.size()]),
                                                       new CloseableInvocationHandler(dataSource));
        }
    }

    class CloseableInvocationHandler extends AbstractProxyInvocationHandler
    {
        CloseableInvocationHandler(final DataSource dataSource)
        {
            super(dataSource);
        }

        @Override
        protected boolean ignore(final Method method)
        {
            return closeableCloseMethod.equals(method);
        }

        @Override
        protected MethodWrapper createMethodWrapper(final Class<?> objectClass, final Object proxy, final Method method, final Object[] args)
        {
            if (method.equals(closeableCloseMethod)) {
                try {
                    final Method datasourceMethod = objectClass.getMethod("close", new Class<?>[0]);
                    return new MethodHolder(datasourceMethod);
                }
                catch (NoSuchMethodException nsme) {
                    return IGNORE_THIS_METHOD;
                }
            }
            else {
                return super.createMethodWrapper(objectClass, proxy, method, args);
            }
        }
    }
}
