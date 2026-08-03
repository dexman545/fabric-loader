package net.fabricmc.loader.impl.discovery;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;
import net.fabricmc.loader.impl.metadata.MockV1ModMetadata;
import net.fabricmc.loader.impl.metadata.ModDependencyImpl;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

public class ModResolverTest {
	@Test
	public void testIncompatibleJiJNotLoaded() throws ModResolutionException, VersionParsingException {
		ModDependency depOnC = new ModDependencyImpl(ModDependency.Kind.DEPENDS, "c", Arrays.asList("*"));
		ModCandidateImpl aMod = createMod("a", "1.0.0", Arrays.asList(new MV("aa", "1.0.0", depOnC)), Collections.emptyList());
		ModCandidateImpl bMod = createMod("b", "1.0.0", Collections.emptyList(), Arrays.asList(new ModDependencyImpl(ModDependency.Kind.DEPENDS, "a", Arrays.asList("*"))));

		List<ModCandidateImpl> modCandidates = new ArrayList<>();
		discoverMod(modCandidates, aMod);
		discoverMod(modCandidates, bMod);

		Solution solution = solveMods(modCandidates);
		Assertions.assertFalse(solution.isModLoaded("aa"));
	}

	@Test
	public void testIncompatibleJiJFailure() throws VersionParsingException {
		ModDependency depOnC = new ModDependencyImpl(ModDependency.Kind.DEPENDS, "c", Arrays.asList("*"));
		ModCandidateImpl aMod = createMod("a", "1.0.0", Arrays.asList(new MV("aa", "1.0.0", depOnC)), Arrays.asList(new ModDependencyImpl(ModDependency.Kind.DEPENDS, "aa", Arrays.asList("*"))));
		ModCandidateImpl bMod = createMod("b", "1.0.0", Collections.emptyList(), Collections.emptyList());

		List<ModCandidateImpl> modCandidates = new ArrayList<>();
		discoverMod(modCandidates, aMod);
		discoverMod(modCandidates, bMod);

		Assertions.assertThrows(ModResolutionException.class, () -> solveMods(modCandidates));
	}

	@Test
	public void testMultiVersionJiJ() throws ModResolutionException, VersionParsingException {
		ModDependency depOnC = new ModDependencyImpl(ModDependency.Kind.DEPENDS, "c", Arrays.asList("*"));
		ModDependency breakOnC = new ModDependencyImpl(ModDependency.Kind.BREAKS, "c", Arrays.asList("*"));
		ModCandidateImpl aMod = createMod("a", "1.0.0",
				Arrays.asList(
						new MV("aa", "1.0.0", depOnC),
						new MV("aa", "2.0.0", breakOnC)
				), Arrays.asList(new ModDependencyImpl(ModDependency.Kind.DEPENDS, "aa", Arrays.asList("*"))));
		ModCandidateImpl bMod = createMod("b", "1.0.0", Collections.emptyList(), Arrays.asList(new ModDependencyImpl(ModDependency.Kind.DEPENDS, "a", Arrays.asList("*"))));

		List<ModCandidateImpl> modCandidates = new ArrayList<>();
		discoverMod(modCandidates, aMod);
		discoverMod(modCandidates, bMod);

		Solution solution = solveMods(modCandidates);
		Assertions.assertTrue(solution.isModLoaded("aa"));
	}

	@Test
	public void testDuplicateRootMods() {
		ModCandidateImpl aMod = createMod("a", "1.0.0", Collections.emptyList(), Collections.emptyList());
		ModCandidateImpl bMod = createMod("b", "1.0.0", Collections.emptyList(), Collections.emptyList());

		List<ModCandidateImpl> modCandidates = new ArrayList<>();
		discoverMod(modCandidates, aMod);
		discoverMod(modCandidates, aMod);
		discoverMod(modCandidates, bMod);

		Assertions.assertThrows(ModResolutionException.class, () -> solveMods(modCandidates));
	}

	@Test
	public void testRootOverrides() throws ModResolutionException, VersionParsingException {
		ModCandidateImpl aMod = createMod("a", "1.0.0",
				Arrays.asList(
						new MV("aa", "2.0.0")
				), Arrays.asList(new ModDependencyImpl(ModDependency.Kind.DEPENDS, "aa", Arrays.asList("*"))));
		ModCandidateImpl aMod2 = createMod("aa", "1.0.1", Collections.emptyList(), Collections.emptyList());

		List<ModCandidateImpl> modCandidates = new ArrayList<>();
		discoverMod(modCandidates, aMod);
		discoverMod(modCandidates, aMod2);

		Solution solution = solveMods(modCandidates);
		Assertions.assertTrue(solution.isModLoaded("aa"));
		Assertions.assertEquals(Version.parse("1.0.1"), solution.getVersion("aa"));
	}

