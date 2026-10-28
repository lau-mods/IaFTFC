package net.claustra01.iaftfc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;

final class DependencyMetadataTest {
    private static final Pattern RANGE = Pattern.compile("([\\[(])([^,\\])]*)(?:,([^\\])]*))?([\\])])");
    private static final List<Artifact> ARTIFACTS = List.of(
            new Artifact("tfc", "net/dries007/tfc/TerraFirmaCraft.class"),
            new Artifact("iceandfire", "com/iafenvoy/iceandfire/IceAndFire.class"),
            new Artifact("patchouli", "vazkii/patchouli/common/item/PatchouliItems.class"),
            new Artifact("uranus", "com/iafenvoy/uranus/Uranus.class"),
            new Artifact("jupiter", "com/iafenvoy/jupiter/Jupiter.class"));

    @Test
    void publishedArtifactsTargetTheSelectedPlatform() throws IOException {
        for (Artifact artifact : ARTIFACTS) {
            try (ZipFile jar = artifact.open()) {
                String metadata = metadata(jar);
                assertTrue(declaresMod(metadata, artifact.modId()));
                assertEquals(artifact.version(), declaredVersion(metadata, jar),
                        artifact.modId() + " artifact must match the adopted version");
                assertTrue(accepts(value(metadata, "loaderVersion"), "4"), artifact.modId() + " must accept FML 4");
                assertTrue(accepts(dependencyRange(metadata, "minecraft"), "1.21.1"),
                        artifact.modId() + " must accept Minecraft 1.21.1");
                assertTrue(accepts(dependencyRange(metadata, "neoforge"), "21.1.265"),
                        artifact.modId() + " must accept NeoForge 21.1.265");
                assertTrue(classMajor(jar, artifact.classEntry()) <= 65,
                        artifact.modId() + " must target Java 21 or earlier");
            }
        }

        assertTrue(accepts(dependencyRange(metadata(artifact("tfc")), "patchouli"),
                artifact("patchouli").version()));
        assertTrue(accepts(dependencyRange(metadata(artifact("iceandfire")), "uranus"),
                artifact("uranus").version()));
        assertTrue(accepts(dependencyRange(metadata(artifact("iceandfire")), "jupiter"),
                artifact("jupiter").version()));
    }

    @Test
    void requiredEmbeddedLibrariesArePresent() throws IOException {
        try (ZipFile uranus = artifact("uranus").open()) {
            assertNotNull(uranus.getEntry("META-INF/jars/architectury-neoforge-13.0.8.jar"));
        }
        try (ZipFile iceAndFire = artifact("iceandfire").open()) {
            assertNotNull(iceAndFire.getEntry("META-INF/jarjar/integration-neoforge-0.2.jar"));
        }
    }

