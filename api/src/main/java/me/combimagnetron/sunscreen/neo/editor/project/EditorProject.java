package me.combimagnetron.sunscreen.neo.editor.project;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.passport.util.math.Vec2i;
import me.combimagnetron.sunscreen.neo.editor.element.PixelArtElement;
import me.combimagnetron.sunscreen.neo.editor.virtual.VirtualElement;
import me.combimagnetron.sunscreen.neo.editor.virtual.VirtualPage;
import me.combimagnetron.sunscreen.neo.element.ModernElement;
import me.combimagnetron.sunscreen.neo.element.impl.ButtonElement;
import me.combimagnetron.sunscreen.neo.element.impl.DropdownElement;
import me.combimagnetron.sunscreen.neo.element.impl.SelectorElement;
import me.combimagnetron.sunscreen.neo.graphic.Canvas;
import me.combimagnetron.sunscreen.neo.graphic.color.Color;
import me.combimagnetron.sunscreen.neo.graphic.text.Text;
import me.combimagnetron.sunscreen.neo.graphic.text.style.impl.color.TextColor;
import me.combimagnetron.sunscreen.neo.property.Decorator;
import me.combimagnetron.sunscreen.neo.property.Size;
import me.combimagnetron.sunscreen.neo.theme.decorator.Target;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EditorProject implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final String displayName;
    private final Identifier identifier;
    private final List<PageData> pages = new ArrayList<>();
    private Identifier themeId = Identifier.of("sunscreen", "theme/modern");
    private Identifier selectedPage;

    public EditorProject(String displayName, Identifier identifier) {
        this.displayName = displayName;
        this.identifier = identifier;
    }

    public @NotNull String displayName() {
        return displayName;
    }

    public @NotNull Identifier identifier() {
        return identifier;
    }

    public @NotNull Identifier themeId() {
        return themeId;
    }

    public @NotNull EditorProject themeId(@NotNull Identifier themeId) {
        this.themeId = themeId;
        return this;
    }

    public @NotNull List<PageData> pages() {
        return pages;
    }

    public @Nullable Identifier selectedPage() {
        return selectedPage;
    }

    public @NotNull EditorProject selectedPage(@Nullable Identifier selectedPage) {
        this.selectedPage = selectedPage;
        return this;
    }

    public @NotNull EditorProject page(@NotNull PageData page) {
        pages.add(page);
        return this;
    }

    public @Nullable PageData selected() {
        if (pages.isEmpty()) return null;
        if (selectedPage == null) return pages.getFirst();
        for (PageData page : pages) {
            if (page.identifier().equals(selectedPage)) return page;
        }
        return pages.getFirst();
    }

    public @Nullable PageData page(@NotNull String key) {
        for (PageData page : pages) {
            if (page.identifier().key().string().equals(key) || page.displayName().equalsIgnoreCase(key)) return page;
        }
        return null;
    }

    public record PageData(@NotNull Identifier identifier, @NotNull String displayName, @NotNull Vec2i position,
                           @NotNull Vec2i size, @NotNull List<ElementData> elements,
                           @Nullable PageViewData view) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    public record PageViewData(@NotNull String mode, int x, int y, int anchorX, int anchorY) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        public static @NotNull PageViewData fixed(@NotNull Vec2i position) {
            return new PageViewData("fixed", position.x(), position.y(), 0, 0);
        }

        public static @NotNull PageViewData center() {
            return new PageViewData("center", 0, 0, 50, 50);
        }

        public static @NotNull PageViewData percent(int x, int y, int anchorX, int anchorY) {
            return new PageViewData("percent", x, y, anchorX, anchorY);
        }

        public @NotNull Vec2i position(@NotNull Vec2i viewport, @NotNull Vec2i size) {
            if (mode.equals("fixed")) return Vec2i.of(x, y);
            if (mode.equals("center"))
                return Vec2i.of((viewport.x() - size.x()) / 2 + x, (viewport.y() - size.y()) / 2 + y);
            return Vec2i.of(viewport.x() * x / 100 - size.x() * anchorX / 100, viewport.y() * y / 100 - size.y() * anchorY / 100);
        }
    }

    public record ElementData(@NotNull Identifier identifier, @NotNull String type, @NotNull Vec2i position,
                              @NotNull Vec2i size, @Nullable String text, @NotNull Vec2i textPosition, int textColor,
                              int selected, int height, @NotNull List<EntryData> entries, int pixelScale,
                              @NotNull Vec2i canvasSize, int @NotNull [] pixels) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        public @Nullable ModernElement<?, Canvas> element() {
            if (type.equals("button")) {
                ButtonElement buttonElement = new ButtonElement(identifier, text == null ? null : Text.vanilla(text), textPosition).textColor(Color.rgba(textColor));
                buttonElement.size(Size.fixed(size));
                return buttonElement;
            }
            if (type.equals("dropdown")) {
                DropdownElement dropdownElement = new DropdownElement(identifier, height);
                for (EntryData entry : entries)
                    dropdownElement.entry(Text.vanilla(entry.text()).color(TextColor.color(Color.rgba(entry.color()))));
                dropdownElement.select(selected).size(Size.fixed(size)).decorator(Decorator.decorator(Target.typed(SelectorElement.class)));
                return dropdownElement;
            }
            if (type.equals("selector")) {
                SelectorElement selectorElement = new SelectorElement(identifier, height);
                for (EntryData entry : entries)
                    selectorElement.entry(Text.vanilla(entry.text()).color(TextColor.color(Color.rgba(entry.color()))));
                selectorElement.select(selected).size(Size.fixed(size));
                return selectorElement;
            }
            if (type.equals("pixel_art")) {
                return new PixelArtElement(identifier, canvasSize).pixels(pixels).layout(Size.fixed(size), pixelScale);
            }
            return null;
        }

        public static @NotNull ElementData of(@NotNull VirtualElement<?> element) {
            ModernElement<?, Canvas> target = element.target();
            if (target instanceof ButtonElement buttonElement) {
                Text text = buttonElement.text();
                return new ElementData(element.identifier(), "button", element.position(), element.size(), text == null ? null : text.content(), buttonElement.textPosition(), buttonElement.textColor().rgba(), 0, 0, List.of(), 1, Vec2i.of(1, 1), new int[1]);
            }
            if (target instanceof DropdownElement dropdownElement) {
                return new ElementData(element.identifier(), "dropdown", element.position(), element.size(), null, Vec2i.zero(), 0, dropdownElement.selected(), dropdownElement.height(), entries(dropdownElement.entries()), 1, Vec2i.of(1, 1), new int[1]);
            }
            if (target instanceof SelectorElement selectorElement) {
                return new ElementData(element.identifier(), "selector", element.position(), element.size(), null, Vec2i.zero(), 0, selectorElement.selected(), selectorElement.height(), entries(selectorElement.entries()), 1, Vec2i.of(1, 1), new int[1]);
            }
            if (target instanceof PixelArtElement pixelArtElement) {
                return new ElementData(element.identifier(), "pixel_art", element.position(), element.size(), null, Vec2i.zero(), 0, 0, 0, List.of(), pixelArtElement.pixelScale(), pixelArtElement.canvasSize(), pixelArtElement.pixels());
            }
            return new ElementData(element.identifier(), "unknown", element.position(), element.size(), null, Vec2i.zero(), 0, 0, 0, List.of(), 1, Vec2i.of(1, 1), new int[1]);
        }

        private static @NotNull List<EntryData> entries(@NotNull List<Text> texts) {
            List<EntryData> entries = new ArrayList<>();
            for (Text text : texts)
                entries.add(new EntryData(text.content(), Color.of(text.color().red(), text.color().green(), text.color().blue(), text.color().alpha()).rgba()));
            return entries;
        }
    }

    public record EntryData(@NotNull String text, int color) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    public static @NotNull PageData page(@NotNull VirtualPage page, @NotNull Vec2i position, @Nullable PageViewData view) {
        List<ElementData> elements = new ArrayList<>();
        for (VirtualElement<?> element : page.children()) {
            ElementData data = ElementData.of(element);
            if (!data.type().equals("unknown")) elements.add(data);
        }
        return new PageData(page.identifier(), page.displayName(), position, page.size(), elements, view);
    }
}
