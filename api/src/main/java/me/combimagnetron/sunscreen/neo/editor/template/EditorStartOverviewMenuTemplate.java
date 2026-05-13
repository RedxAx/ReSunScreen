package me.combimagnetron.sunscreen.neo.editor.template;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.passport.util.math.Vec2i;
import me.combimagnetron.sunscreen.neo.ActiveMenu;
import me.combimagnetron.sunscreen.neo.MenuRoot;
import me.combimagnetron.sunscreen.neo.MenuTemplate;
import me.combimagnetron.sunscreen.neo.TestMenuTemplate;
import me.combimagnetron.sunscreen.neo.editor.EditorController;
import me.combimagnetron.sunscreen.neo.editor.element.EditorElements;
import me.combimagnetron.sunscreen.neo.editor.element.EditorNameElement;
import me.combimagnetron.sunscreen.neo.editor.project.EditorProject;
import me.combimagnetron.sunscreen.neo.editor.project.EditorProjectStore;
import me.combimagnetron.sunscreen.neo.element.Elements;
import me.combimagnetron.sunscreen.neo.element.ModernElement;
import me.combimagnetron.sunscreen.neo.element.impl.DropdownElement;
import me.combimagnetron.sunscreen.neo.element.impl.SelectorElement;
import me.combimagnetron.sunscreen.neo.graphic.Canvas;
import me.combimagnetron.sunscreen.neo.graphic.color.Color;
import me.combimagnetron.sunscreen.neo.graphic.shape.Shape;
import me.combimagnetron.sunscreen.neo.graphic.text.Text;
import me.combimagnetron.sunscreen.neo.graphic.text.style.impl.color.TextColor;
import me.combimagnetron.sunscreen.neo.graphic.text.style.impl.font.FontProperties;
import me.combimagnetron.sunscreen.neo.layout.Layout;
import me.combimagnetron.sunscreen.neo.property.Decorator;
import me.combimagnetron.sunscreen.neo.property.Position;
import me.combimagnetron.sunscreen.neo.property.Size;
import me.combimagnetron.sunscreen.neo.property.Visibility;
import me.combimagnetron.sunscreen.neo.registry.Registries;
import me.combimagnetron.sunscreen.neo.render.engine.context.RenderContext;
import me.combimagnetron.sunscreen.neo.theme.decorator.Target;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class EditorStartOverviewMenuTemplate implements MenuTemplate {
    private final static Identifier IDENTIFIER = Identifier.of("sunscreen", "internal/editor/start");
    private final static Vec2i SCREEN = Vec2i.of(800, 450);
    private final static Vec2i ORIGIN = Vec2i.of(14, 66);
    private final static Vec2i TILE = Vec2i.of(128, 88);
    private final EditorController controller;

    public EditorStartOverviewMenuTemplate(@NotNull EditorController controller) {
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
            Elements.shape(Identifier.of("project/browser/background"), Shape.rectangle(SCREEN), Color.of(8, 8, 8)).position(Position.nil()).size(Size.fixed(SCREEN))
        ).element(
            Elements.image(Identifier.of("project/browser/title"), Canvas.resource("projects_label.png")).position(Position.fixed(Vec2i.of(14, 14)))
        ).element(
            Elements.label(Identifier.of("project/browser/subtitle"), small("Create a new project or reopen a saved one.").color(TextColor.color(Color.of(180, 180, 180)))).position(Position.fixed(Vec2i.of(16, 49))).size(Size.fixed(Vec2i.of(260, 10)))
        ).element(
            EditorElements.frame(Identifier.of("project/browser/frame")).position(Position.fixed(Vec2i.of(14, 64))).size(Size.fixed(Vec2i.of(772, 372)))
        );
        newProject(root);
        savedProjects(root);
        wizard(root);
    }

    private void newProject(@NotNull MenuRoot root) {
        Vec2i position = ORIGIN;
        root.element(
            Elements.image(Identifier.of("project/new/card"), card("New Project", newPreview(), true)).position(Position.fixed(position))
        ).element(
            Elements.button(Identifier.of("project/new/open"), vanilla(""), Vec2i.zero()).canvas(Canvas.empty(TILE)).position(Position.fixed(position)).size(Size.fixed(TILE)).listen().click(event -> {
                if (!event.element().identifier().key().string().equals("project/new/open")) return;
                ActiveMenu menu = event.menu();
                menu.element(Identifier.of("new_project/wizard")).visibility(Visibility.visible());
            }).back()
        );
    }

    private void savedProjects(@NotNull MenuRoot root) {
        try {
            List<String> projects = EditorProjectStore.projects();
            int amount = Math.min(14, projects.size());
            for (int i = 0; i < amount; i++) {
                int index = i;
                String project = projects.get(i);
                EditorProject editorProject = EditorProjectStore.load(project);
                if (editorProject == null) continue;
                Vec2i position = position(i + 1);
                root.element(
                    Elements.image(Identifier.of("project/saved/card/" + i), card(editorProject.displayName(), preview(editorProject), false)).position(Position.fixed(position))
                ).element(
                    Elements.button(Identifier.of("project/saved/open/" + i), vanilla(""), Vec2i.zero()).canvas(Canvas.empty(TILE)).position(Position.fixed(position)).size(Size.fixed(TILE)).listen().click(event -> {
                        if (!event.element().identifier().key().string().equals("project/saved/open/" + index)) return;
                        try {
                            EditorProject loaded = EditorProjectStore.load(project);
                            if (loaded != null) controller.open(loaded);
                        } catch (IOException exception) {
                            event.user().message(Component.text("Failed to open project."));
                        }
                    }).back()
                );
            }
        } catch (IOException ignored) {
        }
    }

    private @NotNull Vec2i position(int index) {
        int column = index % 5;
        int row = index / 5;
        return ORIGIN.add(column * 144, row * 106);
    }

    private @NotNull Canvas card(@NotNull String title, @NotNull Canvas preview, boolean create) {
        Canvas canvas = Canvas.empty(TILE);
        canvas.fill(Vec2i.zero(), TILE, Color.of(39, 39, 39));
        canvas.fill(Vec2i.of(1, 1), TILE.sub(2), Color.of(13, 13, 13));
        canvas.fill(Vec2i.of(2, 2), Vec2i.of(TILE.x() - 4, 14), create ? Color.of(35, 144, 99) : Color.of(27, 27, 27));
        canvas.text(vanilla(trim(title, 16)), Vec2i.of(5, 4));
        canvas.place(preview, Vec2i.of(4, 20));
        return canvas;
    }

    private @NotNull Canvas newPreview() {
        Canvas canvas = Canvas.empty(Vec2i.of(120, 64));
        canvas.fill(Vec2i.zero(), canvas.size(), Color.of(20, 20, 20));
        canvas.fill(Vec2i.of(54, 19), Vec2i.of(12, 26), Color.of(35, 144, 99));
        canvas.fill(Vec2i.of(47, 26), Vec2i.of(26, 12), Color.of(35, 144, 99));
        canvas.text(vanilla("Create"), Vec2i.of(38, 49));
        return canvas;
    }

    private @NotNull Canvas preview(@NotNull EditorProject project) {
        Canvas canvas = Canvas.empty(Vec2i.of(120, 64));
        canvas.fill(Vec2i.zero(), canvas.size(), Color.of(255, 255, 255));
        EditorProject.PageData page = project.selected();
        if (page == null) return canvas;
        Canvas pageCanvas = Canvas.empty(page.size());
        pageCanvas.fill(Vec2i.zero(), page.size(), Color.of(255, 255, 255));
        RenderContext context = new RenderContext(null, null, List.of(EditorMenuTemplate.EDITOR_THEME));
        for (EditorProject.ElementData data : page.elements()) {
            ModernElement<?, Canvas> element = data.element();
            if (element == null) continue;
            pageCanvas.place(element.render(Size.fixed(data.size()), context), data.position());
        }
        float scale = Math.min(120f / Math.max(1, page.size().x()), 64f / Math.max(1, page.size().y()));
        Canvas scaled = pageCanvas.scale(scale);
        Vec2i offset = Vec2i.of(Math.max(0, (120 - scaled.size().x()) / 2), Math.max(0, (64 - scaled.size().y()) / 2));
        canvas.place(scaled, offset);
        return canvas;
    }

    private void wizard(@NotNull MenuRoot root) {
        root.element(
            Layout.group(
                Identifier.of("new_project/wizard"),
                EditorElements.frame(Identifier.of("new_project/wizard/frame")).size(Size.fixed(Vec2i.of(198, 261))).position(Position.fixed(Vec2i.of(0, 9))),
                Elements.image(Identifier.of("new_project/wizard/frame_extension"), Canvas.empty(Vec2i.of(198, 9)).fill(Vec2i.zero(), Vec2i.of(198, 9), Color.of(27, 27, 27))).position(Position.nil()),
                Elements.label(Identifier.of("new_project/wizard/label"), vanilla("New Project")).size(Size.fixed(Vec2i.of(143, 20))).position(Position.fixed(Vec2i.of(1, 1))),
                Elements.label(Identifier.of("new_project/wizard/display_name_label"), vanilla("Display name").color(TextColor.color(Color.of(180, 180, 180)))).size(Size.fixed(Vec2i.of(143, 20))).position(Position.fixed(Vec2i.of(2, 11))),
                Elements.label(Identifier.of("new_project/wizard/identifier_label"), vanilla("Identifier").color(TextColor.color(Color.of(180, 180, 180)))).size(Size.fixed(Vec2i.of(143, 20))).position(Position.fixed(Vec2i.of(2, 32))),
                EditorElements.nameElement(Identifier.of("new_project/wizard/name_element")).size(Size.fixed(Vec2i.of(193, 10))).position(Position.fixed(Vec2i.of(2, 20))),
                Elements.button(Identifier.of("new_project/wizard/confirm_button"), vanilla("Create"), Vec2i.of(31, 3)).listen().click(event -> {
                    if (!event.element().identifier().key().string().equals("new_project/wizard/confirm_button")) return;
                    Layout<?> layout = (Layout<?>) event.menu().element(Identifier.of("new_project/wizard"));
                    EditorNameElement nameElement = (EditorNameElement) layout.child(Identifier.of("new_project/wizard/name_element"));
                    if (!nameElement.validate()) return;
                    controller.editor(nameElement.fakeIdentifier(), nameElement.displayName(), TestMenuTemplate.THEME);
                }).back().size(Size.fixed(Vec2i.of(96, 14))).position(Position.fixed(Vec2i.of(100, 254))).decorator(Decorator.decorator(Target.identifier(Identifier.of("sunscreen", "internal/editor/theme/decorator/button_confirm")))),
                Elements.button(Identifier.of("new_project/wizard/cancel_button"), vanilla("Cancel").color(TextColor.color(Color.of(180, 180, 180))), Vec2i.of(31, 3)).listen().click(event -> {
                    if (!event.element().identifier().key().string().equals("new_project/wizard/cancel_button")) return;
                    Layout<?> layout = (Layout<?>) event.menu().element(Identifier.of("new_project/wizard"));
                    layout.visibility(Visibility.hidden());
                }).back().size(Size.fixed(Vec2i.of(96, 14))).position(Position.fixed(Vec2i.of(2, 254))),
                new DropdownElement(Identifier.of("new_project/wizard/theme_dropdown"), 11).entry(vanilla("Tropical")).entry(vanilla("Modern")).size(Size.fixed(Vec2i.of(193, 50))).position(Position.fixed(Vec2i.of(2, 61))).decorator(Decorator.decorator(Target.typed(SelectorElement.class))),
                Elements.label(Identifier.of("new_project/wizard/theme_label"), vanilla("Theme").color(TextColor.color(Color.of(180, 180, 180)))).size(Size.fixed(Vec2i.of(143, 20))).position(Position.fixed(Vec2i.of(2, 53)))
            ).size(Size.fixed(Vec2i.of(400, 400))).position(Position.fixed(Vec2i.of(301, 105))).visibility(Visibility.hidden())
        );
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
