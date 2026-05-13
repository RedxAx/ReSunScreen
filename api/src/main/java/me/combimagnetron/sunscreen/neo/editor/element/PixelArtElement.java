package me.combimagnetron.sunscreen.neo.editor.element;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.passport.util.math.Vec2i;
import me.combimagnetron.sunscreen.neo.element.GenericModernElement;
import me.combimagnetron.sunscreen.neo.graphic.Canvas;
import me.combimagnetron.sunscreen.neo.graphic.color.Color;
import me.combimagnetron.sunscreen.neo.property.Size;
import me.combimagnetron.sunscreen.neo.render.engine.context.RenderContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Arrays;

public class PixelArtElement extends GenericModernElement<PixelArtElement, Canvas> {
    private final ArrayDeque<Snapshot> undo = new ArrayDeque<>();
    private final ArrayDeque<Snapshot> redo = new ArrayDeque<>();
    private Canvas canvas;
    private Size size;
    private Snapshot change;
    private int pixelScale = 1;

    public PixelArtElement(@NotNull Identifier identifier, @NotNull Vec2i canvasSize) {
        super(identifier);
        this.canvas = Canvas.empty(canvasSize);
        this.size = Size.fixed(canvasSize);
    }

    public @NotNull PixelArtElement pixel(@NotNull Vec2i position, @NotNull Color color) {
        canvas.color(position, color);
        return this;
    }

    public @NotNull PixelArtElement erase(@NotNull Vec2i position) {
        canvas.erase(position);
        return this;
    }

    public @NotNull Color color(@NotNull Vec2i position) {
        return Color.rgba(canvas.bufferedColorSpace().at(position.x(), position.y()));
    }

    public @NotNull Vec2i canvasSize() {
        return canvas.size();
    }

    public int @NotNull [] pixels() {
        return canvas.bufferedColorSpace().buffer().clone();
    }

    public @NotNull PixelArtElement pixels(int @NotNull [] pixels) {
        canvas.bufferedColorSpace().pixels(pixels, 0, 0, canvas.size().x(), canvas.size().y());
        return this;
    }

    public @NotNull PixelArtElement canvasSize(@NotNull Vec2i size) {
        beginChange();
        resizeCanvas(size);
        endChange();
        return this;
    }

    public int pixelScale() {
        return pixelScale;
    }

    public @NotNull PixelArtElement pixelScale(int pixelScale) {
        beginChange();
        this.pixelScale = Math.clamp(pixelScale, 1, 16);
        adaptCanvas();
        endChange();
        return this;
    }

    public @NotNull PixelArtElement layout(@NotNull Size size, int pixelScale) {
        this.size = size;
        this.pixelScale = Math.clamp(pixelScale, 1, 16);
        return this;
    }

    public void beginChange() {
        if (change != null) return;
        change = snapshot();
    }

    public void endChange() {
        if (change == null) return;
        Snapshot current = snapshot();
        if (!change.same(current)) {
            undo.push(change);
            redo.clear();
        }
        change = null;
    }

    public boolean undo() {
        if (undo.isEmpty()) return false;
        redo.push(snapshot());
        restore(undo.pop());
        change = null;
        return true;
    }

    public boolean redo() {
        if (redo.isEmpty()) return false;
        undo.push(snapshot());
        restore(redo.pop());
        change = null;
        return true;
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    private @NotNull Snapshot snapshot() {
        return new Snapshot(canvas.size(), canvas.bufferedColorSpace().buffer().clone(), pixelScale);
    }

    private void restore(@NotNull Snapshot snapshot) {
        pixelScale = snapshot.pixelScale();
        Canvas restored = Canvas.empty(snapshot.size());
        restored.bufferedColorSpace().pixels(snapshot.pixels(), 0, 0, snapshot.size().x(), snapshot.size().y());
        canvas = restored;
    }

    private void adaptCanvas() {
        Vec2i target = size.value();
        resizeCanvas(Vec2i.of(Math.max(1, target.x() / pixelScale), Math.max(1, target.y() / pixelScale)));
    }

    private void resizeCanvas(@NotNull Vec2i size) {
        Vec2i safe = Vec2i.of(Math.clamp(size.x(), 1, 128), Math.clamp(size.y(), 1, 128));
        if (safe.equals(canvas.size())) return;
        Canvas next = Canvas.empty(safe);
        Vec2i copy = Vec2i.of(Math.min(safe.x(), canvas.size().x()), Math.min(safe.y(), canvas.size().y()));
        next.place(canvas.sub(Vec2i.zero(), copy), Vec2i.zero());
        canvas = next;
    }

    @Override
    public @NotNull PixelArtElement size(@NotNull Size size) {
        this.size = size;
        adaptCanvas();
        return this;
    }

    @Override
    public @NotNull Size size() {
        return size;
    }

    @Override
    public @NotNull Canvas render(@NotNull Size property, @Nullable RenderContext context) {
        Vec2i target = size.value();
        return new Canvas(canvas.bufferedColorSpace().resize(target));
    }

    private record Snapshot(@NotNull Vec2i size, int @NotNull [] pixels, int pixelScale) {
        private boolean same(@NotNull Snapshot other) {
            return size.equals(other.size()) && pixelScale == other.pixelScale() && Arrays.equals(pixels, other.pixels());
        }
    }
}
