package com.nesscomputing.jdbc;

import java.lang.annotation.Annotation;
import java.sql.Connection;

import javax.sql.DataSource;

import com.google.common.base.Function;
import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;

public final class NessSqlWrapperBinder
{
    private NessSqlWrapperBinder()
    {
    }

    public static LinkedBindingBuilder<Function<DataSource, DataSource>> bindDataSourceWrapper(final Binder binder, final Annotation annotation)
    {
        return  Multibinder.newSetBinder(binder, new TypeLiteral<Function<DataSource, DataSource>>() {}, annotation).addBinding();
    }

    public static LinkedBindingBuilder<Function<Connection, Connection>> bindConnectionWrapper(final Binder binder, final Annotation annotation)
    {
        return  Multibinder.newSetBinder(binder, new TypeLiteral<Function<Connection, Connection>>() {}, annotation).addBinding();
    }
}
