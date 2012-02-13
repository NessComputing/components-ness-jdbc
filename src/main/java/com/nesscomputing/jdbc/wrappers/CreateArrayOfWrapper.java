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

import java.sql.Connection;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;

/**
 * Provides a pooled C3P0 connection with the necessary magic to allow {@link java.sql.Connection#createArrayOf(String, Object[])} to succeed.
 */
@Singleton
public final class CreateArrayOfWrapper extends AbstractC3P0ConnectionWrapper
{
    public CreateArrayOfWrapper() throws NoSuchMethodException
    {
        super(ImmutableSet.of(Connection.class.getMethod("createArrayOf", new Class<?>[] { String.class, Object [].class})));
    }
}
