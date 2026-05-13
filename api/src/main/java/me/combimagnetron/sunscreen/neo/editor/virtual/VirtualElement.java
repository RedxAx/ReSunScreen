package me.combimagnetron.sunscreen.neo.editor.virtual;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.passport.util.math.Vec2i;
import me.combimagnetron.sunscreen.neo.element.ElementContainer;
import me.combimagnetron.sunscreen.neo.element.ModernElement;
import me.combimagnetron.sunscreen.neo.graphic.Canvas;
import me.combimagnetron.sunscreen.neo.property.PropertyMap;
import me.combimagnetron.sunscreen.neo.property.Size;
import me.combimagnetron.sunscreen.neo.render.engine.context.RenderContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.List;

public class VirtualElement<M extends ModernElement<M, Canvas>> implements VirtualObject<M> {
    private final PropertyMap propertyMap = new PropertyMap();
    private final List<VirtualElement<?>> children = new ArrayList<>();
    private Vec2i position;
    private final M target;
    private final Identifier identifier;
    private final boolean isElementGroup;

    public VirtualElement(Vec2i position, @UnknownNullability ModernElement<?, Canvas> target, Identifier identifier) {
        this.position = position;
        this.target = (M) target;
        this.identifier = identifier;
        this.isElementGroup = ElementContainer.class.isAssignableFrom(target.getClass());

    }

    public synchronized @NotNull VirtualElement<M> add(@NotNull VirtualElement<?> child) {
        if (!isElementGroup) throw new IllegalArgumentException("May only add a child element to element groups!");
        this.children.add(child);
        return this;
    }

    public synchronized @NotNull Canvas render(@NotNull Vec2i size, @NotNull RenderContext context) {
        Canvas canvas;
        try {
            canvas = target.render(Size.fixed(size), context);
        } catch (RuntimeException ignored) {
            canvas = Canvas.error(Size.fixed(size));
        }
        for (VirtualElement<?> child : children) {
            canvas.place(child.render(child.size(), context), child.position());
        }
        return canvas;
    }

    public @NotNull Identifier identifier() {
        return identifier;
    }

    public synchronized @NotNull Vec2i size() {
        return target.size().value();
    }

    public synchronized @NotNull Vec2i position() {
        return position;
    }

    public synchronized @NotNull VirtualElement<M> position(@NotNull Vec2i position) {
        this.position = position;
        return this;
    }

    public synchronized @NotNull VirtualElement<M> size(@NotNull Vec2i size) {
        target.size(Size.fixed(size));
        return this;
    }

    public @NotNull M target() {
        return target;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public boolean isElementGroup() {
        return isElementGroup;
    }

}
