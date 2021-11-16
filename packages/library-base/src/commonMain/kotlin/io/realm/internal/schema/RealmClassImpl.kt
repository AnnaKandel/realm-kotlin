/*
 * Copyright 2021 Realm Inc.
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

package io.realm.internal.schema

import io.realm.internal.interop.Property
import io.realm.internal.interop.Table
import io.realm.schema.RealmClass
import io.realm.schema.RealmProperty

data class RealmClassImpl(
    // Optimization: Store the schema in the C-API alike structure directly from compiler plugin to
    // avoid unnecessary repeated initializations for realm_schema_new
    val cinteropTable: Table,
    val cinteropProperties: List<Property>
) : RealmClass {

    override val name: String = cinteropTable.name
    override val properties: Collection<RealmProperty> = cinteropProperties.map {
        RealmPropertyImpl.fromCoreProperty(it)
    }

    override fun get(key: String): RealmProperty? = properties.firstOrNull { it.name == key }
    override fun primaryKey(): RealmProperty? = properties.firstOrNull { it.primaryKey }

    // FIXME WIP Just to try out migration
    fun toCoreClass(): io.realm.internal.interop.Table = io.realm.internal.interop.Table(
        name,
        properties.firstOrNull { it.primaryKey }?.name,
        properties.size.toLong(),
        0,
        -1,
        0
    )
}