	@Test
	public void testJiJOverrides() throws ModResolutionException, VersionParsingException {
		ModCandidateImpl aMod = createMod("a", "1.0.0",
				Arrays.asList(
						new MV("aa", "2.0.0")
				), Arrays.asList(new ModDependencyImpl(ModDependency.Kind.DEPENDS, "aa", Arrays.asList(">=2"))));
		ModCandidateImpl aMod2 = createMod("aa", "1.0.0", Collections.emptyList(), Collections.emptyList());

		List<ModCandidateImpl> modCandidates = new ArrayList<>();
		discoverMod(modCandidates, aMod);
		discoverMod(modCandidates, aMod2);

		Solution solution = solveMods(modCandidates);
		Assertions.assertTrue(solution.isModLoaded("aa"));
		Assertions.assertEquals(Version.parse("2.0.0"), solution.getVersion("aa"));
	}

	@Test
	public void testDuplicateRootModsOfDiffVers() throws ModResolutionException, VersionParsingException {
		ModCandidateImpl aMod = createMod("a", "1.0.0", Collections.emptyList(), Collections.emptyList());
		ModCandidateImpl aMod2 = createMod("a", "2.0.0", Collections.emptyList(), Collections.emptyList());
		ModCandidateImpl bMod = createMod("b", "1.0.0", Collections.emptyList(), Arrays.asList(new ModDependencyImpl(ModDependency.Kind.DEPENDS, "a", Arrays.asList("*"))));

		List<ModCandidateImpl> modCandidates = new ArrayList<>();
		discoverMod(modCandidates, aMod);
		discoverMod(modCandidates, aMod2);
		discoverMod(modCandidates, bMod);

		Solution solution = solveMods(modCandidates);
		Assertions.assertTrue(solution.isModLoaded("a"));
		Assertions.assertEquals(Version.parse("2.0.0"), solution.getVersion("a"));
	}

	@Test
	public void testDuplicateRootModsOfDiffVersLowestCompat() throws ModResolutionException, VersionParsingException {
		ModCandidateImpl aMod = createMod("a", "1.0.0", Collections.emptyList(), Collections.emptyList());
		ModCandidateImpl aMod2 = createMod("a", "2.0.0", Collections.emptyList(), Collections.emptyList());
		ModCandidateImpl bMod = createMod("b", "1.0.0", Collections.emptyList(),
				Arrays.asList(new ModDependencyImpl(ModDependency.Kind.BREAKS, "a", Arrays.asList(">=2.0.0"))));

		List<ModCandidateImpl> modCandidates = new ArrayList<>();
		discoverMod(modCandidates, aMod);
		discoverMod(modCandidates, aMod2);
		discoverMod(modCandidates, bMod);

		Solution solution = solveMods(modCandidates);
		//dumpModList(solution.candidates);
		Assertions.assertTrue(solution.isModLoaded("b"));
		Assertions.assertEquals(Version.parse("1.0.0"), solution.getVersion("b"));
	}

	@Test
	public void testCircular() throws ModResolutionException, VersionParsingException {
		ModCandidateImpl aMod = createMod("a", "1.0.0", Collections.emptyList(),
				Arrays.asList(new ModDependencyImpl(ModDependency.Kind.DEPENDS, "b", Arrays.asList("*"))));
		ModCandidateImpl bMod = createMod("b", "1.0.0", Collections.emptyList(),
				Arrays.asList(new ModDependencyImpl(ModDependency.Kind.DEPENDS, "c", Arrays.asList("*"))));
		ModCandidateImpl cMod = createMod("c", "1.0.0", Collections.emptyList(),
				Arrays.asList(new ModDependencyImpl(ModDependency.Kind.DEPENDS, "a", Arrays.asList("*"))));

		List<ModCandidateImpl> modCandidates = new ArrayList<>();
		discoverMod(modCandidates, aMod);
		discoverMod(modCandidates, bMod);
		discoverMod(modCandidates, cMod);

		Solution solution = solveMods(modCandidates);
		Assertions.assertTrue(solution.isModLoaded("b"));
	}

	private static Solution solveMods(List<ModCandidateImpl> modCandidates) throws ModResolutionException {
		modCandidates = ModResolver.resolve(modCandidates, EnvType.CLIENT, Collections.emptyMap());

		List<ModContainerImpl> modContainers = modCandidates.stream()
				.peek(modCandidate -> {
					if (!modCandidate.hasPath()) {
						modCandidate.setPaths(Arrays.asList(Paths.get(modCandidate.getId() + ".jar")));
					}
				})
				.map(ModContainerImpl::new)
				.collect(Collectors.toList());

		if (false) {
			System.out.println(modContainers);

			dumpModList(modCandidates);
		}

		return new Solution(modContainers, modCandidates);
	}

	private static ModCandidateImpl createMod(String id, String v, List<MV> mvs, Collection<ModDependency> dependencies) {
		return createMod(id, v, true, mvs, dependencies);
	}

