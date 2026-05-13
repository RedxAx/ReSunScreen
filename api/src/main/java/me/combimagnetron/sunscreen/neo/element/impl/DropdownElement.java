package me.combimagnetron.sunscreen.neo.element.impl;

import me.combimagnetron.passport.event.Dispatcher;
import me.combimagnetron.passport.event.Event;
import me.combimagnetron.passport.event.EventBus;
import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.passport.util.math.Vec2i;
import me.combimagnetron.sunscreen.neo.cursor.CursorStyle;
import me.combimagnetron.sunscreen.neo.element.GenericInteractableModernElement;
import me.combimagnetron.sunscreen.neo.event.UserClickElementEvent;
import me.combimagnetron.sunscreen.neo.event.UserMoveStateChangeEvent;
import me.combimagnetron.sunscreen.neo.graphic.Canvas;
import me.combimagnetron.sunscreen.neo.graphic.text.Text;
import me.combimagnetron.sunscreen.neo.input.InputHandler;
import me.combimagnetron.sunscreen.neo.input.ListenerReferences;
import me.combimagnetron.sunscreen.neo.input.context.MouseInputContext;
import me.combimagnetron.sunscreen.neo.property.Size;
import me.combimagnetron.sunscreen.neo.property.Visibility;
import me.combimagnetron.sunscreen.neo.render.engine.context.RenderContext;
import me.combimagnetron.sunscreen.neo.theme.decorator.ThemeDecorator;
import me.combimagnetron.sunscreen.util.helper.HoverHelper;
import me.combimagnetron.sunscreen.util.helper.PropertyHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class DropdownElement extends GenericInteractableModernElement<DropdownElement, Canvas, DropdownElement.DropdownListenerReferences> {
    private final DropdownListenerReferences references = new DropdownListenerReferences(this);
    private final List<Text> entries = new LinkedList<>();
    private final int height;
    private CursorStyle style = CursorStyle.pointer();
    private int selected = 0;
    private int hovered = -1;
    private boolean pressed = false;
    private boolean open = false;

    public DropdownElement(@NotNull Identifier identifier, @NotNull List<Text> entries, int height) {
        super(identifier);
        this.entries.addAll(entries);
        this.height = height;
    }

    public DropdownElement(@NotNull Identifier identifier, int height) {
        super(identifier);
        this.height = height;
    }

    public @NotNull DropdownElement entry(@NotNull Text text) {
        this.entries.add(text);
        return this;
    }

    public @NotNull DropdownElement entry(int index, @NotNull Text text) {
        entries.set(index, text);
        return this;
    }

    public @NotNull DropdownElement removeEntry(int index) {
        if (entries.size() <= 1) return this;
        entries.remove(index);
        selected = Math.clamp(selected, 0, entries.size() - 1);
        hovered = -1;
        return this;
    }

    public @NotNull List<Text> entries() {
        return Collections.unmodifiableList(entries);
    }

    public int height() {
        return height;
    }

    public @NotNull DropdownElement select(int entry) {
        this.selected = Math.clamp(entry, 0, Math.max(0, entries.size() - 1));
        return this;
    }

    public int selected() {
        return selected;
    }

    public @NotNull DropdownElement unfolded(boolean open) {
        this.open = open;
        return this;
    }

    @Override
    protected void lateInit() {
        super.lateInit();
        InputHandler handler = inputHandler();
        if (handler == null) return;
        references.subscribe(handler);
        handler.subscribe(identifier(), MouseInputContext.class, this::handleCursor);
    }

    private boolean inBounds(@NotNull Vec2i cursor) {
        Vec2i posVec = PropertyHelper.vectorOrThrow(position(), Vec2i.class);
        if (!open) {
            if (cursor.y() - posVec.y() > height)
                return false;
        }
        return HoverHelper.in(this, cursor);
    }

    private void handleCursor(@NotNull UserMoveStateChangeEvent event) {
        if (event.user() != inputHandler().user())
            return;
        MouseInputContext context = event.context();
        Vec2i cursor = context.position();
        boolean leftPressed = context.leftPressed();
        if (!leftPressed)
            pressed = false;
        Visibility visibility = visibility();
        if (visibility.hide())
            return;
        InputHandler handler = inputHandler();
        if (!inBounds(cursor) && style == CursorStyle.click()) {
            handler.cursor(CursorStyle.pointer());
            style = CursorStyle.pointer();
        }
        if (!inBounds(cursor)) {
            if (open && leftPressed && !pressed) {
                pressed = true;
                open = false;
            }
            hovered = -1;
            return;
        }
        style = CursorStyle.click();
        handler.cursor(CursorStyle.click());
        Vec2i relative = cursor.sub(PropertyHelper.vectorOrThrow(position(), Vec2i.class));
        if (relative.y() < height) {
            hovered = -2;
            if (leftPressed && !pressed) {
                pressed = true;
                open = !open;
            }
            return;
        }
        if (!open)
            return;
        int index = indexAt(relative);
        hovered = index;
        if (index == -1)
            return;
        if (leftPressed && !pressed) {
            pressed = true;
            selected = index;
            open = false;
            Dispatcher.dispatcher().post(new UserClickElementEvent<>(handler.user(), this, relative));
        }
    }

    private int indexAt(@NotNull Vec2i relative) {
        int y = height;
        for (int i = 0; i < entries.size(); i++) {
            if (relative.y() >= y && relative.y() < y + height)
                return i;
            y += height;
        }
        return -1;
    }

    @Override
    public @NotNull DropdownListenerReferences listen() {
        return references;
    }

    @Override
    public @NotNull Canvas render(@NotNull Size property, @Nullable RenderContext context) {
        Size size = size();
        if (context == null)
            return Canvas.error(size);
        ThemeDecorator themeDecorator = context.decorator(this);
        if (!(themeDecorator instanceof ThemeDecorator.StateNineSliceThemeDecorator decorator))
            return Canvas.error(size);
        Vec2i sizeVec = PropertyHelper.vectorOrThrow(size, Vec2i.class);
        if (entries.isEmpty()) return Canvas.empty(Vec2i.of(sizeVec.x(), height));
        selected = Math.clamp(selected, 0, entries.size() - 1);
        Text selectedText = entries.get(selected);
        int totalHeight = open ? height + entries.size() * height : height;
        Canvas canvas = Canvas.empty(Vec2i.of(sizeVec.x(), totalHeight));
        ElementPhase headerPhase = hovered == -2 ? ElementPhase.HOVER : ElementPhase.DEFAULT;
        Canvas header = decorator.render(Size.fixed(Vec2i.of(sizeVec.x(), height)), context, headerPhase);
        Canvas renderedSelected = selectedText.render(Size.fixed(Vec2i.of(sizeVec.x(), height)), null).trim();
        Canvas textLayer = decorator.render(Size.fixed(Vec2i.of(sizeVec.x(), height)), context, headerPhase);
        textLayer.place(renderedSelected, Vec2i.of(2, 2));
        canvas.place(header, Vec2i.zero());
        canvas.place(textLayer, Vec2i.zero());
        if (!open)
            return canvas;
        int y = height;
        for (int i = 0; i < entries.size(); i++) {
            Text text = entries.get(i);
            Canvas renderedText = text.render(Size.fixed(Vec2i.of(sizeVec.x(), height)), null).trim();
            Canvas entry = decorator.render(Size.fixed(Vec2i.of(sizeVec.x(), height)), context, phase(i));
            entry.place(renderedText, Vec2i.of(2, 2));
            canvas.place(entry, Vec2i.of(0, y));
            y += height;
        }
        return canvas;
    }

    private @NotNull ElementPhase phase(int index) {
        if (index == hovered)
            return ElementPhase.HOVER;
        if (index == selected)
            return ElementPhase.CLICK;
        return ElementPhase.DEFAULT;
    }

    public static final class DropdownListenerReferences extends ListenerReferences<DropdownElement, DropdownListenerReferences> {
        private final DropdownElement element;

        public DropdownListenerReferences(DropdownElement element) {
            this.element = element;
        }

        @Override
        public @NotNull DropdownElement back() {
            return element;
        }

        @SuppressWarnings("unchecked")
        public @NotNull DropdownListenerReferences select(@NotNull Consumer<UserClickElementEvent<?>> eventConsumer) {
            put(UserClickElementEvent.class, eventConsumer);
            return this;
        }

    }

}
