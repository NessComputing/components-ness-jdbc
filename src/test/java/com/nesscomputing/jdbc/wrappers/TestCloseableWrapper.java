package com.nesscomputing.jdbc.wrappers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.Closeable;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;

import com.nesscomputing.jdbc.wrappers.CloseableWrapper;

public class TestCloseableWrapper
{
    @Test
    public void testNonCloseable() throws Exception
    {
        final DataSource ds = mock(DataSource.class);
        final CloseableWrapper wrap = new CloseableWrapper();
        final DataSource wrappedDs = wrap.apply(ds);

        Assert.assertTrue(wrappedDs instanceof Closeable);

        ((Closeable) wrappedDs).close();
    }

    @Test
    public void testCloseable() throws Exception
    {
        final CloseableDatasource ds = mock(CloseableDatasource.class);
        final CloseableWrapper wrap = new CloseableWrapper();
        final DataSource wrappedDs = wrap.apply(ds);

        Assert.assertTrue(wrappedDs instanceof Closeable);

        ((Closeable) wrappedDs).close();

        verify(ds, only()).close();
        verify(ds, times(1)).close();
    }

    @Test
    public void testHasClose() throws Exception
    {
        final DatasourceWithClose ds = mock(DatasourceWithClose.class);
        final CloseableWrapper wrap = new CloseableWrapper();
        final DataSource wrappedDs = wrap.apply(ds);

        Assert.assertTrue(wrappedDs instanceof Closeable);

        ((Closeable) wrappedDs).close();

        verify(ds, only()).close();
        verify(ds, times(1)).close();
    }

    private static interface CloseableDatasource extends DataSource, Closeable
    {
    }

    private static interface DatasourceWithClose extends DataSource
    {
        void close();
    }
}
