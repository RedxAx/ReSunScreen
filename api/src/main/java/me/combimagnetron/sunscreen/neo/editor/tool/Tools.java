package me.combimagnetron.sunscreen.neo.editor.tool;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.passport.util.math.Vec2i;
import me.combimagnetron.sunscreen.neo.cursor.CursorStyle;
import me.combimagnetron.sunscreen.neo.editor.element.PixelArtElement;
import me.combimagnetron.sunscreen.neo.editor.virtual.VirtualElement;
import me.combimagnetron.sunscreen.neo.editor.virtual.VirtualPage;
import me.combimagnetron.sunscreen.neo.editor.virtual.argument.ElementConstructionProvider;
import me.combimagnetron.sunscreen.neo.graphic.Canvas;
import me.combimagnetron.sunscreen.neo.graphic.color.Color;
import me.combimagnetron.sunscreen.neo.graphic.text.Text;
import me.combimagnetron.sunscreen.neo.render.engine.context.RenderContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class Tools {
    private static final Tool SELECT = new SelectTool();
    private static final Tool PAGE = new PageTool();
    private static final Tool PLACE_ELEMENT = new PlaceElementTool();
    private static final Tool PIXEL_PENCIL = new PixelTool("pixel_pencil", "Pixel Pencil", PixelAction.PENCIL);
    private static final Tool PIXEL_ERASER = new PixelTool("pixel_eraser", "Pixel Eraser", PixelAction.ERASER);
    private static final Tool PIXEL_FILL = new PixelTool("pixel_fill", "Pixel Fill", PixelAction.FILL);

    private Tools() {
    }

    public static @NotNull Tool select() {
        return SELECT;
    }

    public static @NotNull Tool page() {
        return PAGE;
    }

    public static @NotNull Tool placeElement() {
        return PLACE_ELEMENT;
    }

    public static @NotNull Tool pixelPencil() {
        return PIXEL_PENCIL;
    }

    public static @NotNull Tool pixelEraser() {
        return PIXEL_ERASER;
    }

    public static @NotNull Tool pixelFill() {
        return PIXEL_FILL;
    }

    private abstract static class NamedTool implements Tool {
        private final Identifier identifier;
        private final Text displayName;

        private NamedTool(@NotNull String key, @NotNull String displayName) {
            this.identifier = Identifier.of("tool", key);
            this.displayName = Text.vanilla(displayName);
        }

        @Override
        public @NotNull Identifier identifier() {
            return identifier;
        }

        @Override
        public @NotNull Text displayName() {
            return displayName;
        }
    }

    private static final class SelectTool extends NamedTool {
        private Identifier dragPage;
        private Vec2i dragStart;
        private Vec2i pageStart;
        private Vec2i pageSize;
        private PageDragMode pageDragMode;
        private boolean recorded;

        private SelectTool() {
            super("select", "Select");
        }

        @Override
        public @NotNull CursorStyle cursor(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
            VirtualPage page = page(context, pointer);
            if (page == null || pointer.pageLocal() == null) {
                clearHover(context);
                return CursorStyle.pointer();
            }
            if (page.selected() == null) {
                Vec2i position = context.preview().pagePosition(page.identifier());
                if (position == null) return CursorStyle.click();
                return switch (pageDragMode(pointer.world(), position, page.size())) {
                    case MOVE -> CursorStyle.move();
                    case RESIZE_N, RESIZE_S -> CursorStyle.resizeVertical();
                    default -> CursorStyle.resizeHorizontal();
                };
            }
            return page.cursor(pointer.pageLocal());
        }

        @Override
        public void press(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
            VirtualPage page = page(context, pointer);
            if (page == null || pointer.pageLocal() == null) {
                context.controller().selectPage(null);
                context.controller().selectElement(null);
                return;
            }
            context.controller().select(page);
            page.selectForDrag(pointer.pageLocal());
            context.controller().selectElement(page.selected());
            if (page.selected() == null) {
                dragPage = page.identifier();
                dragStart = pointer.world();
                pageStart = context.preview().pagePosition(page.identifier());
                pageSize = page.size();
                pageDragMode = pageDragMode(pointer.world(), pageStart, pageSize);
                recorded = false;
                context.controller().hydrateSelectionControls();
                return;
            }
            context.controller().recordHistory();
            recorded = true;
            page.dragStart(pointer.pageLocal());
            context.controller().hydrateSelectionControls();
        }

        @Override
        public void drag(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
            if (dragPage != null && dragStart != null && pageStart != null && pageSize != null && pageDragMode != null) {
                if (!recorded) {
                    context.controller().recordHistory();
                    recorded = true;
                }
                VirtualPage page = context.controller().page(dragPage);
                if (page == null) return;
                PageRect rect = pageDragMode.apply(pageStart, pageSize, pointer.world().sub(dragStart));
                context.preview().positionPage(dragPage, rect.position());
                page.size(rect.size());
                return;
            }
            VirtualPage page = page(context, pointer);
            if (page == null || pointer.pageLocal() == null) return;
            page.dragUpdate(pointer.pageLocal());
            context.controller().selectElement(page.selected());
        }

        @Override
        public void release(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
            for (VirtualPage page : context.controller().pages()) {
                page.dragEnd();
            }
            dragPage = null;
            dragStart = null;
            pageStart = null;
            pageSize = null;
            pageDragMode = null;
            recorded = false;
            context.controller().hydrateSelectionControls();
        }

        @Override
        public void cancel(@NotNull ToolContext context) {
            dragPage = null;
            dragStart = null;
            pageStart = null;
            pageSize = null;
            pageDragMode = null;
            recorded = false;
        }

        private @NotNull PageDragMode pageDragMode(@NotNull Vec2i cursor, @NotNull Vec2i position, @NotNull Vec2i size) {
            int hitbox = 6;
            boolean horizontal = cursor.x() >= position.x() - hitbox && cursor.x() <= position.x() + size.x() + hitbox;
            boolean vertical = cursor.y() >= position.y() - hitbox && cursor.y() <= position.y() + size.y() + hitbox;
            boolean left = Math.abs(cursor.x() - position.x()) <= hitbox && vertical;
            boolean right = Math.abs(cursor.x() - (position.x() + size.x())) <= hitbox && vertical;
            boolean top = Math.abs(cursor.y() - position.y()) <= hitbox && horizontal;
            boolean bottom = Math.abs(cursor.y() - (position.y() + size.y())) <= hitbox && horizontal;
            if (top && left) return PageDragMode.RESIZE_NW;
            if (top && right) return PageDragMode.RESIZE_NE;
            if (bottom && left) return PageDragMode.RESIZE_SW;
            if (bottom && right) return PageDragMode.RESIZE_SE;
            if (top) return PageDragMode.RESIZE_N;
            if (bottom) return PageDragMode.RESIZE_S;
            if (left) return PageDragMode.RESIZE_W;
            if (right) return PageDragMode.RESIZE_E;
            return PageDragMode.MOVE;
        }
    }

    private static final class PageTool extends NamedTool {
        private Vec2i start;
        private Vec2i current;

        private PageTool() {
            super("page", "Page");
        }

        @Override
        public @NotNull CursorStyle cursor(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
            return CursorStyle.click();
        }

        @Override
        public void press(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
            start = pointer.world();
            current = pointer.world();
        }

        @Override
        public void drag(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
            current = pointer.world();
        }

        @Override
        public void release(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
            if (start == null) return;
            current = pointer.world();
            Rect rect = Rect.of(start, current).minimum(Vec2i.of(16, 16));
            context.controller().recordHistory();
            VirtualPage page = context.controller().page(rect.position(), rect.size());
            context.preview().positionPage(page.identifier(), rect.position());
            context.controller().select(page);
            context.controller().selectElement(null);
            context.controller().hydrateSelectionControls();
            start = null;
            current = null;
        }

        @Override
        public void cancel(@NotNull ToolContext context) {
            start = null;
            current = null;
        }

        @Override
        public @NotNull Canvas overlay(@NotNull ToolContext context, @NotNull RenderContext renderContext) {
            return rectangleOverlay(context, start, current, Color.of(77, 155, 230, 120));
        }
    }

    private static final class PlaceElementTool extends NamedTool {
        private Identifier page;
        private Vec2i start;
        private Vec2i current;

        private PlaceElementTool() {
            super("place_element", "Place Element");
        }

        @Override
        public @NotNull CursorStyle cursor(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
            return pointer.page() == null ? CursorStyle.pointer() : CursorStyle.click();
        }

        @Override
        public void press(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
            if (pointer.page() == null || pointer.pageLocal() == null) return;
            page = pointer.page();
            start = pointer.pageLocal();
            current = pointer.pageLocal();
        }

        @Override
        public void drag(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
            if (page == null) return;
            VirtualPage virtualPage = context.controller().page(page);
            if (virtualPage == null) return;
            current = clamp(context, pointer, virtualPage);
        }

        @Override
        public void release(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
            ElementConstructionProvider<?> provider = context.provider();
            if (page == null || start == null || provider == null) return;
            VirtualPage virtualPage = context.controller().page(page);
            if (virtualPage == null) return;
            current = clamp(context, pointer, virtualPage);
            Rect rect = Rect.of(start, current).minimum(provider.minimumSize());
            Vec2i position = clampPosition(rect.position(), rect.size(), virtualPage.size());
            context.controller().recordHistory();
            VirtualElement<?> element = context.controller().element(virtualPage, provider, position, rect.size());
            virtualPage.select(element.identifier());
            context.controller().select(virtualPage);
            context.controller().selectElement(element);
            context.controller().pendingProvider(null);
            context.controller().tool(select());
            context.controller().hydrateSelectionControls();
            page = null;
            start = null;
            current = null;
        }

        @Override
        public void cancel(@NotNull ToolContext context) {
            page = null;
            start = null;
            current = null;
            context.controller().pendingProvider(null);
            context.controller().tool(select());
        }

        @Override
        public @NotNull Canvas overlay(@NotNull ToolContext context, @NotNull RenderContext renderContext) {
            if (page == null || start == null || current == null) return Canvas.empty(context.preview().viewSize());
            Vec2i pagePosition = context.preview().pagePosition(page);
            if (pagePosition == null) return Canvas.empty(context.preview().viewSize());
            return rectangleOverlay(context, pagePosition.add(start), pagePosition.add(current), Color.of(35, 144, 99, 120));
        }

        private @NotNull Vec2i clamp(@NotNull ToolContext context, @NotNull ToolPointer pointer, @NotNull VirtualPage page) {
            Vec2i local = pointer.pageLocal();
            if (local == null && this.page != null) {
                local = context.preview().worldToPage(this.page, pointer.world());
            }
            if (local == null) return start;
            return Vec2i.of(Math.clamp(local.x(), 0, page.size().x()), Math.clamp(local.y(), 0, page.size().y()));
        }
    }

    private static final class PixelTool extends NamedTool {
        private final PixelAction action;
        private PixelArtElement editing;

        private PixelTool(@NotNull String key, @NotNull String displayName, @NotNull PixelAction action) {
            super(key, displayName);
            this.action = action;
        }

        @Override
        public @NotNull CursorStyle cursor(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
            return CursorStyle.click();
        }

        @Override
        public void press(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
            PixelTarget target = target(context, pointer);
            if (target == null) return;
            editing = target.pixelArtElement();
            context.controller().recordHistory();
            editing.beginChange();
            apply(context, target, pointer);
            if (action == PixelAction.FILL) {
                editing.endChange();
                editing = null;
            }
        }

        @Override
        public void drag(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
            if (action == PixelAction.FILL) return;
            PixelTarget target = target(context, pointer);
            if (target == null || target.pixelArtElement() != editing) return;
            apply(context, target, pointer);
        }

        @Override
        public void release(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
            if (editing == null) return;
            editing.endChange();
            editing = null;
        }

        @Override
        public void cancel(@NotNull ToolContext context) {
            if (editing == null) return;
            editing.endChange();
            editing = null;
        }

        private @Nullable PixelTarget target(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
            VirtualPage page = pointer.page() == null ? context.page() : context.controller().page(pointer.page());
            VirtualElement<?> element = context.element();
            if (page != null && pointer.pageLocal() != null) {
                VirtualElement<?> hovered = page.elementAt(pointer.pageLocal());
                if (hovered != null && hovered.target() instanceof PixelArtElement) {
                    element = hovered;
                    page.select(hovered.identifier());
                    context.controller().select(page);
                    context.controller().selectElement(hovered);
                }
            }
            if (element == null || !(element.target() instanceof PixelArtElement pixelArtElement)) return null;
            return new PixelTarget(page, element, pixelArtElement);
        }

        private void apply(@NotNull ToolContext context, @NotNull PixelTarget target, @NotNull ToolPointer pointer) {
            VirtualPage page = target.page();
            VirtualElement<?> element = target.element();
            PixelArtElement pixelArtElement = target.pixelArtElement();
            Vec2i pixel = pixel(pointer, element, pixelArtElement);
            if (pixel == null) return;
            switch (action) {
                case PENCIL -> pixelArtElement.pixel(pixel, context.controller().activeColor());
                case ERASER -> pixelArtElement.erase(pixel);
                case FILL -> fill(pixelArtElement, pixel, context.controller().activeColor());
            }
            if (page != null) page.markDirty();
        }

        private @Nullable Vec2i pixel(@NotNull ToolPointer pointer, @NotNull VirtualElement<?> element, @NotNull PixelArtElement pixelArtElement) {
            Vec2i pageLocal = pointer.pageLocal();
            if (pageLocal == null) return null;
            Vec2i elementLocal = pageLocal.sub(element.position().x(), element.position().y());
            if (elementLocal.x() < 0 || elementLocal.y() < 0 || elementLocal.x() >= element.size().x() || elementLocal.y() >= element.size().y())
                return null;
            Vec2i canvasSize = pixelArtElement.canvasSize();
            int x = Math.clamp((long) elementLocal.x() * canvasSize.x() / Math.max(1, element.size().x()), 0, canvasSize.x() - 1);
            int y = Math.clamp((long) elementLocal.y() * canvasSize.y() / Math.max(1, element.size().y()), 0, canvasSize.y() - 1);
            return Vec2i.of(x, y);
        }

        private void fill(@NotNull PixelArtElement element, @NotNull Vec2i start, @NotNull Color color) {
            int target = element.color(start).rgba();
            int replacement = color.rgba();
            if (target == replacement) return;
            Set<Vec2i> seen = new HashSet<>();
            ArrayDeque<Vec2i> queue = new ArrayDeque<>();
            queue.add(start);
            while (!queue.isEmpty()) {
                Vec2i pixel = queue.removeFirst();
                if (!seen.add(pixel)) continue;
                if (element.color(pixel).rgba() != target) continue;
                element.pixel(pixel, color);
                add(queue, pixel.add(1, 0), element.canvasSize());
                add(queue, pixel.add(-1, 0), element.canvasSize());
                add(queue, pixel.add(0, 1), element.canvasSize());
                add(queue, pixel.add(0, -1), element.canvasSize());
            }
        }

        private void add(@NotNull ArrayDeque<Vec2i> queue, @NotNull Vec2i pixel, @NotNull Vec2i size) {
            if (pixel.x() < 0 || pixel.y() < 0 || pixel.x() >= size.x() || pixel.y() >= size.y()) return;
            queue.add(pixel);
        }

        private record PixelTarget(@Nullable VirtualPage page, @NotNull VirtualElement<?> element,
                                   @NotNull PixelArtElement pixelArtElement) {
        }
    }

    private static @Nullable VirtualPage page(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
        if (pointer.page() == null) return null;
        return context.controller().page(pointer.page());
    }

    private static void clearHover(@NotNull ToolContext context) {
        for (VirtualPage page : context.controller().pages()) {
            page.clearHover();
        }
    }

    private static @NotNull Canvas rectangleOverlay(@NotNull ToolContext context, @Nullable Vec2i start, @Nullable Vec2i current, @NotNull Color color) {
        Canvas overlay = Canvas.empty(context.preview().viewSize());
        if (start == null || current == null) return overlay;
        Rect rect = Rect.of(context.preview().worldToScreen(start), context.preview().worldToScreen(current)).minimum(Vec2i.of(1, 1));
        overlay.fill(rect.position(), Vec2i.of(rect.size().x(), 1), color);
        overlay.fill(rect.position(), Vec2i.of(1, rect.size().y()), color);
        overlay.fill(rect.position().add(0, rect.size().y() - 1), Vec2i.of(rect.size().x(), 1), color);
        overlay.fill(rect.position().add(rect.size().x() - 1, 0), Vec2i.of(1, rect.size().y()), color);
        return overlay;
    }

    private static @NotNull Vec2i clampPosition(@NotNull Vec2i position, @NotNull Vec2i elementSize, @NotNull Vec2i pageSize) {
        return Vec2i.of(
                Math.clamp(position.x(), 0, Math.max(0, pageSize.x() - elementSize.x())),
                Math.clamp(position.y(), 0, Math.max(0, pageSize.y() - elementSize.y()))
        );
    }

    private record Rect(@NotNull Vec2i position, @NotNull Vec2i size) {

        private static @NotNull Rect of(@NotNull Vec2i first, @NotNull Vec2i second) {
            int x = Math.min(first.x(), second.x());
            int y = Math.min(first.y(), second.y());
            int width = Math.abs(first.x() - second.x());
            int height = Math.abs(first.y() - second.y());
            return new Rect(Vec2i.of(x, y), Vec2i.of(width, height));
        }

        private @NotNull Rect minimum(@NotNull Vec2i minimum) {
            return new Rect(position, Vec2i.of(Math.max(size.x(), minimum.x()), Math.max(size.y(), minimum.y())));
        }
    }

    private record PageRect(@NotNull Vec2i position, @NotNull Vec2i size) {
    }

    private enum PageDragMode {
        MOVE {
            @Override
            PageRect apply(@NotNull Vec2i position, @NotNull Vec2i size, @NotNull Vec2i delta) {
                return new PageRect(position.add(delta), size);
            }
        },
        RESIZE_N {
            @Override
            PageRect apply(@NotNull Vec2i position, @NotNull Vec2i size, @NotNull Vec2i delta) {
                int offset = Math.min(delta.y(), size.y() - 16);
                int y = position.y() + offset;
                return new PageRect(Vec2i.of(position.x(), y), Vec2i.of(size.x(), size.y() - offset));
            }
        },
        RESIZE_S {
            @Override
            PageRect apply(@NotNull Vec2i position, @NotNull Vec2i size, @NotNull Vec2i delta) {
                return new PageRect(position, Vec2i.of(size.x(), Math.max(16, size.y() + delta.y())));
            }
        },
        RESIZE_E {
            @Override
            PageRect apply(@NotNull Vec2i position, @NotNull Vec2i size, @NotNull Vec2i delta) {
                return new PageRect(position, Vec2i.of(Math.max(16, size.x() + delta.x()), size.y()));
            }
        },
        RESIZE_W {
            @Override
            PageRect apply(@NotNull Vec2i position, @NotNull Vec2i size, @NotNull Vec2i delta) {
                int offset = Math.min(delta.x(), size.x() - 16);
                int x = position.x() + offset;
                return new PageRect(Vec2i.of(x, position.y()), Vec2i.of(size.x() - offset, size.y()));
            }
        },
        RESIZE_NE {
            @Override
            PageRect apply(@NotNull Vec2i position, @NotNull Vec2i size, @NotNull Vec2i delta) {
                return RESIZE_E.apply(RESIZE_N.apply(position, size, delta).position(), RESIZE_N.apply(position, size, delta).size(), delta);
            }
        },
        RESIZE_NW {
            @Override
            PageRect apply(@NotNull Vec2i position, @NotNull Vec2i size, @NotNull Vec2i delta) {
                return RESIZE_W.apply(RESIZE_N.apply(position, size, delta).position(), RESIZE_N.apply(position, size, delta).size(), delta);
            }
        },
        RESIZE_SE {
            @Override
            PageRect apply(@NotNull Vec2i position, @NotNull Vec2i size, @NotNull Vec2i delta) {
                return new PageRect(position, Vec2i.of(Math.max(16, size.x() + delta.x()), Math.max(16, size.y() + delta.y())));
            }
        },
        RESIZE_SW {
            @Override
            PageRect apply(@NotNull Vec2i position, @NotNull Vec2i size, @NotNull Vec2i delta) {
                return RESIZE_W.apply(RESIZE_S.apply(position, size, delta).position(), RESIZE_S.apply(position, size, delta).size(), delta);
            }
        };

        abstract PageRect apply(@NotNull Vec2i position, @NotNull Vec2i size, @NotNull Vec2i delta);
    }

    private enum PixelAction {
        PENCIL, ERASER, FILL
    }
}
