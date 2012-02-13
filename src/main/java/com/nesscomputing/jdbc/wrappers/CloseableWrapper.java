package com.nesscomputing.jdbc.wrappers;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

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
    public DataSource apply(final DataSource dataSource)
    {
        Set<Class<?>> interfaces = Sets.newHashSet(dataSource.getClass().getInterfaces());
        interfaces.add(Closeable.class);
        return (DataSource) Proxy.newProxyInstance(dataSource.getClass().getClassLoader(),
                interfaces.toArray(new Class<?>[interfaces.size()]), new CloseableInvocationHandler(dataSource));
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
