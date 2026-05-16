package me.combimagnetron.sunscreen.neo.editor.template;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.passport.util.math.Vec2i;
import me.combimagnetron.sunscreen.neo.MenuRoot;
import me.combimagnetron.sunscreen.neo.MenuTemplate;
import me.combimagnetron.sunscreen.neo.editor.EditorController;
import me.combimagnetron.sunscreen.neo.editor.element.EditorElements;
import me.combimagnetron.sunscreen.neo.editor.element.ThemeSourceImageElement;
import me.combimagnetron.sunscreen.neo.editor.project.EditorAssetStore;
import me.combimagnetron.sunscreen.neo.editor.project.PluginThemeDefinition;
import me.combimagnetron.sunscreen.neo.editor.project.PluginThemeStore;
import me.combimagnetron.sunscreen.neo.editor.project.ThemeComponentDefinition;
import me.combimagnetron.sunscreen.neo.element.Elements;
import me.combimagnetron.sunscreen.neo.element.impl.ButtonElement;
import me.combimagnetron.sunscreen.neo.graphic.Canvas;
import me.combimagnetron.sunscreen.neo.graphic.color.Color;
import me.combimagnetron.sunscreen.neo.graphic.shape.Shape;
import me.combimagnetron.sunscreen.neo.graphic.text.Text;
import me.combimagnetron.sunscreen.neo.graphic.text.style.impl.color.TextColor;
import me.combimagnetron.sunscreen.neo.graphic.text.style.impl.font.FontProperties;
import me.combimagnetron.sunscreen.neo.property.Decorator;
import me.combimagnetron.sunscreen.neo.property.Position;
import me.combimagnetron.sunscreen.neo.property.Size;
import me.combimagnetron.sunscreen.neo.registry.Registries;
import me.combimagnetron.sunscreen.neo.theme.decorator.Target;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class EditorThemeBuilderMenuTemplate implements MenuTemplate {
    private static final Identifier IDENTIFIER = Identifier.of("sunscreen", "internal/editor/theme_builder");
    private static final Vec2i SCREEN = Vec2i.of(800, 450);
    private final EditorController controller;

    public EditorThemeBuilderMenuTemplate(@NotNull EditorController controller) {
        this.controller = controller;
    }

    @Override
    public @NotNull Identifier identifier() {
        return IDENTIFIER;
    }

    @Override
    public void build(@NotNull MenuRoot root) {
        root.theme(EditorMenuTemplate.EDITOR_THEME);
        root.element(
                Elements.shape(Identifier.of("theme_builder/background"), Shape.rectangle(SCREEN), Color.of(8, 8, 8)).position(Position.nil()).size(Size.fixed(SCREEN))
        ).element(
                EditorElements.frame(Identifier.of("theme_builder/left_frame")).position(Position.fixed(Vec2i.of(14, 15))).size(Size.fixed(Vec2i.of(220, 420)))
        ).element(
                EditorElements.frame(Identifier.of("theme_builder/preview_frame")).position(Position.fixed(Vec2i.of(247, 15))).size(Size.fixed(Vec2i.of(539, 420)))
        ).element(
                Elements.label(Identifier.of("theme_builder/title"), vanilla("Theme Builder")).position(Position.fixed(Vec2i.of(18, 19))).size(Size.fixed(Vec2i.of(160, 12)))
        ).element(
                Elements.label(Identifier.of("theme_builder/base_label"), small("Plugin themes").color(TextColor.color(Color.of(180, 180, 180)))).position(Position.fixed(Vec2i.of(18, 45))).size(Size.fixed(Vec2i.of(160, 10)))
        ).element(
                Elements.image(Identifier.of("theme_builder/theme_panel"), themePanel()).position(Position.fixed(Vec2i.of(18, 58)))
        ).element(
                Elements.button(Identifier.of("theme_builder/create_theme"), vanilla("New Theme"), Vec2i.of(6, 2)).size(Size.fixed(Vec2i.of(90, 14))).position(Position.fixed(Vec2i.of(140, 42))).decorator(Decorator.decorator(Target.identifier(Identifier.of("sunscreen", "internal/editor/theme/decorator/button_confirm")))).listen().click(event -> {
                    if (!(event.element() instanceof ButtonElement)) return;
                    if (!event.element().identifier().key().string().equals("theme_builder/create_theme")) return;
                    try {
                        PluginThemeStore.createTheme();
                        event.menu().show(new EditorThemeBuilderMenuTemplate(controller));
                    } catch (IOException exception) {
                        event.user().message(Component.text("Failed to create theme."));
                    }
                }).back()
        ).element(
                Elements.button(Identifier.of("theme_builder/use_theme"), vanilla("Use"), Vec2i.of(8, 2)).size(Size.fixed(Vec2i.of(45, 14))).position(Position.fixed(Vec2i.of(185, 58))).listen().click(event -> {
                    if (!(event.element() instanceof ButtonElement)) return;
                    if (!event.element().identifier().key().string().equals("theme_builder/use_theme")) return;
                    try {
                        List<PluginThemeDefinition> themes = PluginThemeStore.themes();
                        if (themes.isEmpty()) {
                            event.user().message(Component.text("Create a theme first."));
                            return;
                        }
                        controller.recordHistory();
                        controller.theme(PluginThemeStore.compile(themes.getFirst()));
                        event.user().message(Component.text("Using " + themes.getFirst().displayName() + "."));
                    } catch (IOException exception) {
                        event.user().message(Component.text("Failed to use theme."));
                    }
                }).back()
        ).element(
                Elements.label(Identifier.of("theme_builder/assets_label"), small("Imported source images").color(TextColor.color(Color.of(180, 180, 180)))).position(Position.fixed(Vec2i.of(18, 126))).size(Size.fixed(Vec2i.of(160, 10)))
        ).element(
                Elements.image(Identifier.of("theme_builder/assets_panel"), assetPanel()).position(Position.fixed(Vec2i.of(18, 139)))
        ).element(
                Elements.label(Identifier.of("theme_builder/slots_label"), small("Create reusable element").color(TextColor.color(Color.of(180, 180, 180)))).position(Position.fixed(Vec2i.of(18, 260))).size(Size.fixed(Vec2i.of(160, 10)))
        ).element(
                Elements.image(Identifier.of("theme_builder/slots_panel"), slotsPanel()).position(Position.fixed(Vec2i.of(18, 273)))
        ).element(
                componentButton("theme_builder/component/button", "Button", "button", Vec2i.of(18, 385))
        ).element(
                componentButton("theme_builder/component/slider", "Slider", "slider", Vec2i.of(72, 385))
        ).element(
                componentButton("theme_builder/component/selector", "Selector", "selector", Vec2i.of(126, 385))
        ).element(
                componentButton("theme_builder/component/dropdown", "Dropdown", "dropdown", Vec2i.of(180, 385))
        ).element(
                componentButton("theme_builder/component/textfield", "Text", "textfield", Vec2i.of(18, 401))
        ).element(
                guideButton("theme_builder/guide/region", "Region", "region", Vec2i.of(258, 36))
        ).element(
                guideButton("theme_builder/guide/top", "Top", "top", Vec2i.of(314, 36))
        ).element(
                guideButton("theme_builder/guide/bottom", "Bottom", "bottom", Vec2i.of(370, 36))
        ).element(
                guideButton("theme_builder/guide/left", "Left", "left", Vec2i.of(426, 36))
        ).element(
                guideButton("theme_builder/guide/right", "Right", "right", Vec2i.of(482, 36))
        ).element(
                Elements.label(Identifier.of("theme_builder/preview_label"), small("Slice editor").color(TextColor.color(Color.of(180, 180, 180)))).position(Position.fixed(Vec2i.of(252, 20))).size(Size.fixed(Vec2i.of(160, 10)))
        ).element(
                new ThemeSourceImageElement(Identifier.of("theme_builder/source_image"), controller.themeBuilderState()).position(Position.fixed(Vec2i.of(258, 52))).size(Size.fixed(Vec2i.of(330, 260)))
        ).element(
                Elements.image(Identifier.of("theme_builder/editor_panel"), editorPanel()).position(Position.fixed(Vec2i.of(598, 52)))
        ).element(
                Elements.button(Identifier.of("theme_builder/back"), vanilla("Back"), Vec2i.of(8, 2)).size(Size.fixed(Vec2i.of(54, 11))).position(Position.fixed(Vec2i.of(18, 418))).listen().click(event -> {
                    if (!(event.element() instanceof ButtonElement)) return;
                    if (!event.element().identifier().key().string().equals("theme_builder/back")) return;
                    event.menu().show(new EditorMenuTemplate(controller));
                }).back()
        ).element(
                Elements.button(Identifier.of("theme_builder/save"), vanilla("Save"), Vec2i.of(8, 2)).size(Size.fixed(Vec2i.of(54, 11))).position(Position.fixed(Vec2i.of(176, 418))).decorator(Decorator.decorator(Target.identifier(Identifier.of("sunscreen", "internal/editor/theme/decorator/button_confirm")))).listen().click(event -> {
                    if (!(event.element() instanceof ButtonElement)) return;
                    if (!event.element().identifier().key().string().equals("theme_builder/save")) return;
                    event.user().message(Component.text("Plugin themes are saved automatically."));
                }).back()
        );
    }

    private @NotNull ButtonElement componentButton(@NotNull String key, @NotNull String label, @NotNull String type, @NotNull Vec2i position) {
        return Elements.button(Identifier.of(key), vanilla(label), Vec2i.of(2, 2))
                .size(Size.fixed(Vec2i.of(50, 12)))
                .position(Position.fixed(position))
                .listen().click(event -> {
                    if (!(event.element() instanceof ButtonElement)) return;
                    if (!event.element().identifier().key().string().equals(key)) return;
                    try {
                        List<PluginThemeDefinition> themes = PluginThemeStore.themes();
                        PluginThemeDefinition theme = themes.isEmpty() ? PluginThemeStore.createTheme() : themes.getFirst();
                        EditorAssetStore.AssetData asset = firstImageAsset();
                        if (asset == null) {
                            event.user().message(Component.text("Drop an image into Drop to Import first."));
                            return;
                        }
                        PluginThemeStore.createComponentFromSelection(theme, asset, controller.themeBuilderState(), type);
                        event.menu().show(new EditorThemeBuilderMenuTemplate(controller));
                    } catch (IOException exception) {
                        event.user().message(Component.text("Failed to create component."));
                    }
                }).back();
    }

    private @NotNull ButtonElement guideButton(@NotNull String key, @NotNull String label, @NotNull String mode, @NotNull Vec2i position) {
        Target<?> decorator = controller.themeBuilderState().mode().equals(mode)
                ? Target.identifier(Identifier.of("sunscreen", "internal/editor/theme/decorator/button_confirm"))
                : Target.typed(ButtonElement.class);
        return Elements.button(Identifier.of(key), vanilla(label), Vec2i.of(2, 2))
                .size(Size.fixed(Vec2i.of(52, 12)))
                .position(Position.fixed(position))
                .decorator(Decorator.decorator(decorator))
                .listen().click(event -> {
                    if (!(event.element() instanceof ButtonElement)) return;
                    if (!event.element().identifier().key().string().equals(key)) return;
                    controller.themeBuilderState().mode(mode);
                    event.menu().show(new EditorThemeBuilderMenuTemplate(controller));
                }).back();
    }

    private @NotNull Canvas themePanel() {
        Canvas canvas = Canvas.empty(Vec2i.of(210, 60));
        canvas.fill(Vec2i.zero(), canvas.size(), Color.of(27, 27, 27));
        canvas.fill(Vec2i.of(1, 1), canvas.size().sub(2), Color.of(13, 13, 13));
        try {
            List<PluginThemeDefinition> themes = PluginThemeStore.themes();
            if (themes.isEmpty()) {
                canvas.text(small("No plugin themes yet"), Vec2i.of(5, 6));
                canvas.text(small("Use New Theme"), Vec2i.of(5, 20));
                return canvas;
            }
            int y = 6;
            for (int i = 0; i < Math.min(3, themes.size()); i++) {
                PluginThemeDefinition theme = themes.get(i);
                canvas.text(vanilla(trim(theme.displayName(), 22)), Vec2i.of(5, y));
                canvas.text(small(theme.components().size() + " reusable components").color(TextColor.color(Color.of(180, 180, 180))), Vec2i.of(5, y + 10));
                y += 18;
            }
        } catch (IOException exception) {
            canvas.text(small("Theme library unavailable"), Vec2i.of(5, 6));
        }
        return canvas;
    }

    private @NotNull Canvas assetPanel() {
        Canvas canvas = Canvas.empty(Vec2i.of(210, 111));
        canvas.fill(Vec2i.zero(), canvas.size(), Color.of(27, 27, 27));
        canvas.fill(Vec2i.of(1, 1), canvas.size().sub(2), Color.of(13, 13, 13));
        try {
            List<EditorAssetStore.AssetData> assets = EditorAssetStore.assets();
            if (assets.isEmpty()) {
                canvas.text(small("No assets imported"), Vec2i.of(5, 6));
                canvas.text(small("Drop to Import"), Vec2i.of(5, 20));
                return canvas;
            }
            int y = 6;
            for (int i = 0; i < Math.min(6, assets.size()); i++) {
                EditorAssetStore.AssetData asset = assets.get(i);
                canvas.text(small(asset.role() + "  " + trim(asset.file(), 22)), Vec2i.of(5, y));
                y += 16;
            }
        } catch (IOException exception) {
            canvas.text(small("Asset index unavailable"), Vec2i.of(5, 6));
        }
        return canvas;
    }

    private @NotNull Canvas slotsPanel() {
        Canvas canvas = Canvas.empty(Vec2i.of(210, 104));
        canvas.fill(Vec2i.zero(), canvas.size(), Color.of(27, 27, 27));
        canvas.fill(Vec2i.of(1, 1), canvas.size().sub(2), Color.of(13, 13, 13));
        row(canvas, "Source region", "Select image area first", 6);
        row(canvas, "Slice guides", "Top / Bottom / Left / Right", 42);
        row(canvas, "States", "Normal / Hover / Selected / Active", 78);
        return canvas;
    }

    private @NotNull Canvas editorPanel() {
        Canvas canvas = Canvas.empty(Vec2i.of(178, 260));
        canvas.fill(Vec2i.zero(), canvas.size(), Color.of(27, 27, 27));
        canvas.fill(Vec2i.of(1, 1), canvas.size().sub(2), Color.of(13, 13, 13));
        try {
            List<PluginThemeDefinition> themes = PluginThemeStore.themes();
            if (themes.isEmpty()) {
                canvas.text(vanilla("Create a plugin theme first."), Vec2i.of(12, 12));
                return canvas;
            }
            PluginThemeDefinition theme = themes.getFirst();
            canvas.text(vanilla(theme.displayName()), Vec2i.of(8, 8));
            canvas.text(small("Mode: " + controller.themeBuilderState().mode()), Vec2i.of(8, 22));
            canvas.text(small("Region: " + controller.themeBuilderState().regionPosition().x() + "," + controller.themeBuilderState().regionPosition().y()
                    + " " + controller.themeBuilderState().regionSize().x() + "x" + controller.themeBuilderState().regionSize().y()), Vec2i.of(8, 36));
            canvas.text(small("Slice T" + controller.themeBuilderState().top() + " B" + controller.themeBuilderState().bottom()
                    + " L" + controller.themeBuilderState().left() + " R" + controller.themeBuilderState().right()), Vec2i.of(8, 50));
            int y = 72;
            for (ThemeComponentDefinition component : theme.components()) {
                canvas.fill(Vec2i.of(8, y), Vec2i.of(162, 44), Color.of(39, 39, 39));
                canvas.text(vanilla(component.displayName()), Vec2i.of(12, y + 6));
                canvas.text(small(component.type() + " states: " + component.states().keySet()).color(TextColor.color(Color.of(180, 180, 180))), Vec2i.of(12, y + 20));
                ThemeComponentDefinition.StateDefinition normal = component.states().get("normal");
                if (normal != null) {
                    canvas.text(small("region " + normal.position().x() + "," + normal.position().y() + " " + normal.size().x() + "x" + normal.size().y()), Vec2i.of(12, y + 32));
                }
                y += 50;
                if (y > 220) break;
            }
            if (theme.components().isEmpty()) {
                canvas.text(vanilla("Drop a source image, then mark it as a reusable element."), Vec2i.of(12, 34));
            }
        } catch (IOException exception) {
            canvas.text(vanilla("Theme library unavailable."), Vec2i.of(12, 12));
        }
        return canvas;
    }

    private EditorAssetStore.AssetData firstImageAsset() throws IOException {
        for (EditorAssetStore.AssetData asset : EditorAssetStore.assets()) {
            if (asset.type().equals("image")) return asset;
        }
        return null;
    }

    private void row(@NotNull Canvas canvas, @NotNull String title, @NotNull String value, int y) {
        canvas.fill(Vec2i.of(5, y), Vec2i.of(200, 28), Color.of(39, 39, 39));
        canvas.text(vanilla(title), Vec2i.of(9, y + 4));
        canvas.text(small(value).color(TextColor.color(Color.of(180, 180, 180))), Vec2i.of(9, y + 17));
    }

    private static @NotNull String trim(@NotNull String value, int length) {
        if (value.length() <= length) return value;
        return value.substring(0, length);
    }

    private static @NotNull Text vanilla(@NotNull String content) {
        return Text.basic(content).font(Registries.fonts().get(Identifier.of("sunscreen", "font/minecraft"))).fontProperties(FontProperties.properties().baseline(-2));
    }

    private static @NotNull Text small(@NotNull String content) {
        return Text.basic(content).font(Registries.fonts().get(Identifier.of("sunscreen", "font/sunburned"))).fontProperties(FontProperties.properties().baseline(-6));
    }
}
