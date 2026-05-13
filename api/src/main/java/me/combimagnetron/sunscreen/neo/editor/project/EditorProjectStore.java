package me.combimagnetron.sunscreen.neo.editor.project;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.passport.util.math.Vec2i;
import me.combimagnetron.sunscreen.SunscreenLibrary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class EditorProjectStore {

    private EditorProjectStore() {
    }

    public static @NotNull Path save(@NotNull EditorProject project) throws IOException {
        Path folder = folder();
        Files.createDirectories(folder);
        Path path = folder.resolve(fileName(project.identifier()));
        try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(path))) {
            output.writeInt(2);
            output.writeUTF(project.identifier().string());
            output.writeUTF(project.displayName());
            Identifier selectedPage = project.selectedPage();
            output.writeUTF(selectedPage == null ? "" : selectedPage.string());
            output.writeInt(project.pages().size());
            for (EditorProject.PageData page : project.pages()) {
                writePage(output, page);
            }
        }
        return path;
    }

    public static @Nullable EditorProject load(@NotNull String key) throws IOException {
        if (key.isBlank()) return latest();
        Path path = folder().resolve(key.replace(':', '_').replace('/', '_') + ".sunscreen");
        if (!Files.exists(path)) return null;
        return read(path);
    }

    public static @NotNull List<String> projects() throws IOException {
        Path folder = folder();
        if (!Files.exists(folder)) return List.of();
        try (Stream<Path> paths = Files.list(folder)) {
            return paths.filter(p -> p.getFileName().toString().endsWith(".sunscreen"))
                    .map(p -> p.getFileName().toString().replace(".sunscreen", ""))
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
            Path path = paths.filter(p -> p.getFileName().toString().endsWith(".sunscreen"))
                    .max(Comparator.comparingLong(EditorProjectStore::modified))
                    .orElse(null);
            if (path == null) return null;
            return read(path);
        }
    }

    private static @NotNull EditorProject read(@NotNull Path path) throws IOException {
        try (DataInputStream input = new DataInputStream(Files.newInputStream(path))) {
            int version = input.readInt();
            Identifier identifier = identifier(input.readUTF());
            EditorProject project = new EditorProject(input.readUTF(), identifier);
            String selected = input.readUTF();
            if (!selected.isBlank()) project.selectedPage(identifier(selected));
            int pages = input.readInt();
            for (int i = 0; i < pages; i++) {
                project.page(readPage(input, version));
            }
            return project;
        }
    }

    private static void writePage(@NotNull DataOutputStream output, @NotNull EditorProject.PageData page) throws IOException {
        output.writeUTF(page.identifier().string());
        output.writeUTF(page.displayName());
        writeVec(output, page.position());
        writeVec(output, page.size());
        output.writeInt(page.elements().size());
        for (EditorProject.ElementData element : page.elements()) {
            writeElement(output, element);
        }
        writeView(output, page.view() == null ? EditorProject.PageViewData.center() : page.view());
    }

    private static @NotNull EditorProject.PageData readPage(@NotNull DataInputStream input, int version) throws IOException {
        Identifier identifier = identifier(input.readUTF());
        String displayName = input.readUTF();
        Vec2i position = readVec(input);
        Vec2i size = readVec(input);
        int amount = input.readInt();
        List<EditorProject.ElementData> elements = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            elements.add(readElement(input));
        }
        EditorProject.PageViewData view = version >= 2 ? readView(input) : EditorProject.PageViewData.center();
        return new EditorProject.PageData(identifier, displayName, position, size, elements, view);
    }

    private static void writeView(@NotNull DataOutputStream output, @NotNull EditorProject.PageViewData view) throws IOException {
        output.writeUTF(view.mode());
        output.writeInt(view.x());
        output.writeInt(view.y());
        output.writeInt(view.anchorX());
        output.writeInt(view.anchorY());
    }

    private static @NotNull EditorProject.PageViewData readView(@NotNull DataInputStream input) throws IOException {
        return new EditorProject.PageViewData(input.readUTF(), input.readInt(), input.readInt(), input.readInt(), input.readInt());
    }

    private static void writeElement(@NotNull DataOutputStream output, @NotNull EditorProject.ElementData element) throws IOException {
        output.writeUTF(element.identifier().string());
        output.writeUTF(element.type());
        writeVec(output, element.position());
        writeVec(output, element.size());
        output.writeBoolean(element.text() != null);
        if (element.text() != null) output.writeUTF(element.text());
        writeVec(output, element.textPosition());
        output.writeInt(element.textColor());
        output.writeInt(element.selected());
        output.writeInt(element.height());
        output.writeInt(element.entries().size());
        for (EditorProject.EntryData entry : element.entries()) {
            output.writeUTF(entry.text());
            output.writeInt(entry.color());
        }
        output.writeInt(element.pixelScale());
        writeVec(output, element.canvasSize());
        output.writeInt(element.pixels().length);
        for (int pixel : element.pixels()) output.writeInt(pixel);
    }

    private static @NotNull EditorProject.ElementData readElement(@NotNull DataInputStream input) throws IOException {
        Identifier identifier = identifier(input.readUTF());
        String type = input.readUTF();
        Vec2i position = readVec(input);
        Vec2i size = readVec(input);
        String text = input.readBoolean() ? input.readUTF() : null;
        Vec2i textPosition = readVec(input);
        int textColor = input.readInt();
        int selected = input.readInt();
        int height = input.readInt();
        int entryAmount = input.readInt();
        List<EditorProject.EntryData> entries = new ArrayList<>();
        for (int i = 0; i < entryAmount; i++) {
            entries.add(new EditorProject.EntryData(input.readUTF(), input.readInt()));
        }
        int pixelScale = input.readInt();
        Vec2i canvasSize = readVec(input);
        int[] pixels = new int[input.readInt()];
        for (int i = 0; i < pixels.length; i++) pixels[i] = input.readInt();
        return new EditorProject.ElementData(identifier, type, position, size, text, textPosition, textColor, selected, height, entries, pixelScale, canvasSize, pixels);
    }

    private static void writeVec(@NotNull DataOutputStream output, @NotNull Vec2i vec) throws IOException {
        output.writeInt(vec.x());
        output.writeInt(vec.y());
    }

    private static @NotNull Vec2i readVec(@NotNull DataInputStream input) throws IOException {
        return Vec2i.of(input.readInt(), input.readInt());
    }

    private static @NotNull Path folder() {
        return SunscreenLibrary.library().path().resolve(".projects");
    }

    private static @NotNull String fileName(@NotNull Identifier identifier) {
        return identifier.string().replace(':', '_').replace('/', '_') + ".sunscreen";
    }

    private static @NotNull Identifier identifier(@NotNull String value) {
        return value.contains(":") ? Identifier.split(value) : Identifier.of(value);
    }

    private static long modified(@NotNull Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ignored) {
            return 0;
        }
    }
}
