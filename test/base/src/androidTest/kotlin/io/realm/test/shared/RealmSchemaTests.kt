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

package io.realm.test.shared

import io.realm.BaseRealm
import io.realm.Realm
import io.realm.DynamicRealm
import io.realm.schema.MutableRealmSchema
import io.realm.schema.MutableRealmProperty
import io.realm.schema.CollectionType
import io.realm.schema.ElementType
import io.realm.RealmConfiguration
import io.realm.entities.Sample
import io.realm.entities.schema.SchemaVariations
import io.realm.schema.CollectionType
import io.realm.schema.ElementType
import io.realm.test.platform.PlatformUtils
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

public class RealmSchemaTests {

    private lateinit var tmpDir: String
    private lateinit var realm: Realm

    @BeforeTest
    fun setup() {
        tmpDir = PlatformUtils.createTempDir()
        val configuration =
            RealmConfiguration.Builder(schema = setOf(SchemaVariations::class, Sample::class))
                .path("$tmpDir/default.realm").build()
        realm = Realm.open(configuration)
    }

    @AfterTest
    fun tearDown() {
        if (!realm.isClosed()) {
            realm.close()
        }
        PlatformUtils.deleteTempDir(tmpDir)
    }

    @Test
    fun realmClass() {
        val schema = realm.schema()

        val schemaVariationsName = "SchemaVariations"
        val schemaVariationsDescriptor = schema[schemaVariationsName]
        assertEquals(schemaVariationsName, schemaVariationsDescriptor.name)
        assertEquals("string", schemaVariationsDescriptor.primaryKey()?.name)

        val sampleName = "Sample"
        val sampleDescriptor = schema[sampleName]
        assertEquals(sampleName, sampleDescriptor.name)
        assertNull(sampleDescriptor.primaryKey())
    }

    @Test
    fun realmProperty() {
        val schema = realm.schema()

        val schemaVariationsName = "SchemaVariations"
        val schemaVariationsDescriptor = schema[schemaVariationsName]

        schemaVariationsDescriptor["string"]!!.run {
            assertEquals("string", name)
            type.run {
                assertEquals(CollectionType.NONE, collectionType)
                elementType.run {
                    assertEquals(ElementType.FieldType.STRING, fieldType)
                    assertFalse(nullable)
                }
                assertTrue(primaryKey)
                assertFalse(index)
                assertFalse(nullable)
            }
        }
        schemaVariationsDescriptor["nullableString"]!!.run {
            assertEquals("nullableString", name)
            type.run {
                assertEquals(CollectionType.NONE, collectionType)
                elementType.run {
                    assertEquals(ElementType.FieldType.STRING, fieldType)
                    assertTrue(nullable)
                }
                assertFalse(primaryKey)
                assertTrue(index)
                assertTrue(nullable)
            }
        }
        schemaVariationsDescriptor["stringList"]!!.run {
            assertEquals("stringList", name)
            type.run {
                assertEquals(CollectionType.LIST, collectionType)
                elementType.run {
                    assertEquals(ElementType.FieldType.STRING, fieldType)
                    assertFalse(nullable)
                }
                assertFalse(primaryKey)
                assertFalse(index)
                assertFalse(nullable)
            }
        }
    }

