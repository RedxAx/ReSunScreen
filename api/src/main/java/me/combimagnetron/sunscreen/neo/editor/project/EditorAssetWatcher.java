package me.combimagnetron.sunscreen.neo.editor.project;

import me.combimagnetron.sunscreen.SunscreenLibrary;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public final class EditorAssetWatcher implements AutoCloseable {
    private final AtomicBoolean running = new AtomicBoolean();
    private WatchService watchService;

    public void start() throws IOException {
        if (!running.compareAndSet(false, true)) return;
        Path folder = EditorAssetStore.importFolder();
        Files.createDirectories(folder);
        importExisting(folder);
        watchService = folder.getFileSystem().newWatchService();
        folder.register(watchService, ENTRY_CREATE, ENTRY_MODIFY);
        Thread thread = new Thread(() -> watch(folder), "Sunscreen Asset Import Watcher");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void close() {
        running.set(false);
        try {
            if (watchService != null) watchService.close();
        } catch (IOException ignored) {
        }
    }

    private void watch(@NotNull Path folder) {
        while (running.get()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception ignored) {
                return;
            }
            key.pollEvents().forEach(event -> importPath(folder.resolve((Path) event.context())));
            key.reset();
        }
    }

    private void importExisting(@NotNull Path folder) throws IOException {
        try (var paths = Files.list(folder)) {
            paths.filter(Files::isRegularFile).forEach(this::importPath);
        }
    }

    private void importPath(@NotNull Path path) {
        if (!Files.isRegularFile(path)) return;
        try {
            EditorAssetStore.importAsset(path);
        } catch (IOException exception) {
            SunscreenLibrary.library().logger().warn("Failed to import dropped Sunscreen asset {}", path, exception);
        }
    }
}
