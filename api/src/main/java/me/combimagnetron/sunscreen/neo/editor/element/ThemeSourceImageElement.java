package me.combimagnetron.sunscreen.neo.editor.element;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.passport.util.math.Vec2i;
import me.combimagnetron.sunscreen.SunscreenLibrary;
import me.combimagnetron.sunscreen.neo.editor.project.EditorAssetStore;
import me.combimagnetron.sunscreen.neo.editor.project.EditorThemeBuilderState;
import me.combimagnetron.sunscreen.neo.element.GenericInteractableModernElement;
import me.combimagnetron.sunscreen.neo.graphic.Canvas;
import me.combimagnetron.sunscreen.neo.graphic.color.Color;
import me.combimagnetron.sunscreen.neo.event.UserMoveStateChangeEvent;
import me.combimagnetron.sunscreen.neo.input.InputHandler;
import me.combimagnetron.sunscreen.neo.input.ListenerReferences;
import me.combimagnetron.sunscreen.neo.input.context.MouseInputContext;
import me.combimagnetron.sunscreen.neo.property.Size;
import me.combimagnetron.sunscreen.neo.render.engine.context.RenderContext;
import me.combimagnetron.sunscreen.util.helper.PropertyHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class ThemeSourceImageElement extends GenericInteractableModernElement<ThemeSourceImageElement, Canvas, ThemeSourceImageElement.References> {
    private final References references = new References(this);
    private final EditorThemeBuilderState state;
    private boolean pressed;

    public ThemeSourceImageElement(@NotNull Identifier identifier, @NotNull EditorThemeBuilderState state) {
        super(identifier);
        this.state = state;
    }

    @Override
    protected void lateInit() {
        super.lateInit();
        InputHandler handler = inputHandler();
        if (handler == null) return;
        handler.subscribe(identifier(), MouseInputContext.class, this::handleCursor);
    }

    private void handleCursor(@NotNull UserMoveStateChangeEvent event) {
            boolean left = event.context().leftPressed();
            if (!left) {
                pressed = false;
                return;
            }
            if (pressed) return;
            pressed = true;
            Canvas source = source();
            if (source == null) return;
            Vec2i local = event.context().position().sub(PropertyHelper.vectorOrThrow(position(), Vec2i.class));
            float scale = scale(source.size(), PropertyHelper.vectorOrThrow(size(), Vec2i.class));
            Vec2i image = Vec2i.of((int) (local.x() / scale), (int) (local.y() / scale));
            state.click(image, source.size());
    }

    @Override
    public @NotNull References listen() {
        return references;
    }

    @Override
    public @NotNull Canvas render(@NotNull Size property, @Nullable RenderContext context) {
        Vec2i size = PropertyHelper.vectorOrThrow(size(), Vec2i.class);
        Canvas canvas = Canvas.empty(size);
        canvas.fill(Vec2i.zero(), size, Color.of(13, 13, 13));
        Canvas source = source();
        if (source == null) return canvas.text(me.combimagnetron.sunscreen.neo.graphic.text.Text.vanilla("Drop an image first"), Vec2i.of(8, 8));
        float scale = scale(source.size(), size);
        Canvas scaled = source.scale(scale);
        canvas.place(scaled, Vec2i.zero());
        drawRegion(canvas, scale);
        return canvas;
    }

    private void drawRegion(@NotNull Canvas canvas, float scale) {
        Vec2i position = scaled(state.regionPosition(), scale);
        Vec2i size = scaled(state.regionSize(), scale);
        line(canvas, position, Vec2i.of(size.x(), 1), Color.of(35, 144, 99));
        line(canvas, position.add(0, size.y() - 1), Vec2i.of(size.x(), 1), Color.of(35, 144, 99));
        line(canvas, position, Vec2i.of(1, size.y()), Color.of(35, 144, 99));
        line(canvas, position.add(size.x() - 1, 0), Vec2i.of(1, size.y()), Color.of(35, 144, 99));
        line(canvas, position.add(0, Math.max(0, (int) (state.top() * scale))), Vec2i.of(size.x(), 1), Color.of(255, 255, 255));
        line(canvas, position.add(0, Math.max(0, size.y() - (int) (state.bottom() * scale))), Vec2i.of(size.x(), 1), Color.of(255, 255, 255));
        line(canvas, position.add(Math.max(0, (int) (state.left() * scale)), 0), Vec2i.of(1, size.y()), Color.of(255, 255, 255));
        line(canvas, position.add(Math.max(0, size.x() - (int) (state.right() * scale)), 0), Vec2i.of(1, size.y()), Color.of(255, 255, 255));
    }

    private void line(@NotNull Canvas canvas, @NotNull Vec2i position, @NotNull Vec2i size, @NotNull Color color) {
        canvas.fill(position, size, color);
    }

    private @NotNull Vec2i scaled(@NotNull Vec2i value, float scale) {
        return Vec2i.of((int) (value.x() * scale), (int) (value.y() * scale));
    }

    private float scale(@NotNull Vec2i source, @NotNull Vec2i target) {
        return Math.min((float) target.x() / Math.max(1, source.x()), (float) target.y() / Math.max(1, source.y()));
    }

    private @Nullable Canvas source() {
        try {
            for (EditorAssetStore.AssetData asset : EditorAssetStore.assets()) {
                if (!asset.type().equals("image")) continue;
                if (state.asset() == null) state.asset(asset.identifier(), Canvas.file(SunscreenLibrary.library().path().resolve("assets").resolve(asset.file())).size());
                if (!asset.identifier().equals(state.asset())) continue;
                return Canvas.file(SunscreenLibrary.library().path().resolve("assets").resolve(asset.file()));
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    public static final class References extends ListenerReferences<ThemeSourceImageElement, References> {
        private final ThemeSourceImageElement back;

        public References(@NotNull ThemeSourceImageElement back) {
            this.back = back;
        }

        @Override
        public @NotNull ThemeSourceImageElement back() {
            return back;
        }
    }
}