	private static ModCandidateImpl createMod(String id, String v, boolean isRoot, List<MV> mvs, Collection<ModDependency> dependencies) {
		Collection<ModCandidateImpl> nested = mvs.stream().map((mv) ->
						ModCandidateImpl.createNested(mv.id+".jar", mv.id.hashCode(), createModMetadata(mv.id, mv.version, mv.dependencies),
								false, Collections.emptyList()))
				.collect(Collectors.toList());

		// If not root mod (eg in the mod folder, it does not have to load)
		ModCandidateImpl mod = ModCandidateImpl.createPlain(isRoot ? Collections.singletonList(Paths.get(id + ".jar")) : null, createModMetadata(id, v, dependencies), false, nested);
		for (ModCandidateImpl modCandidate : nested) {
			modCandidate.addParent(mod);
		}
		return mod;
	}

	private static LoaderModMetadata createModMetadata(String id, String v, Collection<ModDependency> deps) {
		try {
			return MockV1ModMetadata.create(id, Version.parse(v), deps);
		} catch (VersionParsingException e) {
			throw new RuntimeException(e);
		}
	}

	private static void discoverMod(List<ModCandidateImpl> mods, ModCandidateImpl modCandidate) {
		mods.add(modCandidate);
		for (ModCandidateImpl nestedMod : modCandidate.getNestedMods()) {
			discoverMod(mods, nestedMod);
		}
	}

	private static void dumpModList(List<ModCandidateImpl> mods) {
		StringBuilder modListText = new StringBuilder();

		boolean[] lastItemOfNestLevel = new boolean[mods.size()];
		List<ModCandidateImpl> topLevelMods = mods.stream()
				.filter(mod -> mod.getParentMods().isEmpty())
				.collect(Collectors.toList());
		int topLevelModsCount = topLevelMods.size();

		for (int i = 0; i < topLevelModsCount; i++) {
			boolean lastItem = i == topLevelModsCount - 1;

			if (lastItem) lastItemOfNestLevel[0] = true;

			dumpModList0(topLevelMods.get(i), modListText, 0, lastItemOfNestLevel);
		}

		int modsCount = mods.size();
		Log.info(LogCategory.GENERAL, "Loading %d mod%s:%n%s", modsCount, modsCount != 1 ? "s" : "", modListText);
	}

	private static void dumpModList0(ModCandidateImpl mod, StringBuilder log, int nestLevel, boolean[] lastItemOfNestLevel) {
		if (log.length() > 0) log.append('\n');

		for (int depth = 0; depth < nestLevel; depth++) {
			log.append(depth == 0 ? "\t" : lastItemOfNestLevel[depth] ? "     " : "   | ");
		}

		log.append(nestLevel == 0 ? "\t" : "  ");
		log.append(nestLevel == 0 ? "-" : lastItemOfNestLevel[nestLevel] ? " \\--" : " |--");
		log.append(' ');
		log.append(mod.getId());
		log.append(' ');
		log.append(mod.getVersion().getFriendlyString());

		List<ModCandidateImpl> nestedMods = new ArrayList<>(mod.getNestedMods());
		nestedMods.sort(Comparator.comparing(nestedMod -> nestedMod.getMetadata().getId()));

		if (!nestedMods.isEmpty()) {
			Iterator<ModCandidateImpl> iterator = nestedMods.iterator();
			ModCandidateImpl nestedMod;
			boolean lastItem;

			while (iterator.hasNext()) {
				nestedMod = iterator.next();
				lastItem = !iterator.hasNext();

				if (lastItem) lastItemOfNestLevel[nestLevel+1] = true;

				dumpModList0(nestedMod, log, nestLevel + 1, lastItemOfNestLevel);

				if (lastItem) lastItemOfNestLevel[nestLevel+1] = false;
			}
		}
	}

	private static class MV {
		String id;
		String version;
		Collection<ModDependency> dependencies;

		public MV(String id, String version) {
			this(id, version, Collections.emptyList());
		}

		public MV(String id, String version, ModDependency... dependencies) {
			this(id, version, Arrays.asList(dependencies));
		}

		public MV(String id, String version, Collection<ModDependency> dependencies) {
			this.id = id;
			this.version = version;
			this.dependencies = dependencies;
		}
	}

	private static class Solution {
		List<ModContainerImpl> containers;
		List<ModCandidateImpl> candidates;

		public Solution(List<ModContainerImpl> containers, List<ModCandidateImpl> candidates) {
			this.containers = containers;
			this.candidates = candidates;
		}

		boolean isModLoaded(String id) {
			for (ModContainerImpl container : containers) {
				if (container.getMetadata().getId().equals(id)) {
					return true;
				}
			}

			return false;
		}

		Version getVersion(String id) {
			for (ModContainerImpl container : containers) {
				if (container.getMetadata().getId().equals(id)) {
					return container.getMetadata().getVersion();
				}
			}

			return null;
		}
	}
}
