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
