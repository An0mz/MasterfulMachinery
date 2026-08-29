package io.ticticboom.mods.mm.setup;

import io.ticticboom.mods.mm.Ref;
import net.neoforged.fml.loading.FMLPaths;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BuiltInContentInstaller {

    private static final String RESOURCE_ROOT = "/builtin/";
    private static final String MARKER_NAME = ".builtin_installed";

    private static final List<String> BUILT_IN = List.of(
            "controllers/pulverizer.json",
            "ports/pulverizer_item.json",
            "ports/pulverizer_energy.json"
    );

    /**
     * Built-in machines are shipped as ordinary config files so a pack can read, edit or remove
     * them like any other. Each entry is written once and then recorded in the marker file, so a
     * pack that deletes one does not get it back on the next launch.
     */
    public static void install() {
        try {
            Path root = FMLPaths.CONFIGDIR.get().resolve("mm");
            Path marker = root.resolve(MARKER_NAME);
            Set<String> installed = Files.exists(marker) ? new HashSet<>(Files.readAllLines(marker)) : new HashSet<>();
            List<String> added = new ArrayList<>();

            for (String entry : BUILT_IN) {
                if (installed.contains(entry)) {
                    continue;
                }
                if (!copy(entry, root.resolve(entry))) {
                    continue;
                }
                added.add(entry);
            }

            if (!added.isEmpty()) {
                Files.createDirectories(root);
                Files.write(marker, added, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                Ref.LOG.info("Installed {} built-in Masterful Machinery config file(s): {}", added.size(), added);
            }
        } catch (Exception e) {
            Ref.LOG.error("Failed to install built-in Masterful Machinery content", e);
        }
    }

    private static boolean copy(String entry, Path target) {
        try (InputStream in = BuiltInContentInstaller.class.getResourceAsStream(RESOURCE_ROOT + entry)) {
            if (in == null) {
                Ref.LOG.error("Built-in config {} is missing from the mod jar and was skipped.", entry);
                return false;
            }
            if (Files.exists(target)) {
                return true;
            }
            Files.createDirectories(target.getParent());
            Files.copy(in, target);
            return true;
        } catch (Exception e) {
            Ref.LOG.error("Failed to write built-in config {}", entry, e);
            return false;
        }
    }
}
