package me.combimagnetron.sunscreen.neo.editor.project;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.sunscreen.SunscreenLibrary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class EditorProjectStore {

    private EditorProjectStore() {
    }

    public static @NotNull Path save(@NotNull EditorProject project) throws IOException {
        Path folder = folder();
        Files.createDirectories(folder);
        Path projectFolder = folder.resolve(ProjectFileHandler.fileName(project.identifier()));
        ProjectFileHandler.write(project, projectFolder);
        return projectFolder.resolve(ProjectFileHandler.PROJECT_FILE);
    }

    public static @Nullable EditorProject load(@NotNull String key) throws IOException {
        if (key.isBlank()) return latest();
        Path path = folder().resolve(key.replace(':', '_').replace('/', '_'));
        if (!Files.exists(path.resolve(ProjectFileHandler.PROJECT_FILE))) return null;
        return ProjectFileHandler.read(path);
    }

    public static @NotNull List<String> projects() throws IOException {
        Path folder = folder();
        if (!Files.exists(folder)) return List.of();
        try (Stream<Path> paths = Files.list(folder)) {
            return paths.filter(EditorProjectStore::projectFolder)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }

    public static @NotNull List<String> pages(@NotNull String key) throws IOException {
        EditorProject project = key.isBlank() ? latest() : load(key);
        if (project == null) return List.of();
        return project.pages().stream().map(page -> page.identifier().key().string()).toList();
    }

    public static @Nullable EditorProject latest() throws IOException {
        Path folder = folder();
        if (!Files.exists(folder)) return null;
        try (Stream<Path> paths = Files.list(folder)) {
            Path path = paths.filter(EditorProjectStore::projectFolder)
                    .max(Comparator.comparingLong(EditorProjectStore::modified))
                    .orElse(null);
            if (path == null) return null;
            return ProjectFileHandler.read(path);
        }
    }

    private static boolean projectFolder(@NotNull Path path) {
        return Files.isDirectory(path) && Files.exists(path.resolve(ProjectFileHandler.PROJECT_FILE));
    }

    private static @NotNull Path folder() {
        return SunscreenLibrary.library().path().resolve(".projects");
    }

    private static long modified(@NotNull Path path) {
        try {
            return Files.getLastModifiedTime(path.resolve(ProjectFileHandler.PROJECT_FILE)).toMillis();
        } catch (IOException ignored) {
            return 0;
        }
    }

    public static @NotNull Identifier identifier(@NotNull String value) {
        return ProjectFileHandler.identifier(value);
    }
}