    @Test
    void iaftfcRequiresTheTwoGameplayMods() throws IOException {
        String metadata;
        try (var input = DependencyMetadataTest.class.getResourceAsStream("/META-INF/neoforge.mods.toml")) {
            assertNotNull(input, "The built mod must contain NeoForge metadata");
            metadata = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertRequiredDependency(metadata, "tfc", "[4.2.12]");
        assertRequiredDependency(metadata, "iceandfire", "[2.1.4]");
    }

    private static void assertRequiredDependency(String metadata, String modId, String versionRange) {
        String block = dependencyBlock(metadata, modId);
        assertEquals("required", value(block, "type"));
        assertEquals(versionRange, value(block, "versionRange"));
        assertEquals("AFTER", value(block, "ordering"));
        assertEquals("BOTH", value(block, "side"));
    }

    private static Artifact artifact(String modId) {
        return ARTIFACTS.stream().filter(candidate -> candidate.modId().equals(modId)).findFirst().orElseThrow();
    }

    private static String metadata(Artifact artifact) throws IOException {
        try (ZipFile jar = artifact.open()) {
            return metadata(jar);
        }
    }

    private static String metadata(ZipFile jar) throws IOException {
        var entry = jar.getEntry("META-INF/neoforge.mods.toml");
        assertNotNull(entry, jar.getName() + " must contain NeoForge metadata");
        try (var input = jar.getInputStream(entry)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String value(String metadata, String key) {
        Matcher matcher = Pattern.compile("(?m)^\\s*" + key + "\\s*=\\s*\"([^\"]+)\"").matcher(metadata);
        assertTrue(matcher.find(), "Missing " + key);
        return matcher.group(1);
    }

    private static String dependencyRange(String metadata, String modId) {
        return value(dependencyBlock(metadata, modId), "versionRange");
    }

    private static String dependencyBlock(String metadata, String modId) {
        for (String block : metadata.split("\\[\\[dependencies\\.[^]]+]]")) {
            if (Pattern.compile("(?m)^\\s*modId\\s*=\\s*\"" + Pattern.quote(modId) + "\"")
                    .matcher(block).find()) {
                return block;
            }
        }
        throw new AssertionError("Missing dependency metadata for " + modId);
    }

    private static boolean declaresMod(String metadata, String modId) {
        String mods = metadata.split("\\[\\[dependencies\\.", 2)[0];
        return Pattern.compile("(?m)^\\s*modId\\s*=\\s*\"" + Pattern.quote(modId) + "\"")
                .matcher(mods).find();
    }

    private static String declaredVersion(String metadata, ZipFile jar) throws IOException {
        String version = value(metadata, "version");
        if (version.equals("${file.jarVersion}")) {
            try (var input = jar.getInputStream(jar.getEntry("META-INF/MANIFEST.MF"))) {
                version = new Manifest(input).getMainAttributes().getValue("Implementation-Version");
            }
        }
        assertNotNull(version, jar.getName() + " must declare its implementation version");
        return version;
    }

    private static int classMajor(ZipFile jar, String classEntry) throws IOException {
        var entry = jar.getEntry(classEntry);
        assertNotNull(entry, jar.getName() + " must contain " + classEntry);
        try (var input = new DataInputStream(jar.getInputStream(entry))) {
            assertEquals(0xCAFEBABE, input.readInt());
            input.readUnsignedShort();
            return input.readUnsignedShort();
        }
    }

    private static boolean accepts(String range, String version) {
        Matcher matcher = RANGE.matcher(range);
        while (matcher.find()) {
            String lower = matcher.group(2).trim();
            String upper = matcher.group(3);
            if (upper == null) {
                if (compare(version, lower) == 0) {
                    return true;
                }
                continue;
            }
            upper = upper.trim();
            boolean aboveLower = lower.isEmpty() || compare(version, lower) > 0
                    || matcher.group(1).equals("[") && compare(version, lower) == 0;
            boolean belowUpper = upper.isEmpty() || compare(version, upper) < 0
                    || matcher.group(4).equals("]") && compare(version, upper) == 0;
            if (aboveLower && belowUpper) {
                return true;
            }
        }
        return false;
    }

    private static int compare(String left, String right) {
        List<String> leftParts = List.of(left.split("[.-]"));
        List<String> rightParts = List.of(right.split("[.-]"));
        for (int index = 0; index < Math.max(leftParts.size(), rightParts.size()); index++) {
            String leftPart = index < leftParts.size() ? leftParts.get(index) : "0";
            String rightPart = index < rightParts.size() ? rightParts.get(index) : "0";
            int result = leftPart.chars().allMatch(Character::isDigit)
                    && rightPart.chars().allMatch(Character::isDigit)
                    ? Integer.compare(Integer.parseInt(leftPart), Integer.parseInt(rightPart))
                    : leftPart.compareToIgnoreCase(rightPart);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    private record Artifact(String modId, String classEntry) {
        String version() {
            String version = System.getProperty("dependency.version." + modId);
            assertNotNull(version, "Gradle must provide the adopted " + modId + " version");
            return version;
        }

        ZipFile open() throws IOException {
            String directory = System.getProperty("dependency.directory");
            assertNotNull(directory, "Gradle must provide the resolved dependency directory");
            try (var paths = Files.list(Path.of(directory))) {
                for (Path path : paths.filter(candidate -> candidate.toString().endsWith(".jar")).toList()) {
                    ZipFile jar = new ZipFile(path.toFile());
                    if (jar.getEntry("META-INF/neoforge.mods.toml") != null
                            && declaresMod(metadata(jar), modId)) {
                        return jar;
                    }
                    jar.close();
                }
            }
            throw new AssertionError("Resolved artifact not found for " + modId);
        }
    }
}
