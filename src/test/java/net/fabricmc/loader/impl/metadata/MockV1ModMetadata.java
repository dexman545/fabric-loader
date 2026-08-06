/*
 * Copyright 2016 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.loader.impl.metadata;

import java.util.Collection;
import java.util.Collections;

import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModEnvironment;

public class MockV1ModMetadata {
	public static LoaderModMetadata create(String id, Version version, Collection<ModDependency> dependencies) {
		return new V1ModMetadata(id, version, Collections.emptyList(), ModEnvironment.UNIVERSAL, Collections.emptyMap(), Collections.emptyList(),
				Collections.emptyList(), null, dependencies, false, id.toUpperCase(), null,
				Collections.emptyList(), Collections.emptyList(), null, Collections.emptyList(), null,
				Collections.emptyMap(), Collections.emptyMap());
	}

	public static LoaderModMetadata create(String id, Version version, Collection<ModDependency> dependencies, Collection<String> provides) {
		return new V1ModMetadata(id, version, provides, ModEnvironment.UNIVERSAL, Collections.emptyMap(), Collections.emptyList(),
				Collections.emptyList(), null, dependencies, false, id.toUpperCase(), null,
				Collections.emptyList(), Collections.emptyList(), null, Collections.emptyList(), null,
				Collections.emptyMap(), Collections.emptyMap());
	}
}
