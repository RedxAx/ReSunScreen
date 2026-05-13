package me.combimagnetron.sunscreen.neo.editor.virtual;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.passport.util.math.Vec2i;
import me.combimagnetron.sunscreen.neo.cursor.CursorStyle;
import me.combimagnetron.sunscreen.neo.editor.EditorController;
import me.combimagnetron.sunscreen.neo.element.ModernElement;
import me.combimagnetron.sunscreen.neo.graphic.Canvas;
import me.combimagnetron.sunscreen.neo.graphic.color.Color;
import me.combimagnetron.sunscreen.neo.page.Page;
import me.combimagnetron.sunscreen.neo.render.engine.context.RenderContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class VirtualPage implements VirtualObject<Page> {
    private final Map<Identifier, VirtualElement<?>> children = new LinkedHashMap<>();
    private Vec2i size;
    private final EditorController controller;
    private final Identifier identifier;
    private final String displayName;
    private Identifier selected;
    private Canvas previous;
    private Canvas current;
    private boolean dirty = true;
    private DragMode dragMode;
    private DragMode hoverMode;
    private Vec2i dragStart;
    private Vec2i originalPos;
    private Vec2i originalSize;

    public VirtualPage(@NotNull Vec2i size, @NotNull Identifier identifier, @NotNull String displayName, @NotNull EditorController controller) {
        this.size = size;
        this.identifier = identifier;
        this.displayName = displayName;
        this.controller = controller;
    }

    public synchronized boolean dirty() {
        return dirty || !Objects.equals(previous, current);
    }

    public synchronized @NotNull Collection<VirtualElement<?>> children() {
        return children.values();
    }

    private @Nullable DragMode handleAt(@NotNull Vec2i cursor, @NotNull VirtualElement<?> element) {
        Vec2i pos = element.position();
        Vec2i size = element.size();
        int hitbox = 6;
        boolean inside = cursor.x() >= pos.x() && cursor.x() < pos.x() + size.x() && cursor.y() >= pos.y() && cursor.y() < pos.y() + size.y();
        boolean horizontal = cursor.x() >= pos.x() - hitbox && cursor.x() <= pos.x() + size.x() + hitbox;
        boolean vertical = cursor.y() >= pos.y() - hitbox && cursor.y() <= pos.y() + size.y() + hitbox;
        boolean left = Math.abs(cursor.x() - pos.x()) <= hitbox && vertical;
        boolean right = Math.abs(cursor.x() - (pos.x() + size.x())) <= hitbox && vertical;
        boolean top = Math.abs(cursor.y() - pos.y()) <= hitbox && horizontal;
        boolean bottom = Math.abs(cursor.y() - (pos.y() + size.y())) <= hitbox && horizontal;
        if (top && left) return DragMode.RESIZE_NW;
        if (top && right) return DragMode.RESIZE_NE;
        if (bottom && left) return DragMode.RESIZE_SW;
        if (bottom && right) return DragMode.RESIZE_SE;
        if (top) return DragMode.RESIZE_N;
        if (bottom) return DragMode.RESIZE_S;
        if (left) return DragMode.RESIZE_W;
        if (right) return DragMode.RESIZE_E;
        return inside ? DragMode.MOVE : null;
    }

    public synchronized @NotNull Canvas render(@NotNull RenderContext context) {
        previous = current;
        Canvas canvas = Canvas.empty(size);
        canvas.fill(Vec2i.zero(), size, Color.of(255, 255, 255));
        VirtualElement<?> selectedChild = selected();
        if (selectedChild != null) {
            Vec2i pos = selectedChild.position();
            Vec2i elementSize = selectedChild.size();
            Color border = Color.of(77, 155, 230);
            Color accent = Color.of(72, 74, 119);
            fill(canvas, pos.sub(1, 1), Vec2i.of(elementSize.x() + 2, 1), border);
            fill(canvas, pos.sub(1, 1), Vec2i.of(1, elementSize.y() + 2), border);
            fill(canvas, pos.add(elementSize.x(), -1), Vec2i.of(1, elementSize.y() + 2), border);
            fill(canvas, pos.add(-1, elementSize.y()), Vec2i.of(elementSize.x() + 2, 1), border);

            fill(canvas, pos.sub(2, 2), Vec2i.of(2, 2), accent);
            fill(canvas, pos.add(elementSize.x(), -2), Vec2i.of(2, 2), accent);
            fill(canvas, pos.add(-2, elementSize.y()), Vec2i.of(2, 2), accent);
            fill(canvas, pos.add(elementSize.x(), elementSize.y()), Vec2i.of(2, 2), accent);

            fill(canvas, pos.add(elementSize.x() / 2 - 1, -1), Vec2i.of(2, 1), accent);
            fill(canvas, pos.add(elementSize.x() / 2 - 1, elementSize.y()), Vec2i.of(2, 1), accent);
            fill(canvas, pos.add(-1, elementSize.y() / 2 - 1), Vec2i.of(1, 2), accent);
            fill(canvas, pos.add(elementSize.x(), elementSize.y() / 2 - 1), Vec2i.of(1, 2), accent);
            highlight(canvas, pos, elementSize, hoverMode, Color.of(240, 220, 90));
        }
        for (VirtualElement<?> child : children.values()) {
            canvas.place(child.render(child.size(), context), child.position());
        }
        current = canvas;
        dirty = false;
        return canvas;
    }

    private void fill(@NotNull Canvas canvas, @NotNull Vec2i position, @NotNull Vec2i amount, @NotNull Color color) {
        int x = Math.max(0, position.x());
        int y = Math.max(0, position.y());
        int maxX = Math.min(size.x(), position.x() + amount.x());
        int maxY = Math.min(size.y(), position.y() + amount.y());
        if (maxX <= x || maxY <= y) return;
        canvas.fill(Vec2i.of(x, y), Vec2i.of(maxX - x, maxY - y), color);
    }

    private void highlight(@NotNull Canvas canvas, @NotNull Vec2i position, @NotNull Vec2i amount, @Nullable DragMode mode, @NotNull Color color) {
        if (mode == null || mode == DragMode.MOVE) return;
        switch (mode) {
            case RESIZE_N -> fill(canvas, position.sub(1, 2), Vec2i.of(amount.x() + 2, 2), color);
            case RESIZE_S -> fill(canvas, position.add(-1, amount.y()), Vec2i.of(amount.x() + 2, 2), color);
            case RESIZE_W -> fill(canvas, position.sub(2, 1), Vec2i.of(2, amount.y() + 2), color);
            case RESIZE_E -> fill(canvas, position.add(amount.x(), -1), Vec2i.of(2, amount.y() + 2), color);
            case RESIZE_NW -> fill(canvas, position.sub(3, 3), Vec2i.of(4, 4), color);
            case RESIZE_NE -> fill(canvas, position.add(amount.x() - 1, -3), Vec2i.of(4, 4), color);
            case RESIZE_SW -> fill(canvas, position.add(-3, amount.y() - 1), Vec2i.of(4, 4), color);
            case RESIZE_SE -> fill(canvas, position.add(amount.x() - 1, amount.y() - 1), Vec2i.of(4, 4), color);
            case MOVE -> {
            }
        }
    }

    public synchronized @NotNull CursorStyle cursor(@NotNull Vec2i cursor) {
        VirtualElement<?> element = selected();
        hoverMode = element == null ? null : handleAt(cursor, element);
        if (hoverMode == null) return CursorStyle.click();
        return switch (hoverMode) {
            case MOVE -> CursorStyle.move();
            case RESIZE_N, RESIZE_S -> CursorStyle.resizeVertical();
            default -> CursorStyle.resizeHorizontal();
        };
    }

    public synchronized void clearHover() {
        hoverMode = null;
    }

    public synchronized void dragStart(@NotNull Vec2i cursor) {
        VirtualElement<?> element = selected();
        if (element == null) return;
        dragMode = handleAt(cursor, element);
        if (dragMode == null) return;
        dragStart = cursor;
        originalPos = element.position();
        originalSize = element.size();
    }

    public synchronized void dragUpdate(@NotNull Vec2i cursor) {
        VirtualElement<?> element = selected();
        if (element == null || dragStart == null || dragMode == null || originalPos == null || originalSize == null) return;
        Vec2i delta = cursor.sub(dragStart.x(), dragStart.y());
        Vec2i nextPosition = originalPos;
        Vec2i nextSize = originalSize;
        switch (dragMode) {
            case MOVE -> nextPosition = originalPos.add(delta.x(), delta.y());
            case RESIZE_N -> {
                int y = originalPos.y() + Math.min(delta.y(), originalSize.y() - 1);
                nextPosition = Vec2i.of(originalPos.x(), y);
                nextSize = Vec2i.of(originalSize.x(), originalSize.y() - Math.min(delta.y(), originalSize.y() - 1));
            }
            case RESIZE_S -> nextSize = Vec2i.of(originalSize.x(), originalSize.y() + delta.y());
            case RESIZE_E -> nextSize = Vec2i.of(originalSize.x() + delta.x(), originalSize.y());
            case RESIZE_W -> {
                int x = originalPos.x() + Math.min(delta.x(), originalSize.x() - 1);
                nextPosition = Vec2i.of(x, originalPos.y());
                nextSize = Vec2i.of(originalSize.x() - Math.min(delta.x(), originalSize.x() - 1), originalSize.y());
            }
            case RESIZE_NE -> {
                int y = originalPos.y() + Math.min(delta.y(), originalSize.y() - 1);
                nextPosition = Vec2i.of(originalPos.x(), y);
                nextSize = Vec2i.of(originalSize.x() + delta.x(), originalSize.y() - Math.min(delta.y(), originalSize.y() - 1));
            }
            case RESIZE_NW -> {
                int x = originalPos.x() + Math.min(delta.x(), originalSize.x() - 1);
                int y = originalPos.y() + Math.min(delta.y(), originalSize.y() - 1);
                nextPosition = Vec2i.of(x, y);
                nextSize = Vec2i.of(originalSize.x() - Math.min(delta.x(), originalSize.x() - 1), originalSize.y() - Math.min(delta.y(), originalSize.y() - 1));
            }
            case RESIZE_SE -> nextSize = originalSize.add(delta.x(), delta.y());
            case RESIZE_SW -> {
                int x = originalPos.x() + Math.min(delta.x(), originalSize.x() - 1);
                nextPosition = Vec2i.of(x, originalPos.y());
                nextSize = Vec2i.of(originalSize.x() - Math.min(delta.x(), originalSize.x() - 1), originalSize.y() + delta.y());
            }
        }
        nextSize = clampSize(nextSize);
        nextPosition = clampPosition(nextPosition, nextSize);
        element.position(nextPosition);
        element.size(nextSize);
        markDirty();
        Vec2i size = element.size();
        Vec2i position = element.position();
        controller.publishSelectionControls(size, position);
    }

    public synchronized void dragEnd() {
        dragStart = null;
        dragMode = null;
    }

    public synchronized @NotNull VirtualPage page(@NotNull VirtualElement<?> element) {
        Vec2i elementSize = clampSize(element.size());
        element.size(elementSize);
        element.position(clampPosition(element.position(), elementSize));
        children.put(element.identifier(), element);
        markDirty();
        return this;
    }

    public synchronized void select(@NotNull Identifier identifier) {
        this.selected = identifier;
    }

    public synchronized void clearSelection() {
        selected = null;
    }

    public synchronized void select(@NotNull Vec2i cursor) {
        VirtualElement<?> element = elementAt(cursor);
        selected = element == null ? null : element.identifier();
    }

    public synchronized void selectForDrag(@NotNull Vec2i cursor) {
        VirtualElement<?> selectedElement = selected();
        if (selectedElement != null && handleAt(cursor, selectedElement) != null) return;
        select(cursor);
    }

    public synchronized @Nullable <M extends ModernElement<M, Canvas>> VirtualElement<M> selected() {
        return (VirtualElement<M>) children.get(selected);
    }

    public synchronized @Nullable VirtualElement<?> element(@NotNull Identifier identifier) {
        return children.get(identifier);
    }

    public synchronized void remove(@NotNull Identifier identifier) {
        children.remove(identifier);
        if (identifier.equals(selected)) selected = null;
        markDirty();
    }

    public synchronized @Nullable VirtualElement<?> elementAt(@NotNull Vec2i cursor) {
        VirtualElement<?> result = null;
        for (VirtualElement<?> element : children.values()) {
            Vec2i pos = element.position();
            Vec2i s = element.size();
            if (cursor.x() >= pos.x() && cursor.x() < pos.x() + s.x()
                && cursor.y() >= pos.y() && cursor.y() < pos.y() + s.y()) {
                result = element;
            }
        }
        return result;
    }

    public synchronized void markDirty() {
        dirty = true;
    }

    private @NotNull Vec2i clampSize(@NotNull Vec2i size) {
        return Vec2i.of(Math.max(1, size.x()), Math.max(1, size.y()));
    }

    private @NotNull Vec2i clampPosition(@NotNull Vec2i position, @NotNull Vec2i elementSize) {
        int maxX = Math.max(0, size.x() - elementSize.x());
        int maxY = Math.max(0, size.y() - elementSize.y());
        return Vec2i.of(Math.clamp(position.x(), 0, maxX), Math.clamp(position.y(), 0, maxY));
    }

    public @NotNull Vec2i size() {
        return size;
    }

    public synchronized @NotNull VirtualPage size(@NotNull Vec2i size) {
        this.size = Vec2i.of(Math.max(16, size.x()), Math.max(16, size.y()));
        markDirty();
        return this;
    }

    public @NotNull Identifier identifier() {
        return identifier;
    }

    public @NotNull String displayName() {
        return displayName;
    }

    private enum DragMode {
        MOVE, RESIZE_N, RESIZE_S, RESIZE_E, RESIZE_W, RESIZE_NE, RESIZE_NW, RESIZE_SE, RESIZE_SW
    }

}
