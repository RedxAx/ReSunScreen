package me.combimagnetron.sunscreen.neo.editor.template;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.passport.util.math.Vec2i;
import me.combimagnetron.sunscreen.neo.MenuRoot;
import me.combimagnetron.sunscreen.neo.MenuTemplate;
import me.combimagnetron.sunscreen.neo.editor.project.EditorProject;
import me.combimagnetron.sunscreen.neo.editor.project.EditorThemes;
import me.combimagnetron.sunscreen.neo.element.Elements;
import me.combimagnetron.sunscreen.neo.element.ModernElement;
import me.combimagnetron.sunscreen.neo.graphic.Canvas;
import me.combimagnetron.sunscreen.neo.graphic.color.Color;
import me.combimagnetron.sunscreen.neo.graphic.shape.Shape;
import me.combimagnetron.sunscreen.neo.property.Position;
import me.combimagnetron.sunscreen.neo.property.Size;
import org.jetbrains.annotations.NotNull;

public class EditorProjectDisplayMenuTemplate implements MenuTemplate {
    private static final Vec2i VIEWPORT = Vec2i.of(800, 450);
    private final EditorProject project;
    private final String pageKey;

    public EditorProjectDisplayMenuTemplate(@NotNull EditorProject project) {
        this(project, "");
    }

    public EditorProjectDisplayMenuTemplate(@NotNull EditorProject project, @NotNull String pageKey) {
        this.project = project;
        this.pageKey = pageKey;
    }

    @Override
    public @NotNull Identifier identifier() {
        return Identifier.of("sunscreen", "project/" + project.identifier().key().string());
    }

    @Override
    public void build(@NotNull MenuRoot root) {
        root.theme(EditorThemes.theme(project.themeId()));
        EditorProject.PageData page = pageKey.isBlank() ? project.selected() : project.page(pageKey);
        if (page == null) return;
        EditorProject.PageViewData view = page.view() == null ? EditorProject.PageViewData.center() : page.view();
        Vec2i pagePosition = view.position(VIEWPORT, page.size());
        root.element(Elements.shape(Identifier.of("project/background"), Shape.rectangle(page.size()), Color.of(255, 255, 255)).position(Position.fixed(pagePosition)).size(Size.fixed(page.size())));
        for (EditorProject.ElementData data : page.elements()) {
            ModernElement<?, Canvas> element = data.element();
            if (element == null) continue;
            root.element(element.position(Position.fixed(pagePosition.add(data.position()))));
        }
    }
}
