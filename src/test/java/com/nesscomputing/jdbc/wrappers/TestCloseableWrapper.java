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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.Closeable;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;

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
