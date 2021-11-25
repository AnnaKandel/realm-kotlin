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

package io.realm.internal.interop

/**
 * This interface is added by the compiler plugin to all [RealmObject] classes, it contains
 * internal properties of the model.
 *
 * This interface is not meant to be used externally (consider using [RealmObject] instead)
 */
@Suppress("VariableNaming")
interface RealmObjectInterop {
    // Names must match identifiers in compiler plugin (plugin-compiler/io.realm.compiler.Identifiers.kt)

    // Invariant: This is never null for managed objects!
    var `$realm$ObjectPointer`: NativePointer?
}
