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
}
