package me.combimagnetron.sunscreen.neo.editor.element;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.passport.util.math.Vec2i;
import me.combimagnetron.sunscreen.neo.cursor.CursorStyle;
import me.combimagnetron.sunscreen.neo.editor.EditorController;
import me.combimagnetron.sunscreen.neo.editor.tool.Tools;
import me.combimagnetron.sunscreen.neo.editor.virtual.VirtualElement;
import me.combimagnetron.sunscreen.neo.editor.virtual.VirtualPage;
import me.combimagnetron.sunscreen.neo.element.GenericInteractableModernElement;
import me.combimagnetron.sunscreen.neo.element.impl.ButtonElement;
import me.combimagnetron.sunscreen.neo.element.impl.DropdownElement;
import me.combimagnetron.sunscreen.neo.element.impl.SelectorElement;
import me.combimagnetron.sunscreen.neo.event.UserMoveStateChangeEvent;
import me.combimagnetron.sunscreen.neo.event.UserTextStateChangeEvent;
import me.combimagnetron.sunscreen.neo.graphic.Canvas;
import me.combimagnetron.sunscreen.neo.graphic.color.Color;
import me.combimagnetron.sunscreen.neo.graphic.text.Text;
import me.combimagnetron.sunscreen.neo.graphic.text.style.impl.color.TextColor;
import me.combimagnetron.sunscreen.neo.input.InputHandler;
import me.combimagnetron.sunscreen.neo.input.ListenerReferences;
import me.combimagnetron.sunscreen.neo.input.context.MouseInputContext;
import me.combimagnetron.sunscreen.neo.input.context.TextInputContext;
import me.combimagnetron.sunscreen.neo.property.Size;
import me.combimagnetron.sunscreen.neo.render.engine.context.RenderContext;
import me.combimagnetron.sunscreen.util.helper.HoverHelper;
import me.combimagnetron.sunscreen.util.helper.PropertyHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ElementPanelElement extends GenericInteractableModernElement<ElementPanelElement, Canvas, ElementPanelElement.ElementPanelElementListenerReferences> {
    private final ElementPanelElementListenerReferences references = new ElementPanelElementListenerReferences(this);
    private final EditorController controller;
    private final Color[] colors = new Color[]{
            Color.of(0, 0, 0),
            Color.of(255, 255, 255),
            Color.of(220, 40, 40),
            Color.of(40, 180, 80),
            Color.of(50, 90, 220),
            Color.of(230, 210, 40),
            Color.of(130, 130, 130)
    };
    private boolean leftWasPressed;
    private Edit edit;
    private Identifier editElement;
    private int editIndex;

    public ElementPanelElement(@NotNull Identifier identifier, @NotNull EditorController controller) {
        super(identifier);
        this.controller = controller;
    }

    @Override
    protected void lateInit() {
        super.lateInit();
        InputHandler handler = inputHandler();
        if (handler == null) return;
        references.subscribe(handler);
        handler.subscribe(identifier(), MouseInputContext.class, this::handleCursor);
        handler.subscribe(identifier(), TextInputContext.class, this::handleText);
    }

    private void handleCursor(@NotNull UserMoveStateChangeEvent event) {
        InputHandler handler = inputHandler();
        if (handler == null || event.user() != handler.user()) return;
        MouseInputContext context = event.context();
        if (!focused()) {
            leftWasPressed = context.leftPressed();
            edit = null;
            return;
        }
        boolean hover = HoverHelper.in(this, context.position());
        if (hover) handler.cursor(CursorStyle.click());
        if (hover && context.leftPressed() && !leftWasPressed) {
            click(context.position().sub(PropertyHelper.vectorOrThrow(position(), Vec2i.class)));
        }
        leftWasPressed = context.leftPressed();
    }

    private void handleText(@NotNull UserTextStateChangeEvent event) {
        InputHandler handler = inputHandler();
        if (handler == null || event.user() != handler.user()) return;
        VirtualElement<?> element = controller.selectedElement();
        if (edit == null || element == null || !element.identifier().equals(editElement)) return;
        String value = event.context().stream().value();
        if (value.isBlank()) return;
        controller.recordHistory();
        switch (edit) {
            case BUTTON_TEXT -> {
                if (element.target() instanceof ButtonElement buttonElement) buttonElement.text(Text.vanilla(value));
            }
            case DROPDOWN_ENTRY -> {
                if (element.target() instanceof DropdownElement dropdownElement && editIndex >= 0 && editIndex < dropdownElement.entries().size())
                    dropdownElement.entry(editIndex, Text.vanilla(value));
            }
            case SELECTOR_ENTRY -> {
                if (element.target() instanceof SelectorElement selectorElement && editIndex >= 0 && editIndex < selectorElement.entries().size())
                    selectorElement.entry(editIndex, Text.vanilla(value));
            }
        }
        dirty();
    }

    private void click(@NotNull Vec2i local) {
        VirtualElement<?> element = controller.selectedElement();
        if (element == null) return;
        if (inside(local, Vec2i.of(96, 4), Vec2i.of(34, 14))) {
            controller.tool(Tools.select());
            controller.selectElement(null);
            edit = null;
            dirty();
            return;
        }
        if (element.target() instanceof ButtonElement buttonElement) {
            clickButton(element, buttonElement, local);
            return;
        }
        if (element.target() instanceof DropdownElement dropdownElement) {
            clickEntries(element, dropdownElement, local);
            return;
        }
        if (element.target() instanceof SelectorElement selectorElement) {
            clickEntries(element, selectorElement, local);
        }
    }

    private void clickButton(@NotNull VirtualElement<?> element, @NotNull ButtonElement buttonElement, @NotNull Vec2i local) {
        if (inside(local, Vec2i.of(4, 23), Vec2i.of(82, 14))) {
            start(Edit.BUTTON_TEXT, element.identifier(), 0);
            return;
        }
        if (inside(local, Vec2i.of(4, 49), Vec2i.of(26, 14))) {
            controller.recordHistory();
            buttonElement.textPosition(buttonElement.textPosition().add(-1, 0));
            dirty();
            return;
        }
        if (inside(local, Vec2i.of(34, 49), Vec2i.of(26, 14))) {
            controller.recordHistory();
            buttonElement.textPosition(buttonElement.textPosition().add(1, 0));
            dirty();
            return;
        }
        if (inside(local, Vec2i.of(64, 49), Vec2i.of(26, 14))) {
            controller.recordHistory();
            buttonElement.textPosition(buttonElement.textPosition().add(0, -1));
            dirty();
            return;
        }
        if (inside(local, Vec2i.of(94, 49), Vec2i.of(26, 14))) {
            controller.recordHistory();
            buttonElement.textPosition(buttonElement.textPosition().add(0, 1));
            dirty();
            return;
        }
        Color color = color(local);
        if (color == null) return;
        controller.recordHistory();
        controller.activeColor(color);
        buttonElement.textColor(color);
        dirty();
    }

    private void clickEntries(@NotNull VirtualElement<?> element, @NotNull DropdownElement dropdownElement, @NotNull Vec2i local) {
        if (inside(local, Vec2i.of(4, 23), Vec2i.of(26, 14))) {
            controller.recordHistory();
            dropdownElement.select(dropdownElement.selected() - 1);
            dirty();
            return;
        }
        if (inside(local, Vec2i.of(34, 23), Vec2i.of(26, 14))) {
            controller.recordHistory();
            dropdownElement.select(dropdownElement.selected() + 1);
            dirty();
            return;
        }
        if (inside(local, Vec2i.of(64, 23), Vec2i.of(34, 14))) {
            controller.recordHistory();
            dropdownElement.entry(Text.vanilla("Entry"));
            dropdownElement.select(dropdownElement.entries().size() - 1);
            dirty();
            return;
        }
        if (inside(local, Vec2i.of(102, 23), Vec2i.of(26, 14))) {
            controller.recordHistory();
            dropdownElement.removeEntry(dropdownElement.selected());
            dirty();
            return;
        }
        if (inside(local, Vec2i.of(4, 49), Vec2i.of(82, 14))) {
            start(Edit.DROPDOWN_ENTRY, element.identifier(), dropdownElement.selected());
            return;
        }
        Color color = color(local);
        if (color == null) return;
        controller.recordHistory();
        controller.activeColor(color);
        Text text = dropdownElement.entries().get(dropdownElement.selected());
        dropdownElement.entry(dropdownElement.selected(), Text.vanilla(text.content()).color(TextColor.color(color)));
        dirty();
    }

    private void clickEntries(@NotNull VirtualElement<?> element, @NotNull SelectorElement selectorElement, @NotNull Vec2i local) {
        if (inside(local, Vec2i.of(4, 23), Vec2i.of(26, 14))) {
            controller.recordHistory();
            selectorElement.select(selectorElement.selected() - 1);
            dirty();
            return;
        }
        if (inside(local, Vec2i.of(34, 23), Vec2i.of(26, 14))) {
            controller.recordHistory();
            selectorElement.select(selectorElement.selected() + 1);
            dirty();
            return;
        }
        if (inside(local, Vec2i.of(64, 23), Vec2i.of(34, 14))) {
            controller.recordHistory();
            selectorElement.entry(Text.vanilla("Entry"));
            selectorElement.select(selectorElement.entries().size() - 1);
            dirty();
            return;
        }
        if (inside(local, Vec2i.of(102, 23), Vec2i.of(26, 14))) {
            controller.recordHistory();
            selectorElement.removeEntry(selectorElement.selected());
            dirty();
            return;
        }
        if (inside(local, Vec2i.of(4, 49), Vec2i.of(82, 14))) {
            start(Edit.SELECTOR_ENTRY, element.identifier(), selectorElement.selected());
            return;
        }
        Color color = color(local);
        if (color == null) return;
        controller.recordHistory();
        controller.activeColor(color);
        Text text = selectorElement.entries().get(selectorElement.selected());
        selectorElement.entry(selectorElement.selected(), Text.vanilla(text.content()).color(TextColor.color(color)));
        dirty();
    }

    private void start(@NotNull Edit edit, @NotNull Identifier identifier, int index) {
        InputHandler handler = inputHandler();
        if (handler == null) return;
        this.edit = edit;
        this.editElement = identifier;
        this.editIndex = index;
        handler.peek(TextInputContext.class, TextInputContext::clear, handler.user());
        handler.anvil(true);
        handler.peek(MouseInputContext.class, old -> old.withLeftPressed(false), handler.user());
    }

    private @Nullable Color color(@NotNull Vec2i local) {
        for (int i = 0; i < colors.length; i++) {
            Vec2i pos = Vec2i.of(4 + i * 16, 91);
            if (inside(local, pos, Vec2i.of(12, 12))) return colors[i];
        }
        return null;
    }

    private boolean inside(@NotNull Vec2i local, @NotNull Vec2i position, @NotNull Vec2i size) {
        return local.x() >= position.x() && local.y() >= position.y() && local.x() < position.x() + size.x() && local.y() < position.y() + size.y();
    }

    private boolean focused() {
        VirtualElement<?> element = controller.selectedElement();
        if (element == null) return false;
        return element.target() instanceof ButtonElement || element.target() instanceof DropdownElement || element.target() instanceof SelectorElement;
    }

    private void dirty() {
        VirtualPage page = controller.selected();
        if (page != null) page.markDirty();
    }

    @Override
    public @NotNull ElementPanelElementListenerReferences listen() {
        return references;
    }

    @Override
    public @NotNull Canvas render(@NotNull Size property, @Nullable RenderContext context) {
        Vec2i size = PropertyHelper.vectorOrThrow(size(), Vec2i.class);
        Canvas canvas = Canvas.empty(size);
        VirtualElement<?> element = controller.selectedElement();
        if (element == null) return canvas;
        canvas.place(FrameElement.frame(size), Vec2i.zero());
        canvas.fill(Vec2i.of(1, 1), size.sub(2), Color.of(13, 13, 13));
        action(canvas, "Done", Vec2i.of(96, 4), true, context);
        if (element.target() instanceof ButtonElement buttonElement) {
            renderButton(canvas, buttonElement, context);
            return canvas;
        }
        if (element.target() instanceof DropdownElement dropdownElement) {
            renderEntries(canvas, "Dropdown", dropdownElement.entries(), dropdownElement.selected(), context);
            return canvas;
        }
        if (element.target() instanceof SelectorElement selectorElement) {
            renderEntries(canvas, "Selector", selectorElement.entries(), selectorElement.selected(), context);
            return canvas;
        }
        return Canvas.empty(size);
    }

    private void renderButton(@NotNull Canvas canvas, @NotNull ButtonElement element, @Nullable RenderContext context) {
        text(canvas, "Button", Vec2i.of(4, 4), Color.of(255, 255, 255), context);
        Text buttonText = element.text();
        field(canvas, buttonText == null ? "" : buttonText.content(), Vec2i.of(4, 23), context);
        text(canvas, "Text Pos " + element.textPosition().x() + "," + element.textPosition().y(), Vec2i.of(4, 39), Color.of(180, 180, 180), context);
        action(canvas, "X-", Vec2i.of(4, 49), true, context);
        action(canvas, "X+", Vec2i.of(34, 49), true, context);
        action(canvas, "Y-", Vec2i.of(64, 49), true, context);
        action(canvas, "Y+", Vec2i.of(94, 49), true, context);
        text(canvas, "Color", Vec2i.of(4, 79), Color.of(180, 180, 180), context);
        swatches(canvas);
    }

    private void renderEntries(@NotNull Canvas canvas, @NotNull String title, @NotNull List<Text> entries, int selected, @Nullable RenderContext context) {
        text(canvas, title, Vec2i.of(4, 4), Color.of(255, 255, 255), context);
        action(canvas, "<", Vec2i.of(4, 23), true, context);
        action(canvas, ">", Vec2i.of(34, 23), true, context);
        action(canvas, "Add", Vec2i.of(64, 23), true, context);
        action(canvas, "Del", Vec2i.of(102, 23), entries.size() > 1, context);
        String value = entries.isEmpty() ? "" : entries.get(Math.clamp(selected, 0, entries.size() - 1)).content();
        text(canvas, (selected + 1) + "/" + entries.size(), Vec2i.of(4, 39), Color.of(180, 180, 180), context);
        field(canvas, value, Vec2i.of(4, 49), context);
        text(canvas, "Color", Vec2i.of(4, 79), Color.of(180, 180, 180), context);
        swatches(canvas);
    }

    private void action(@NotNull Canvas canvas, @NotNull String label, @NotNull Vec2i position, boolean active, @Nullable RenderContext context) {
        Color background = active ? Color.of(39, 39, 39) : Color.of(20, 20, 20);
        Color textColor = active ? Color.of(255, 255, 255) : Color.of(93, 93, 93);
        int width = label.length() > 2 ? 34 : 26;
        canvas.fill(position, Vec2i.of(width, 14), background);
        canvas.fill(position.add(1, 1), Vec2i.of(width - 2, 12), Color.of(13, 13, 13));
        text(canvas, label, position.add(label.length() > 2 ? 4 : 7, 3), textColor, context);
    }

    private void field(@NotNull Canvas canvas, @NotNull String value, @NotNull Vec2i position, @Nullable RenderContext context) {
        canvas.fill(position, Vec2i.of(82, 14), Color.of(39, 39, 39));
        canvas.fill(position.add(1, 1), Vec2i.of(80, 12), Color.of(20, 20, 20));
        String text = value.length() > 11 ? value.substring(0, 11) : value;
        text(canvas, text, position.add(3, 3), Color.of(255, 255, 255), context);
    }

    private void swatches(@NotNull Canvas canvas) {
        for (int i = 0; i < colors.length; i++) {
            swatch(canvas, Vec2i.of(4 + i * 16, 91), colors[i]);
        }
    }

    private void swatch(@NotNull Canvas canvas, @NotNull Vec2i position, @NotNull Color color) {
        boolean active = color.rgba() == controller.activeColor().rgba();
        canvas.fill(position.sub(1, 1), Vec2i.of(14, 14), active ? Color.of(255, 255, 255) : Color.of(39, 39, 39));
        canvas.fill(position, Vec2i.of(12, 12), color);
    }

    private void text(@NotNull Canvas canvas, @NotNull String value, @NotNull Vec2i position, @NotNull Color color, @Nullable RenderContext context) {
        Canvas text = Text.vanilla(value).color(TextColor.color(color)).render(Size.fixed(Vec2i.of(120, 12)), context);
        canvas.place(text, position);
    }

    private enum Edit {
        BUTTON_TEXT,
        DROPDOWN_ENTRY,
        SELECTOR_ENTRY
    }

    public static final class ElementPanelElementListenerReferences extends ListenerReferences<ElementPanelElement, ElementPanelElementListenerReferences> {
        private final @NotNull ElementPanelElement back;

        public ElementPanelElementListenerReferences(@NotNull ElementPanelElement back) {
            this.back = back;
        }

        @Override
        public @NotNull ElementPanelElement back() {
            return back;
        }
    }
}