    @Test
    @Suppress("NestedBlockDepth")
    fun schema_optionCoverage() {
        // Class property options
        val primaryKeyOptionsClass = mutableSetOf(false, true)
        // TODO Embedded object is not supported yet
        // val embeddedOptions = setOf(false, true)

        // Property options
        val collectionTypeNullability =
            CollectionType.values().map { it to mutableSetOf(false, true) }.toMap().toMutableMap()
        val fieldTypes = ElementType.FieldType.values().toMutableSet()
        val indexOptions = mutableSetOf(false, true)
        val primaryKeyOptionProperty = mutableSetOf(false, true)

        val schema = realm.schema()

        // Verify class descriptors
        for (classDescriptor in schema.classes) {
            (classDescriptor.primaryKey() == null).let { primaryKeyOptionsClass.remove(it) }
        }
        assertEquals(2, schema.classes.size)

        // Verify properties of SchemaVariations
        val classDescriptor = schema["SchemaVariations"]
        assertEquals("SchemaVariations", classDescriptor.name)
        for (property in classDescriptor.properties) {
            property.run {
                type.run {
                    collectionType.let {
                        collectionTypeNullability.getValue(it).remove(elementType.nullable)
                    }
                    elementType.run {
                        fieldType.let { fieldTypes.remove(it) }
                        nullable.let {
                            assertEquals(it, property.nullable)
                        }
                    }
                }
                primaryKey.let { primaryKeyOptionProperty.remove(it) }
                index.let { indexOptions.remove(it) }
                if (primaryKey) {
                    assertEquals(classDescriptor.primaryKey(), this)
                }
            }
        }

        // Assert class options exhaustiveness
        assertTrue(
            primaryKeyOptionsClass.isEmpty(),
            "Primary key options not exhausted: $primaryKeyOptionsClass"
        )
        // Assert property options exhaustiveness
        assertTrue(collectionTypeNullability.none { (_, v) -> v.isNotEmpty() }, "Collection types not exhausted: $collectionTypeNullability")
        assertTrue(fieldTypes.isEmpty(), "Field types not exhausted: $fieldTypes")
        assertTrue(indexOptions.isEmpty(), "Index options not exhausted: $indexOptions")
        assertTrue(
            primaryKeyOptionProperty.isEmpty(),
            "Primary key options for properties not exhausted: $primaryKeyOptionProperty"
        )
    }

    @Test
    fun migration() {
        realm.close()
        val newConfiguration = RealmConfiguration.Builder(schema = setOf(io.realm.entities.migration.Child::class))
            .schemaVersion(1)
            .migration { oldRealm: DynamicRealm?, newRealm: DynamicRealm? -> // , oldVersion, newVersion ->
                println("Migration: ${oldRealm!!.version().version}->${newRealm!!.version().version}")
                println("Old schema: ${oldRealm!!.schema()}")
                println("New schema: ${newRealm!!.schema()}")
                val schema = MutableRealmSchema(oldRealm!!.schema())
                schema.classes.removeIf { it.name == "Parent" }
                schema.classes.first { it.name == "Child" }.let {
                    it.properties.add(MutableRealmProperty("SADF", CollectionType.NONE, ElementType.FieldType.STRING, false, false, false))
                }
                newRealm.schema(schema, newRealm.version().version)
            }
            .path("$tmpDir/default.realm").build()
        val newRealm = Realm.open(newConfiguration)
    }

    @Test
    fun migrationExamples() {
//        val oldSchema = realm.schema()
//        val schema: MutableRealmSchema = MutableRealmSchema(oldSchema)
//        // Add class
//        schema.classes.add(MutableRealmClass())
//        schema.classes.toSet()
//        // Remove class
//        schema.classes.removeIf { it.name == "ASDF" }
//        // Rename class (optimized as it shouldn't remove data)
//        schema.classes.find { it.name == "ASDF" }?.also { it.name = "asd" }
//        // Flip embedded
//        schema.classes.find { it.name == "ASDF"}?.also { it.embedded = !it.embedded }
//
//
//        // Add property
//        schema.classes.find { it.name == "SADF" }?.also {
//            it.properties.add(MutableRealmProperty())
//        }
//        // Remove property
//        schema.classes.find { it.name == "SADF" }?.also {
//            it.properties.removeIf { it.name == ""}
//        }
//        // Rename/Change property (optimized without removing it)
//        schema.classes.find { it.name == "SADF" }?.also {
//            it.properties.find{ it.name == ""}?.also { it.primaryKey = true}
//        }
//
//        val oldSchema = realm.schema()
//        val schema = MutableRealmSchema(oldSchema)
//        // Add class
//        schema.classes.add(MutableRealmClass())
//        // Remove class
//        schema.classes.removeIf { it.name == "ASDF" }
//        // Rename class
//        schema.classes.find { it.name == "ASDF" }?.also { it.name = "asd" }
//        // Flip embedded
//        schema.classes.find { it.name == "ASDF"}?.also { it.embedded = !it.embedded }
//
//
//        // Add property
//        schema.classes.find { it.name == "SADF" }?.also {
//            it.properties.add(MutableRealmProperty())
//        }
//        // Remove property
//        schema.classes.find { it.name == "SADF" }?.also {
//            it.properties.removeIf { it.name == ""}
//        }
//        // Rename/Change property
//        schema.classes.find { it.name == "SADF" }?.also {
//            it.properties.find{ it.name == ""}?.also { it.primaryKey = true}
//        }


    }
}
