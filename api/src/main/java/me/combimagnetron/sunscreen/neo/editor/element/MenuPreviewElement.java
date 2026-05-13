package me.combimagnetron.sunscreen.neo.editor.element;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.passport.util.math.Vec2i;
import me.combimagnetron.sunscreen.neo.cursor.CursorStyle;
import me.combimagnetron.sunscreen.neo.editor.EditorController;
import me.combimagnetron.sunscreen.neo.editor.tool.ToolContext;
import me.combimagnetron.sunscreen.neo.editor.tool.ToolPointer;
import me.combimagnetron.sunscreen.neo.editor.virtual.VirtualPage;
import me.combimagnetron.sunscreen.neo.element.GenericInteractableModernElement;
import me.combimagnetron.sunscreen.neo.event.UserMoveStateChangeEvent;
import me.combimagnetron.sunscreen.neo.event.UserScrollStateChangeEvent;
import me.combimagnetron.sunscreen.neo.graphic.Canvas;
import me.combimagnetron.sunscreen.neo.graphic.color.Color;
import me.combimagnetron.sunscreen.neo.graphic.text.Text;
import me.combimagnetron.sunscreen.neo.graphic.text.style.impl.color.TextColor;
import me.combimagnetron.sunscreen.neo.input.InputHandler;
import me.combimagnetron.sunscreen.neo.input.ListenerReferences;
import me.combimagnetron.sunscreen.neo.input.context.MouseInputContext;
import me.combimagnetron.sunscreen.neo.input.context.ScrollInputContext;
import me.combimagnetron.sunscreen.neo.property.Size;
import me.combimagnetron.sunscreen.neo.render.engine.context.RenderContext;
import me.combimagnetron.sunscreen.util.helper.HoverHelper;
import me.combimagnetron.sunscreen.util.helper.PropertyHelper;
import me.combimagnetron.sunscreen.util.helper.editor.NameHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class MenuPreviewElement extends GenericInteractableModernElement<MenuPreviewElement, Canvas, MenuPreviewElement.MenuPreviewElementListenerReferences> {
    private static final Color GRAY = Color.of(180, 180, 180);
    private static final Color WHITE = Color.of(255, 255, 255);
    private final MenuPreviewElementListenerReferences references = new MenuPreviewElementListenerReferences(this);
    private final EditorController controller;
    private final Map<Identifier, Entry> renderedPages = new LinkedHashMap<>();
    private final Canvas canvas = Canvas.empty(Vec2i.of(2048, 2048));
    private Vec2i lastPos;
    private Vec2i center = Vec2i.of(259, 216);
    private double zoom = 1.0;
    private boolean leftWasPressed;
    private boolean rightWasPressed;

    protected MenuPreviewElement(@Nullable Identifier identifier, @NotNull EditorController controller) {
        super(identifier);
        this.controller = controller;
        this.controller.preview(this);
        populate();
    }

    public static @NotNull MenuPreviewElement empty(@NotNull EditorController controller) {
        return new MenuPreviewElement(Identifier.of("preview_empty"), controller);
    }

    private void populate() {
        for (int y = 0; y < canvas.size().y(); y++) {
            for (int x = 0; x < canvas.size().x(); x++) {
                boolean gray = ((x / 7) + (y / 7)) % 2 == 0;
                canvas.color(Vec2i.of(x, y), gray ? GRAY : WHITE);
            }
        }
    }

    private void renderPages() {
        renderedPages.keySet().removeIf(identifier -> controller.page(identifier) == null);
        for (VirtualPage page : controller.pages()) {
            boolean dirty = page.dirty();
            Canvas pageCanvas = page.render(controller.context());
            Entry existing = renderedPages.get(page.identifier());
            Vec2i position = pagePosition(page.identifier());
            if (position == null) {
                position = findNextSpace(pageCanvas.size());
                controller.pagePosition(page.identifier(), position);
            }
            if (existing == null || dirty) {
                renderedPages.put(page.identifier(), new Entry(pageCanvas, position));
            }
        }
    }

    public @NotNull Vec2i viewSize() {
        return PropertyHelper.vectorOrThrow(size(), Vec2i.class);
    }

    public synchronized double zoom() {
        return zoom;
    }

    public synchronized @NotNull Vec2i center() {
        return center;
    }

    public synchronized @NotNull Vec2i screenToWorld(@NotNull Vec2i screenPos) {
        return transform().world(localScreen(screenPos));
    }

    public synchronized @NotNull Vec2i worldToScreen(@NotNull Vec2i worldPos) {
        return transform().screen(worldPos);
    }

    public @Nullable Vec2i pagePosition(@NotNull Identifier identifier) {
        Vec2i position = controller.pagePosition(identifier);
        if (position != null) return position;
        Entry entry = renderedPages.get(identifier);
        return entry == null ? null : entry.position();
    }

    public void positionPage(@NotNull Identifier identifier, @NotNull Vec2i position) {
        controller.pagePosition(identifier, position);
        Entry entry = renderedPages.get(identifier);
        if (entry != null) renderedPages.put(identifier, new Entry(entry.canvas(), position));
    }

    public @Nullable Identifier pageAtWorld(@NotNull Vec2i world) {
        for (Map.Entry<Identifier, Entry> entry : renderedPages.entrySet()) {
            Vec2i pos = entry.getValue().position();
            Vec2i size = entry.getValue().canvas().size();
            if (world.x() >= pos.x() && world.x() < pos.x() + size.x() && world.y() >= pos.y() && world.y() < pos.y() + size.y())
                return entry.getKey();
        }
        return null;
    }

    public @Nullable Vec2i worldToPage(@NotNull Identifier identifier, @NotNull Vec2i world) {
        Vec2i position = pagePosition(identifier);
        if (position == null) return null;
        return world.sub(position.x(), position.y());
    }

    public @NotNull Vec2i pageToWorld(@NotNull Identifier identifier, @NotNull Vec2i pageLocal) {
        Vec2i position = pagePosition(identifier);
        if (position == null) return pageLocal;
        return position.add(pageLocal);
    }

    private @NotNull ToolPointer pointer(@NotNull Vec2i cursor) {
        Vec2i local = localScreen(cursor);
        Vec2i world = screenToWorld(cursor);
        Identifier page = pageAtWorld(world);
        Vec2i pageLocal = page == null ? null : worldToPage(page, world);
        return new ToolPointer(cursor, local, world, page, pageLocal);
    }

    private @NotNull Vec2i findNextSpace(@NotNull Vec2i size) {
        int rowY = 17;
        int rowX = 3;
        int rowHeight = 0;
        for (Entry entry : renderedPages.values()) {
            Vec2i pos = entry.position();
            Vec2i pageSize = entry.canvas().size();
            if (pos.y() > rowY) {
                rowY = pos.y();
                rowX = pos.x() + pageSize.x() + 3;
                rowHeight = pageSize.y();
            } else {
                rowX = Math.max(rowX, pos.x() + pageSize.x() + 3);
                rowHeight = Math.max(rowHeight, pageSize.y());
            }
        }
        if (rowX + size.x() > canvas.size().x()) return Vec2i.of(3, rowY + rowHeight + 17);
        return Vec2i.of(rowX, rowY);
    }

    @Override
    protected void lateInit() {
        super.lateInit();
        InputHandler handler = inputHandler();
        if (handler == null) return;
        handler.subscribe(identifier(), ScrollInputContext.class, this::handleScroll);
        handler.subscribe(identifier(), MouseInputContext.class, this::handleCursor);
    }

    private synchronized void handleCursor(@NotNull UserMoveStateChangeEvent event) {
        InputHandler inputHandler = inputHandler();
        if (inputHandler == null || event.user() != inputHandler.user()) return;
        MouseInputContext context = event.context();
        Vec2i cursor = context.position();
        if (!HoverHelper.in(this, cursor)) {
            cleanupOutsidePreview(context, cursor);
            return;
        }
        if (context.rightPressed()) {
            if (!rightWasPressed) lastPos = cursor;
            handlePan(inputHandler, cursor);
            rightWasPressed = true;
            leftWasPressed = context.leftPressed();
            return;
        }
        if (rightWasPressed) {
            rightWasPressed = false;
            lastPos = null;
        }
        ToolPointer pointer = pointer(cursor);
        ToolContext toolContext = controller.toolContext();
        inputHandler.cursor(controller.tool().cursor(toolContext, pointer));
        boolean moved = lastPos == null || !lastPos.equals(cursor);
        if (context.leftPressed() && !leftWasPressed) controller.tool().press(toolContext, pointer);
        if (context.leftPressed() && leftWasPressed && moved) controller.tool().drag(toolContext, pointer);
        if (!context.leftPressed() && leftWasPressed) controller.tool().release(toolContext, pointer);
        leftWasPressed = context.leftPressed();
        lastPos = cursor;
    }

    private void cleanupOutsidePreview(@NotNull MouseInputContext context, @NotNull Vec2i cursor) {
        if (context.leftPressed() && leftWasPressed) {
            ToolPointer pointer = pointer(cursor);
            controller.tool().drag(controller.toolContext(), pointer);
        }
        if (!context.leftPressed() && leftWasPressed) {
            ToolPointer pointer = pointer(cursor);
            controller.tool().release(controller.toolContext(), pointer);
            leftWasPressed = false;
        }
        if (!context.rightPressed()) rightWasPressed = false;
        lastPos = context.leftPressed() || context.rightPressed() ? cursor : null;
    }

    private void handlePan(@NotNull InputHandler inputHandler, @NotNull Vec2i cursor) {
        inputHandler.cursor(CursorStyle.move());
        if (lastPos != null) {
            Vec2i delta = transform().worldDelta(cursor.sub(lastPos.x(), lastPos.y()));
            center = center.sub(delta.x(), delta.y());
            Vec2i size = PropertyHelper.vectorOrThrow(size(), Vec2i.class);
            clampCenter(size);
        }
        lastPos = cursor;
    }

    private synchronized void handleScroll(@NotNull UserScrollStateChangeEvent event) {
        InputHandler inputHandler = inputHandler();
        if (inputHandler == null || event.user() != inputHandler.user()) return;
        MouseInputContext mouseContext = inputHandler.context(MouseInputContext.class);
        if (!HoverHelper.in(this, mouseContext.position())) return;
        Vec2i cursor = mouseContext.position();
        Vec2i before = screenToWorld(cursor);
        Vec2i size = PropertyHelper.vectorOrThrow(size(), Vec2i.class);
        zoom = clampZoom(event.context().value() == 1f ? zoom * 1.1 : zoom / 1.1, minimumZoom(size));
        center = transform(before, zoom, localScreen(cursor)).origin();
        clampCenter(size);
    }

    @Override
    public @NonNull MenuPreviewElementListenerReferences listen() {
        return references;
    }

    @Override
    public synchronized @NotNull Canvas render(@NonNull Size property, @Nullable RenderContext context) {
        Vec2i size = PropertyHelper.vectorOrThrow(size(), Vec2i.class);
        zoom = clampZoom(zoom, minimumZoom(size));
        clampCenter(size);
        Vec2i viewSize = worldViewSize(size);
        center = viewPosition(viewSize);
        Matrix transform = transform();
        Canvas rendered = Canvas.empty(size);
        drawBackground(rendered, transform);
        renderPages();
        for (Map.Entry<Identifier, Entry> entry : renderedPages.entrySet()) {
            placePage(rendered, entry.getValue(), size, transform);
            placeSelection(rendered, entry.getKey(), entry.getValue(), transform);
            placeLabel(rendered, entry.getKey(), entry.getValue(), size, transform);
        }
        rendered.place(controller.tool().overlay(controller.toolContext(), controller.context()), Vec2i.zero());
        return rendered;
    }

    private void drawBackground(@NotNull Canvas rendered, @NotNull Matrix transform) {
        for (int y = 0; y < rendered.size().y(); y++) {
            for (int x = 0; x < rendered.size().x(); x++) {
                Vec2i world = transform.world(Vec2i.of(x, y));
                rendered.bufferedColorSpace().colorDirectInteger(x, y, canvas.bufferedColorSpace().at(world.x(), world.y()));
            }
        }
    }

    private void placePage(@NotNull Canvas rendered, @NotNull Entry entry, @NotNull Vec2i size, @NotNull Matrix transform) {
        Vec2i position = entry.position();
        Vec2i pageSize = entry.canvas().size();
        for (int y = 0; y < size.y(); y++) {
            Vec2i row = transform.world(Vec2i.of(0, y));
            int pageY = row.y() - position.y();
            if (pageY < 0 || pageY >= pageSize.y()) continue;
            for (int x = 0; x < size.x(); x++) {
                Vec2i world = transform.world(Vec2i.of(x, y));
                int pageX = world.x() - position.x();
                if (pageX < 0 || pageX >= pageSize.x()) continue;
                rendered.bufferedColorSpace().colorDirectInteger(x, y, entry.canvas().bufferedColorSpace().at(pageX, pageY));
            }
        }
    }

    private void placeSelection(@NotNull Canvas rendered, @NotNull Identifier identifier, @NotNull Entry entry, @NotNull Matrix transform) {
        VirtualPage selected = controller.selected();
        if (selected == null || !selected.identifier().equals(identifier)) return;
        Vec2i pageSize = entry.canvas().size();
        Vec2i screenPos = transform.screen(entry.position());
        Vec2i screenSize = Vec2i.of(Math.max(1, (int) Math.round(pageSize.x() * zoom)), Math.max(1, (int) Math.round(pageSize.y() * zoom)));
        Color color = selected.selected() == null ? Color.of(35, 144, 99) : Color.of(77, 155, 230);
        fill(rendered, Vec2i.of(screenPos.x() - 1, screenPos.y() - 1), Vec2i.of(screenSize.x() + 2, 1), color);
        fill(rendered, Vec2i.of(screenPos.x() - 1, screenPos.y() + screenSize.y()), Vec2i.of(screenSize.x() + 2, 1), color);
        fill(rendered, Vec2i.of(screenPos.x() - 1, screenPos.y() - 1), Vec2i.of(1, screenSize.y() + 2), color);
        fill(rendered, Vec2i.of(screenPos.x() + screenSize.x(), screenPos.y() - 1), Vec2i.of(1, screenSize.y() + 2), color);
    }

    private void fill(@NotNull Canvas canvas, @NotNull Vec2i position, @NotNull Vec2i amount, @NotNull Color color) {
        int x = Math.max(0, position.x());
        int y = Math.max(0, position.y());
        int maxX = Math.min(canvas.size().x(), position.x() + amount.x());
        int maxY = Math.min(canvas.size().y(), position.y() + amount.y());
        if (maxX <= x || maxY <= y) return;
        canvas.fill(Vec2i.of(x, y), Vec2i.of(maxX - x, maxY - y), color);
    }

    private void placeLabel(@NotNull Canvas rendered, @NotNull Identifier identifier, @NotNull Entry entry, @NotNull Vec2i size, @NotNull Matrix transform) {
        Vec2i pageSize = entry.canvas().size();
        Vec2i screenPos = transform.screen(entry.position());
        Vec2i screenSize = Vec2i.of(Math.max(1, (int) Math.round(pageSize.x() * zoom)), Math.max(1, (int) Math.round(pageSize.y() * zoom)));
        if (screenPos.x() >= size.x() || screenPos.y() >= size.y()) return;
        if (screenPos.x() + screenSize.x() <= 0 || screenPos.y() + screenSize.y() <= 0) return;
        Canvas text = Text.vanilla("Page \"" + NameHelper.suggestDisplayName(identifier.string()) + "\"").color(TextColor.color(Color.of(255, 255, 255))).render(Size.fixed(Vec2i.of(100, 11)), null).trim();
        Canvas label = FrameElement.frame(Vec2i.of(15 + text.size().x(), 13));
        label.place(Canvas.resource("editor_assets/pc_icon.png"), Vec2i.of(2, 2));
        label.place(text, Vec2i.of(13, 3));
        int labelX = Math.clamp(screenPos.x(), 0, Math.max(0, size.x() - label.size().x()));
        int labelY = screenPos.y() - 14;
        if (labelY < 0) labelY = Math.min(size.y() - label.size().y(), screenPos.y() + screenSize.y() + 1);
        rendered.place(label, Vec2i.of(labelX, labelY));
    }

    private @NotNull Vec2i localScreen(@NotNull Vec2i screen) {
        Vec2i elementPos = PropertyHelper.vectorOrThrow(position(), Vec2i.class);
        return screen.sub(elementPos.x(), elementPos.y());
    }

    private @NotNull Matrix transform() {
        return transform(center, zoom);
    }

    private @NotNull Matrix transform(@NotNull Vec2i origin, double zoom) {
        return new Matrix(zoom, 0, 0, zoom, -origin.x() * zoom, -origin.y() * zoom);
    }

    private @NotNull Matrix transform(@NotNull Vec2i world, double zoom, @NotNull Vec2i screen) {
        return transform(Vec2i.of(
                (int) Math.floor(world.x() - screen.x() / zoom),
                (int) Math.floor(world.y() - screen.y() / zoom)
        ), zoom);
    }

    private @NotNull Vec2i worldViewSize(@NotNull Vec2i size) {
        return Vec2i.of(
                Math.clamp((int) Math.ceil(size.x() / zoom), 1, canvas.size().x()),
                Math.clamp((int) Math.ceil(size.y() / zoom), 1, canvas.size().y())
        );
    }

    private double minimumZoom(@NotNull Vec2i size) {
        return Math.max(0.5, Math.max((double) size.x() / canvas.size().x(), (double) size.y() / canvas.size().y()));
    }

    private void clampCenter(@NotNull Vec2i size) {
        Vec2i viewSize = worldViewSize(size);
        center = viewPosition(viewSize);
    }

    private @NotNull Vec2i viewPosition(@NotNull Vec2i viewSize) {
        center = Vec2i.of(
            Math.clamp(center.x(), 0, Math.max(0, canvas.size().x() - viewSize.x())),
            Math.clamp(center.y(), 0, Math.max(0, canvas.size().y() - viewSize.y()))
        );
        return center;
    }

    private double clampZoom(double value, double min) {
        if (value < min) return min;
        return Math.min(value, 2.0);
    }

    public static final class MenuPreviewElementListenerReferences extends ListenerReferences<MenuPreviewElement, MenuPreviewElementListenerReferences> {
        private final @NotNull MenuPreviewElement back;

        public MenuPreviewElementListenerReferences(@NotNull MenuPreviewElement back) {
            this.back = back;
        }

        @Override
        public @NotNull MenuPreviewElement back() {
            return back;
        }
    }

    private record Entry(@NotNull Canvas canvas, @NotNull Vec2i position) {
    }

    private record Matrix(double a, double b, double c, double d, double e, double f) {
        private @NotNull Vec2i screen(@NotNull Vec2i world) {
            return Vec2i.of(
                    (int) Math.floor(a * world.x() + c * world.y() + e),
                    (int) Math.floor(b * world.x() + d * world.y() + f)
            );
        }

        private @NotNull Vec2i world(@NotNull Vec2i screen) {
            double det = a * d - b * c;
            double x = screen.x() - e;
            double y = screen.y() - f;
            return Vec2i.of(
                    (int) Math.floor((d * x - c * y) / det),
                    (int) Math.floor((-b * x + a * y) / det)
            );
        }

        private @NotNull Vec2i worldDelta(@NotNull Vec2i screen) {
            double det = a * d - b * c;
            return Vec2i.of(
                    (int) Math.round((d * screen.x() - c * screen.y()) / det),
                    (int) Math.round((-b * screen.x() + a * screen.y()) / det)
            );
        }

        private @NotNull Vec2i origin() {
            return world(Vec2i.zero());
        }
    }
}
