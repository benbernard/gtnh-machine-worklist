package com.benbernard.machineworklist;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class WorklistPositionTest {

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void restartRestoresRecipeFiltersAndBothScrollPositions() throws Exception {
        Path file = temporary.getRoot()
            .toPath()
            .resolve("navigation/world.properties");
        WorklistPosition saved = new WorklistPosition();
        saved.group = 16;
        saved.snapshot = "chosen-recipe-snapshot";
        saved.recipe = "{\"recipe\":\"plate with configuration 6\"}";
        saved.tab = 2;
        saved.readyOnly = true;
        saved.showMissing = true;
        saved.scroll = 17;
        saved.missingScroll = 9;
        saved.keyboardRow = 19;
        assertTrue(saved.save(file));
        WorklistPosition reopened = WorklistPosition.load(file);
        assertNotNull(reopened);
        assertTrue(reopened.matches(16, saved.snapshot));
        assertEquals(saved.recipe, reopened.recipe);
        assertEquals(2, reopened.tab);
        assertTrue(reopened.readyOnly);
        assertTrue(reopened.showMissing);
        assertEquals(17, reopened.scroll);
        assertEquals(9, reopened.missingScroll);
        assertEquals(19, reopened.keyboardRow);
    }

    @Test
    public void groupReuseOrChangedRecipesCannotRestoreAnotherPlan() {
        WorklistPosition saved = new WorklistPosition();
        saved.group = 16;
        saved.snapshot = "original selection and quantities";
        assertFalse(saved.matches(17, saved.snapshot));
        assertFalse(saved.matches(16, "new recipes or target quantity"));
        assertFalse(new WorklistPosition().matches(0, ""));
    }

    @Test
    public void worldFilesAreDistinctAndDoNotExposeWorldNames() {
        Path directory = temporary.getRoot()
            .toPath();
        Path one = WorklistPosition.file(directory, "saves/World One");
        Path two = WorklistPosition.file(directory, "server.example:25565");
        assertNotEquals(one, two);
        assertEquals(one, WorklistPosition.file(directory, "saves/World One"));
        assertEquals(directory, one.getParent());
        assertFalse(
            one.getFileName()
                .toString()
                .contains("World"));
        assertFalse(
            two.getFileName()
                .toString()
                .contains("server"));
    }

    @Test
    public void missingMalformedAndFutureFilesFallBackWithoutThrowing() throws Exception {
        Path file = temporary.getRoot()
            .toPath()
            .resolve("state.properties");
        assertNull(WorklistPosition.load(file));
        Files.write(file, "version=1\ngroup=broken\nsnapshot=x".getBytes(StandardCharsets.UTF_8));
        assertNull(WorklistPosition.load(file));
        Files.write(file, "version=999\ngroup=16\nsnapshot=x".getBytes(StandardCharsets.UTF_8));
        assertNull(WorklistPosition.load(file));
    }

    @Test
    public void invalidCoordinatesAreBoundedBeforeUse() throws Exception {
        Path file = temporary.getRoot()
            .toPath()
            .resolve("state.properties");
        Files.write(
            file,
            "version=1\ngroup=16\nsnapshot=x\ntab=999\nscroll=-5\nmissingScroll=-2\nkeyboardRow=-9"
                .getBytes(StandardCharsets.UTF_8));
        WorklistPosition saved = WorklistPosition.load(file);
        assertNotNull(saved);
        assertEquals(2, saved.tab);
        assertEquals(0, saved.scroll);
        assertEquals(0, saved.missingScroll);
        assertEquals(-1, saved.keyboardRow);
    }

    @Test
    public void failedSaveDoesNotReplaceThePreviousRecord() throws Exception {
        Path directory = temporary.newFolder("blocked.properties")
            .toPath();
        Path previous = directory.resolve("preserved");
        Files.write(previous, new byte[] { 1, 2, 3 });
        WorklistPosition saved = new WorklistPosition();
        saved.snapshot = "x";
        assertFalse(saved.save(directory));
        assertArrayEquals(new byte[] { 1, 2, 3 }, Files.readAllBytes(previous));
    }
}
