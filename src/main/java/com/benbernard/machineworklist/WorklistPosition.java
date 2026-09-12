package com.benbernard.machineworklist;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;

/** Navigation values only: never retain a GUI, container, inventory or running request. */
final class WorklistPosition {

    int group;
    String snapshot = "";
    String recipe = "";
    int tab;
    int scroll;
    int missingScroll;
    int keyboardRow = -1;
    boolean readyOnly;
    boolean showMissing;

    boolean matches(int groupId, String identity) {
        return group == groupId && !snapshot.isEmpty() && snapshot.equals(identity);
    }

    static Path file(Path directory, String world) {
        return directory.resolve(UUID.nameUUIDFromBytes(world.getBytes(StandardCharsets.UTF_8)) + ".properties");
    }

    static WorklistPosition load(Path file) {
        if (!Files.isRegularFile(file)) return null;
        try (InputStream input = Files.newInputStream(file)) {
            Properties values = new Properties();
            values.load(input);
            if (!"1".equals(values.getProperty("version"))) return null;
            WorklistPosition result = new WorklistPosition();
            result.group = Integer.parseInt(values.getProperty("group"));
            result.snapshot = values.getProperty("snapshot", "");
            if (result.group < -1 || result.snapshot.isEmpty()) return null;
            result.recipe = values.getProperty("recipe", "");
            result.tab = Math.max(0, Math.min(2, Integer.parseInt(values.getProperty("tab", "0"))));
            result.scroll = Math.max(0, Integer.parseInt(values.getProperty("scroll", "0")));
            result.missingScroll = Math.max(0, Integer.parseInt(values.getProperty("missingScroll", "0")));
            result.keyboardRow = Math.max(-1, Integer.parseInt(values.getProperty("keyboardRow", "-1")));
            result.readyOnly = Boolean.parseBoolean(values.getProperty("readyOnly"));
            result.showMissing = Boolean.parseBoolean(values.getProperty("showMissing"));
            return result;
        } catch (IOException | IllegalArgumentException failure) {
            return null;
        }
    }

    boolean save(Path file) {
        Properties values = new Properties();
        values.setProperty("version", "1");
        values.setProperty("group", Integer.toString(group));
        values.setProperty("snapshot", snapshot);
        values.setProperty("recipe", recipe);
        values.setProperty("tab", Integer.toString(tab));
        values.setProperty("scroll", Integer.toString(scroll));
        values.setProperty("missingScroll", Integer.toString(missingScroll));
        values.setProperty("keyboardRow", Integer.toString(keyboardRow));
        values.setProperty("readyOnly", Boolean.toString(readyOnly));
        values.setProperty("showMissing", Boolean.toString(showMissing));
        try {
            Files.createDirectories(file.getParent());
            Path temporary = Files.createTempFile(file.getParent(), "navigation-", ".tmp");
            try {
                try (OutputStream output = Files.newOutputStream(temporary)) {
                    values.store(output, "Last worklist position; world/server identifier is hashed in the filename.");
                }
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
            return true;
        } catch (IOException failure) {
            return false;
        }
    }
}
