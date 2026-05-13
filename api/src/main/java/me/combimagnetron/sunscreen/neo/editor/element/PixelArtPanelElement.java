package me.combimagnetron.sunscreen.neo.editor.element;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.passport.util.math.Vec2i;
import me.combimagnetron.sunscreen.neo.cursor.CursorStyle;
import me.combimagnetron.sunscreen.neo.editor.EditorController;
import me.combimagnetron.sunscreen.neo.editor.tool.Tools;
import me.combimagnetron.sunscreen.neo.editor.virtual.VirtualElement;
import me.combimagnetron.sunscreen.neo.editor.virtual.VirtualPage;
import me.combimagnetron.sunscreen.neo.element.GenericInteractableModernElement;
import me.combimagnetron.sunscreen.neo.event.UserMoveStateChangeEvent;
import me.combimagnetron.sunscreen.neo.graphic.Canvas;
import me.combimagnetron.sunscreen.neo.graphic.color.Color;
import me.combimagnetron.sunscreen.neo.graphic.text.Text;
import me.combimagnetron.sunscreen.neo.graphic.text.style.impl.color.TextColor;
import me.combimagnetron.sunscreen.neo.input.InputHandler;
import me.combimagnetron.sunscreen.neo.input.ListenerReferences;
import me.combimagnetron.sunscreen.neo.input.context.MouseInputContext;
import me.combimagnetron.sunscreen.neo.property.Size;
import me.combimagnetron.sunscreen.neo.render.engine.context.RenderContext;
import me.combimagnetron.sunscreen.util.helper.HoverHelper;
import me.combimagnetron.sunscreen.util.helper.PropertyHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PixelArtPanelElement extends GenericInteractableModernElement<PixelArtPanelElement, Canvas, PixelArtPanelElement.PixelArtPanelElementListenerReferences> {
    private final PixelArtPanelElementListenerReferences references = new PixelArtPanelElementListenerReferences(this);
    private final EditorController controller;
    private final Color[] colors = new Color[]{
            Color.of(0, 0, 0),
            Color.of(255, 255, 255),
            Color.of(220, 40, 40),
            Color.of(40, 180, 80),
            Color.of(50, 90, 220),
            Color.of(230, 210, 40),
            Color.none(),
            Color.of(130, 130, 130)
    };
    private boolean leftWasPressed;

    public PixelArtPanelElement(@NotNull Identifier identifier, @NotNull EditorController controller) {
        super(identifier);
        this.controller = controller;
    }

    @Override
    protected void lateInit() {
        super.lateInit();
        InputHandler handler = inputHandler();
        if (handler == null) return;
        handler.subscribe(identifier(), MouseInputContext.class, this::handleCursor);
    }

    private void handleCursor(@NotNull UserMoveStateChangeEvent event) {
        InputHandler handler = inputHandler();
        if (handler == null || event.user() != handler.user()) return;
        MouseInputContext context = event.context();
        if (!focused()) {
            leftWasPressed = context.leftPressed();
            return;
        }
        boolean hover = HoverHelper.in(this, context.position());
        if (hover) handler.cursor(CursorStyle.click());
        if (hover && context.leftPressed() && !leftWasPressed) {
            click(context.position().sub(PropertyHelper.vectorOrThrow(position(), Vec2i.class)));
        }
        leftWasPressed = context.leftPressed();
    }

    private void click(@NotNull Vec2i local) {
        PixelArtElement element = selectedPixelArt();
        if (element == null) return;
        if (inside(local, Vec2i.of(4, 20), Vec2i.of(34, 14))) {
            controller.undo();
            dirty();
            return;
        }
        if (inside(local, Vec2i.of(42, 20), Vec2i.of(34, 14))) {
            controller.redo();
            dirty();
            return;
        }
        if (inside(local, Vec2i.of(80, 20), Vec2i.of(42, 14))) {
            controller.tool(Tools.select());
            controller.selectElement(null);
            dirty();
            return;
        }
        if (inside(local, Vec2i.of(4, 48), Vec2i.of(26, 14))) {
            controller.recordHistory();
            element.pixelScale(element.pixelScale() - 1);
            dirty();
            return;
        }
        if (inside(local, Vec2i.of(34, 48), Vec2i.of(26, 14))) {
            controller.recordHistory();
            element.pixelScale(element.pixelScale() + 1);
            dirty();
            return;
        }
        if (inside(local, Vec2i.of(4, 76), Vec2i.of(26, 14))) {
            controller.tool(Tools.pixelPencil());
            return;
        }
        if (inside(local, Vec2i.of(34, 76), Vec2i.of(26, 14))) {
            controller.tool(Tools.pixelEraser());
            return;
        }
        if (inside(local, Vec2i.of(64, 76), Vec2i.of(26, 14))) {
            controller.tool(Tools.pixelFill());
            return;
        }
        for (int i = 0; i < colors.length; i++) {
            Vec2i pos = Vec2i.of(4 + i * 16, 104);
            if (!inside(local, pos, Vec2i.of(12, 12))) continue;
            controller.activeColor(colors[i]);
            return;
        }
    }

    private boolean inside(@NotNull Vec2i local, @NotNull Vec2i position, @NotNull Vec2i size) {
        return local.x() >= position.x() && local.y() >= position.y() && local.x() < position.x() + size.x() && local.y() < position.y() + size.y();
    }

    private boolean focused() {
        return selectedPixelArt() != null;
    }

    private @Nullable PixelArtElement selectedPixelArt() {
        VirtualElement<?> element = controller.selectedElement();
        if (element == null || !(element.target() instanceof PixelArtElement pixelArtElement)) return null;
        return pixelArtElement;
    }

    private void dirty() {
        VirtualPage page = controller.selected();
        if (page != null) page.markDirty();
    }

    @Override
    public @NotNull PixelArtPanelElementListenerReferences listen() {
        return references;
    }

    @Override
    public @NotNull Canvas render(@NotNull Size property, @Nullable RenderContext context) {
        Vec2i size = PropertyHelper.vectorOrThrow(size(), Vec2i.class);
        Canvas canvas = Canvas.empty(size);
        PixelArtElement element = selectedPixelArt();
        if (element == null) return canvas;
        canvas.place(FrameElement.frame(size), Vec2i.zero());
        canvas.fill(Vec2i.of(1, 1), size.sub(2), Color.of(13, 13, 13));
        text(canvas, "Pixel Art", Vec2i.of(4, 4), Color.of(255, 255, 255), context);
        action(canvas, "Undo", Vec2i.of(4, 20), element.canUndo(), context);
        action(canvas, "Redo", Vec2i.of(42, 20), element.canRedo(), context);
        action(canvas, "Done", Vec2i.of(80, 20), true, context);
        text(canvas, "Scale " + element.pixelScale() + "  Grid " + element.canvasSize().x() + "x" + element.canvasSize().y(), Vec2i.of(4, 38), Color.of(180, 180, 180), context);
        action(canvas, "-", Vec2i.of(4, 48), element.pixelScale() > 1, context);
        action(canvas, "+", Vec2i.of(34, 48), element.pixelScale() < 16, context);
        text(canvas, "Tool", Vec2i.of(4, 66), Color.of(180, 180, 180), context);
        tool(canvas, "P", Vec2i.of(4, 76), controller.tool() == Tools.pixelPencil(), context);
        tool(canvas, "E", Vec2i.of(34, 76), controller.tool() == Tools.pixelEraser(), context);
        tool(canvas, "F", Vec2i.of(64, 76), controller.tool() == Tools.pixelFill(), context);
        text(canvas, "Color", Vec2i.of(4, 94), Color.of(180, 180, 180), context);
        for (int i = 0; i < colors.length; i++) {
            swatch(canvas, Vec2i.of(4 + i * 16, 104), colors[i]);
        }
        return canvas;
    }

    private void action(@NotNull Canvas canvas, @NotNull String label, @NotNull Vec2i position, boolean active, @Nullable RenderContext context) {
        Color background = active ? Color.of(39, 39, 39) : Color.of(20, 20, 20);
        Color textColor = active ? Color.of(255, 255, 255) : Color.of(93, 93, 93);
        int width = label.length() > 2 ? 34 : 18;
        canvas.fill(position, Vec2i.of(width, 14), background);
        canvas.fill(position.add(1, 1), Vec2i.of(width - 2, 12), Color.of(13, 13, 13));
        text(canvas, label, position.add(label.length() > 2 ? 4 : 3, 3), textColor, context);
    }

    private void tool(@NotNull Canvas canvas, @NotNull String label, @NotNull Vec2i position, boolean active, @Nullable RenderContext context) {
        Color background = active ? Color.of(77, 155, 230) : Color.of(39, 39, 39);
        canvas.fill(position, Vec2i.of(26, 14), background);
        canvas.fill(position.add(1, 1), Vec2i.of(24, 12), active ? Color.of(31, 62, 84) : Color.of(13, 13, 13));
        text(canvas, label, position.add(10, 3), Color.of(255, 255, 255), context);
    }

    private void swatch(@NotNull Canvas canvas, @NotNull Vec2i position, @NotNull Color color) {
        boolean active = color.rgba() == controller.activeColor().rgba();
        canvas.fill(position.sub(1, 1), Vec2i.of(14, 14), active ? Color.of(255, 255, 255) : Color.of(39, 39, 39));
        if (color.rgba() == Color.none().rgba()) {
            canvas.fill(position, Vec2i.of(12, 12), Color.of(180, 180, 180));
            canvas.fill(position.add(1, 1), Vec2i.of(10, 10), Color.of(13, 13, 13));
            canvas.fill(position.add(2, 2), Vec2i.of(8, 8), Color.of(80, 80, 80));
            return;
        }
        canvas.fill(position, Vec2i.of(12, 12), color);
    }

    private void text(@NotNull Canvas canvas, @NotNull String value, @NotNull Vec2i position, @NotNull Color color, @Nullable RenderContext context) {
        Canvas text = Text.vanilla(value).color(TextColor.color(color)).render(Size.fixed(Vec2i.of(80, 12)), context);
        canvas.place(text, position);
    }

    public static final class PixelArtPanelElementListenerReferences extends ListenerReferences<PixelArtPanelElement, PixelArtPanelElementListenerReferences> {
        private final @NotNull PixelArtPanelElement back;

        public PixelArtPanelElementListenerReferences(@NotNull PixelArtPanelElement back) {
            this.back = back;
        }

        @Override
        public @NotNull PixelArtPanelElement back() {
            return back;
        }
    }
}
