package me.combimagnetron.sunscreen.neo.editor;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.passport.util.math.Vec2i;
import me.combimagnetron.sunscreen.neo.ActiveMenu;
import me.combimagnetron.sunscreen.neo.editor.element.MenuPreviewElement;
import me.combimagnetron.sunscreen.neo.editor.input.SelectorInputContext;
import me.combimagnetron.sunscreen.neo.editor.project.EditorProject;
import me.combimagnetron.sunscreen.neo.editor.project.EditorThemeBuilderState;
import me.combimagnetron.sunscreen.neo.editor.project.EditorThemes;
import me.combimagnetron.sunscreen.neo.editor.template.EditorMenuTemplate;
import me.combimagnetron.sunscreen.neo.editor.template.EditorStartOverviewMenuTemplate;
import me.combimagnetron.sunscreen.neo.editor.tool.Tool;
import me.combimagnetron.sunscreen.neo.editor.tool.ToolContext;
import me.combimagnetron.sunscreen.neo.editor.tool.Tools;
import me.combimagnetron.sunscreen.neo.editor.virtual.VirtualElement;
import me.combimagnetron.sunscreen.neo.editor.virtual.VirtualPage;
import me.combimagnetron.sunscreen.neo.editor.virtual.VirtualTheme;
import me.combimagnetron.sunscreen.neo.editor.virtual.argument.ElementConstructionProvider;
import me.combimagnetron.sunscreen.neo.editor.widget.EditorWidget;
import me.combimagnetron.sunscreen.neo.element.ElementLike;
import me.combimagnetron.sunscreen.neo.element.ModernElement;
import me.combimagnetron.sunscreen.neo.element.impl.ButtonElement;
import me.combimagnetron.sunscreen.neo.element.impl.DropdownElement;
import me.combimagnetron.sunscreen.neo.element.impl.SelectorElement;
import me.combimagnetron.sunscreen.neo.event.UserUpdateSelectorInputEvent;
import me.combimagnetron.sunscreen.neo.event.UserClickElementEvent;
import me.combimagnetron.sunscreen.neo.graphic.Canvas;
import me.combimagnetron.sunscreen.neo.graphic.color.Color;
import me.combimagnetron.sunscreen.neo.graphic.text.Text;
import me.combimagnetron.sunscreen.neo.input.InputHandler;
import me.combimagnetron.sunscreen.neo.input.context.TextInputContext;
import me.combimagnetron.sunscreen.neo.layout.Layout;
import me.combimagnetron.sunscreen.neo.render.engine.context.RenderContext;
import me.combimagnetron.sunscreen.neo.theme.ModernTheme;
import me.combimagnetron.sunscreen.neo.theme.decorator.ThemeDecorator;
import me.combimagnetron.sunscreen.user.SunscreenUser;
import me.combimagnetron.sunscreen.util.helper.editor.NameHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EditorController {
    private static final Map<UUID, EditorController> CONTROLLERS = new ConcurrentHashMap<>();
    private final LinkedHashMap<Identifier, VirtualPage> pages = new LinkedHashMap<>();
    private final LinkedHashMap<Identifier, Vec2i> pagePositions = new LinkedHashMap<>();
    private final LinkedHashMap<Identifier, EditorProject.PageViewData> pageViews = new LinkedHashMap<>();
    private final LinkedList<Identifier> pagesList = new LinkedList<>();
    private final ArrayDeque<EditorProject> undo = new ArrayDeque<>();
    private final ArrayDeque<EditorProject> redo = new ArrayDeque<>();
    private final SunscreenUser<?> user;
    private RenderContext renderContext = new RenderContext(null, null, List.of());
    private ElementConstructionProvider<?> pendingProvider;
    private VirtualTheme theme;
    private ActiveMenu active;
    private Identifier identifier = Identifier.of("project");
    private String displayName = "Project";
    private Identifier selectedPage;
    private Identifier selectedElement;
    private Tool tool = Tools.select();
    private MenuPreviewElement preview;
    private Color activeColor = Color.of(0, 0, 0);
    private InputHandler selectorInputHandler;
    private final EditorThemeBuilderState themeBuilderState = new EditorThemeBuilderState();
    private boolean publishingSelectionControls;
    private boolean historyChanging;

    public EditorController(SunscreenUser<?> user) {
        this.user = user;
    }

    public EditorController(SunscreenUser<?> user, @NotNull UUID uuid) {
        this(user);
        CONTROLLERS.put(uuid, this);
    }

    public static @Nullable EditorController controller(@NotNull UUID uuid) {
        return CONTROLLERS.get(uuid);
    }

    public void start() {
        active = user.open(new EditorStartOverviewMenuTemplate(this)).menu();
    }

    public void open(@NotNull EditorProject project) {
        if (active == null) return;
        pages.clear();
        pagePositions.clear();
        pageViews.clear();
        pagesList.clear();
        selectedPage = null;
        selectedElement = null;
        undo.clear();
        redo.clear();
        identifier = project.identifier();
        displayName = project.displayName();
        theme = null;
        editor(identifier, displayName, EditorThemes.theme(project.themeId()));
        pages.clear();
        pagePositions.clear();
        pageViews.clear();
        pagesList.clear();
        selectedPage = null;
        for (EditorProject.PageData pageData : project.pages()) {
            VirtualPage page = new VirtualPage(pageData.size(), pageData.identifier(), pageData.displayName(), this);
            for (EditorProject.ElementData elementData : pageData.elements()) {
                ModernElement<?, Canvas> element = elementData.element();
                if (element == null) continue;
                page.page(new VirtualElement<>(elementData.position(), element, elementData.identifier()));
            }
            page(page);
            pagePosition(page.identifier(), pageData.position());
            pageView(page.identifier(), pageData.view() == null ? EditorProject.PageViewData.center() : pageData.view());
        }
        selectPage(project.selectedPage());
        if (selectedPage == null && !pagesList.isEmpty()) select(0);
        active.show(new EditorMenuTemplate(this));
        bindSelectionControls();
    }

    private void restore(@NotNull EditorProject project) {
        pages.clear();
        pagePositions.clear();
        pageViews.clear();
        pagesList.clear();
        selectedPage = null;
        selectedElement = null;
        identifier = project.identifier();
        displayName = project.displayName();
        theme = null;
        theme(EditorThemes.theme(project.themeId()));
        for (EditorProject.PageData pageData : project.pages()) {
            VirtualPage page = new VirtualPage(pageData.size(), pageData.identifier(), pageData.displayName(), this);
            for (EditorProject.ElementData elementData : pageData.elements()) {
                ModernElement<?, Canvas> element = elementData.element();
                if (element == null) continue;
                page.page(new VirtualElement<>(elementData.position(), element, elementData.identifier()));
            }
            page(page);
            pagePosition(page.identifier(), pageData.position());
            pageView(page.identifier(), pageData.view() == null ? EditorProject.PageViewData.center() : pageData.view());
        }
        selectPage(project.selectedPage());
        if (selectedPage == null && !pagesList.isEmpty()) select(0);
        if (active != null) {
            active.show(new EditorMenuTemplate(this));
            bindSelectionControls();
        }
    }

    public void recordHistory() {
        if (historyChanging) return;
        undo.push(project());
        while (undo.size() > 100) undo.removeLast();
        redo.clear();
    }

    public void undo() {
        if (undo.isEmpty()) return;
        historyChanging = true;
        try {
            redo.push(project());
            restore(undo.pop());
        } finally {
            historyChanging = false;
        }
    }

    public void redo() {
        if (redo.isEmpty()) return;
        historyChanging = true;
        try {
            undo.push(project());
            restore(redo.pop());
        } finally {
            historyChanging = false;
        }
    }

    public @NotNull List<VirtualPage> pages() {
        return pagesList.stream().map(this::page).toList();
    }

    public void select(int index) {
        if (index < 0 || index >= pagesList.size()) return;
        select(pagesList.get(index));
    }

    public @NotNull RenderContext clearedContext() {
        return renderContext.clear();
    }

    public @NotNull RenderContext context() {
        return renderContext;
    }

    public @Nullable VirtualTheme theme() {
        return theme;
    }

    public @NotNull EditorThemeBuilderState themeBuilderState() {
        return themeBuilderState;
    }

    public @NotNull EditorController theme(@NotNull VirtualTheme theme) {
        this.theme = theme;
        renderContext = renderContext.withComponents(List.of(theme));
        return this;
    }

    public @NotNull EditorController theme(@NotNull ModernTheme theme) {
        VirtualTheme virtualTheme = new VirtualTheme(theme.identifier());
        if (theme.colorScheme() != null) virtualTheme.colorScheme(theme.colorScheme());
        for (ThemeDecorator decorator : theme.decorators()) {
            virtualTheme.decorator(decorator);
        }
        return theme(virtualTheme);
    }

    public @NotNull EditorController page(@NotNull VirtualPage page) {
        if (pages.containsKey(page.identifier())) {
            pages.put(page.identifier(), page);
            pagePositions.putIfAbsent(page.identifier(), Vec2i.zero());
            pageViews.putIfAbsent(page.identifier(), EditorProject.PageViewData.center());
            if (selectedPage == null) selectedPage = page.identifier();
            return this;
        }
        pages.put(page.identifier(), page);
        pagesList.add(page.identifier());
        pagePositions.putIfAbsent(page.identifier(), Vec2i.zero());
        pageViews.putIfAbsent(page.identifier(), EditorProject.PageViewData.center());
        if (selectedPage == null) selectedPage = page.identifier();
        return this;
    }

    public @NotNull VirtualPage page(@NotNull Vec2i position, @NotNull Vec2i size) {
        Identifier identifier = nextPageIdentifier();
        String displayName = NameHelper.suggestDisplayName(identifier.string());
        VirtualPage page = new VirtualPage(size, identifier, displayName, this);
        page(page);
        pagePosition(identifier, position);
        return page;
    }

    public @NotNull VirtualElement<?> element(@NotNull VirtualPage page, @NotNull ElementConstructionProvider<?> provider, @NotNull Vec2i position, @NotNull Vec2i size) {
        Identifier identifier = nextElementIdentifier(provider);
        ModernElement<?, Canvas> element = provider.constructDefault(identifier, size);
        VirtualElement<?> virtualElement = new VirtualElement<>(position, element, identifier);
        page.page(virtualElement);
        select(page);
        selectElement(virtualElement);
        return virtualElement;
    }

    public @Nullable VirtualPage selected() {
        if (selectedPage == null) return null;
        return page(selectedPage);
    }

    public @NotNull EditorController select(@NotNull Identifier identifier) {
        if (!pages.containsKey(identifier)) return this;
        selectedPage = identifier;
        selectedElement = null;
        hydrateSelection();
        return this;
    }

    public @NotNull EditorController select(@NotNull VirtualPage page) {
        return select(page.identifier());
    }

    public @NotNull EditorController selectPage(@Nullable Identifier identifier) {
        if (identifier != null && !pages.containsKey(identifier)) return this;
        selectedPage = identifier;
        if (identifier == null) selectedElement = null;
        hydrateSelection();
        return this;
    }

    public @NotNull EditorController selectElement(@Nullable VirtualElement<?> element) {
        if (element == null) {
            selectedElement = null;
            VirtualPage page = selected();
            if (page != null) page.clearSelection();
            hydrateSelection();
            return this;
        }
        selectedElement = element.identifier();
        hydrateSelection();
        return this;
    }

    public @Nullable VirtualElement<?> selectedElement() {
        VirtualPage page = selected();
        if (page == null || selectedElement == null) return null;
        return page.element(selectedElement);
    }

    public @Nullable VirtualPage page(@NotNull Identifier identifier) {
        return pages.get(identifier);
    }

    public @Nullable Vec2i pagePosition(@NotNull Identifier identifier) {
        return pagePositions.get(identifier);
    }

    public @NotNull EditorController pagePosition(@NotNull Identifier identifier, @NotNull Vec2i position) {
        pagePositions.put(identifier, position);
        return this;
    }

    public void deleteSelectedPage() {
        if (selectedPage == null) return;
        recordHistory();
        Identifier removed = selectedPage;
        pages.remove(removed);
        pagePositions.remove(removed);
        pageViews.remove(removed);
        pagesList.remove(removed);
        selectedElement = null;
        selectedPage = pagesList.isEmpty() ? null : pagesList.getFirst();
        hydratePageViewControls();
    }

    public void deleteSelectedElement() {
        VirtualPage page = selected();
        VirtualElement<?> element = selectedElement();
        if (page == null || element == null) return;
        recordHistory();
        page.remove(element.identifier());
        selectedElement = null;
        hydrateSelectionControls();
    }

    public void deleteSelected() {
        if (selectedElement() != null) {
            deleteSelectedElement();
        } else {
            deleteSelectedPage();
        }
    }

    public @NotNull EditorProject.PageViewData pageView(@NotNull Identifier identifier) {
        return pageViews.getOrDefault(identifier, EditorProject.PageViewData.center());
    }

    public @NotNull EditorController pageView(@NotNull Identifier identifier, @NotNull EditorProject.PageViewData view) {
        pageViews.put(identifier, view);
        return this;
    }

    public @NotNull Map<Identifier, Vec2i> pagePositions() {
        return pagePositions;
    }

    public void editor(@NotNull Identifier identifier, @NotNull String displayName, @NotNull ModernTheme theme) {
        undo.clear();
        redo.clear();
        this.identifier = identifier;
        this.displayName = displayName;
        theme(theme);
        if (pages.isEmpty()) page(Vec2i.of(320, 240), Vec2i.of(176, 128));
        if (active == null) return;
        active.show(new EditorMenuTemplate(this));
        bindSelectionControls();
    }

    public @NotNull Tool tool() {
        return tool;
    }

    public @NotNull EditorController tool(@NotNull Tool tool) {
        if (this.tool == tool) return this;
        Tool previous = this.tool;
        this.tool = tool;
        previous.cancel(toolContext());
        return this;
    }

    public @NotNull ToolContext toolContext() {
        return new ToolContext(this, Objects.requireNonNullElseGet(preview, () -> MenuPreviewElement.empty(this)), selected(), selectedElement(), pendingProvider);
    }

    public @Nullable ElementConstructionProvider<?> pendingProvider() {
        return pendingProvider;
    }

    public @NotNull EditorController pendingProvider(@Nullable ElementConstructionProvider<?> provider) {
        pendingProvider = provider;
        return this;
    }

    public @NotNull Identifier nextPageIdentifier() {
        int index = pages.size() + 1;
        Identifier identifier = Identifier.of("page_" + index);
        while (pages.containsKey(identifier)) {
            index++;
            identifier = Identifier.of("page_" + index);
        }
        return identifier;
    }

    public @NotNull Identifier nextElementIdentifier(@NotNull ElementConstructionProvider<?> provider) {
        String key = provider.identifier().key().string().replace("_element", "").toLowerCase(Locale.ROOT);
        int index = 1;
        Identifier identifier = Identifier.of(key + "_" + index);
        while (elementExists(identifier)) {
            index++;
            identifier = Identifier.of(key + "_" + index);
        }
        return identifier;
    }

    public @NotNull Color activeColor() {
        return activeColor;
    }

    public @NotNull EditorController activeColor(@NotNull Color activeColor) {
        this.activeColor = activeColor;
        return this;
    }

    public @NotNull EditorController preview(@NotNull MenuPreviewElement preview) {
        this.preview = preview;
        return this;
    }

    public @Nullable ActiveMenu active() {
        return active;
    }

    public @NotNull EditorProject project() {
        EditorProject project = new EditorProject(displayName, identifier)
                .themeId(theme == null ? EditorThemes.MODERN : theme.identifier())
                .selectedPage(selectedPage);
        for (VirtualPage page : pages()) {
            Vec2i position = pagePosition(page.identifier());
            project.page(EditorProject.page(page, position == null ? Vec2i.zero() : position, pageView(page.identifier())));
        }
        return project;
    }

    public @NotNull ActiveMenu menu() {
        if (active == null) throw new IllegalStateException("Editor menu is not active");
        return active;
    }

    public void hydrateSelectionControls() {
        ActiveMenu menu = active;
        VirtualElement<?> element = selectedElement();
        if (menu == null || element == null) return;
        menu.inputHandler().peek(TextInputContext.class, TextInputContext::clear, menu.user());
        publishSelectionControls(element.size(), element.position());
        hydratePageViewControls();
    }

    public void publishSelectionControls(@NotNull Vec2i size, @NotNull Vec2i position) {
        ActiveMenu menu = active;
        if (menu == null) return;
        publishingSelectionControls = true;
        try {
            menu.inputHandler().peek(SelectorInputContext.class,
                    old -> old.inner(Identifier.of("top_right/size/value"), new SelectorInputContext.ValueInnerContext(new Integer[]{size.x(), size.y(), size.y(), size.x()})),
                    menu.user()
            );
            menu.inputHandler().peek(SelectorInputContext.class,
                    old -> old.inner(Identifier.of("top_right/position/value"), new SelectorInputContext.ValueInnerContext(new Integer[]{position.x(), position.y(), position.y(), position.x()})),
                    menu.user()
            );
        } finally {
            publishingSelectionControls = false;
        }
    }

    private void hydrateSelection() {
        hydrateElementControls();
        hydratePageViewControls();
    }

    private void hydrateElementControls() {
        VirtualElement<?> element = selectedElement();
        if (element == null) return;
        if (element.target() instanceof ButtonElement buttonElement) {
            activeColor(buttonElement.textColor());
            return;
        }
        if (element.target() instanceof DropdownElement dropdownElement) {
            if (dropdownElement.entries().isEmpty()) return;
            activeColor(color(dropdownElement.entries().get(dropdownElement.selected())));
            return;
        }
        if (element.target() instanceof SelectorElement selectorElement) {
            if (selectorElement.entries().isEmpty()) return;
            activeColor(color(selectorElement.entries().get(selectorElement.selected())));
        }
    }

    private @NotNull Color color(@NotNull Text text) {
        return Color.of(text.color().red(), text.color().green(), text.color().blue(), text.color().alpha());
    }

    private void bindSelectionControls() {
        if (active == null || selectorInputHandler == active.inputHandler()) return;
        selectorInputHandler = active.inputHandler();
        selectorInputHandler.subscribe(Identifier.of("editor/selection_controls"), SelectorInputContext.class, this::selector);
        selectorInputHandler.subscribe(Identifier.of("editor/page_view_controls"), SelectorInputContext.class, this::pageViewSelector);
        selectorInputHandler.listen(Identifier.of("editor/page_view_mode"), UserClickElementEvent.class, this::pageViewMode);
        hydratePageViewControls();
    }

    private void hydratePageViewControls() {
        ActiveMenu menu = active;
        if (menu == null || selectedPage == null) return;
        EditorProject.PageViewData view = pageView(selectedPage);
        if (control(Identifier.of("low_right/page_view/mode")) instanceof SelectorElement mode)
            mode.select(pageViewMode(view));
        menu.inputHandler().peek(SelectorInputContext.class,
                old -> old
                        .inner(Identifier.of("low_right/page_view/position"), new SelectorInputContext.ValueInnerContext(new Integer[]{view.y(), view.y(), view.x(), view.x()}))
                        .inner(Identifier.of("low_right/page_view/anchor"), new SelectorInputContext.ValueInnerContext(new Integer[]{view.anchorY(), view.anchorY(), view.anchorX(), view.anchorX()})),
                menu.user()
        );
    }

    private @Nullable ModernElement<?, Canvas> control(@NotNull Identifier identifier) {
        ActiveMenu menu = active;
        if (menu == null) return null;
        Set<Identifier> visited = new HashSet<>();
        for (ElementLike<?> element : menu.root().elementLikes()) {
            ModernElement<?, Canvas> found = control(element, identifier, visited);
            if (found != null) return found;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private @Nullable ModernElement<?, Canvas> control(@NotNull ElementLike<?> element, @NotNull Identifier identifier, @NotNull Set<Identifier> visited) {
        if (!visited.add(element.identifier())) return null;
        if (element instanceof ModernElement<?, ?> modernElement && modernElement.identifier().equals(identifier))
            return (ModernElement<?, Canvas>) modernElement;
        if (element instanceof EditorWidget<?> widget) {
            ModernElement<?, Canvas> nested = widget.nested(identifier);
            if (nested != null) return nested;
        }
        if (!(element instanceof Layout<?> layout)) return null;
        for (ModernElement<?, Canvas> child : layout.children()) {
            ModernElement<?, Canvas> found = control(child, identifier, visited);
            if (found != null) return found;
        }
        return null;
    }

    private int pageViewMode(@NotNull EditorProject.PageViewData view) {
        if (view.mode().equals("fixed")) return 0;
        if (view.mode().equals("center")) return 1;
        return 2;
    }

    private void pageViewMode(@NotNull UserClickElementEvent<?> event) {
        ActiveMenu menu = active;
        if (menu == null || selectedPage == null || event.user() != menu.user()) return;
        if (!(event.element() instanceof SelectorElement element)) return;
        if (!element.identifier().key().string().equals("low_right/page_view/mode")) return;
        EditorProject.PageViewData view = pageView(selectedPage);
        recordHistory();
        if (element.selected() == 0)
            pageView(selectedPage, EditorProject.PageViewData.fixed(Vec2i.of(view.x(), view.y())));
        if (element.selected() == 1)
            pageView(selectedPage, new EditorProject.PageViewData("center", view.x(), view.y(), 50, 50));
        if (element.selected() == 2)
            pageView(selectedPage, EditorProject.PageViewData.percent(view.x(), view.y(), view.anchorX(), view.anchorY()));
        hydratePageViewControls();
    }

    private void pageViewSelector(@NotNull UserUpdateSelectorInputEvent event) {
        ActiveMenu menu = active;
        if (menu == null || selectedPage == null || event.user() != menu.user()) return;
        SelectorInputContext context = event.context();
        EditorProject.PageViewData view = pageView(selectedPage);
        int x = view.x();
        int y = view.y();
        int anchorX = view.anchorX();
        int anchorY = view.anchorY();
        if (context.innerContexts().get(Identifier.of("low_right/page_view/position")) instanceof SelectorInputContext.ValueInnerContext position) {
            x = pageViewX(position, x);
            y = pageViewY(position, y);
        }
        if (context.innerContexts().get(Identifier.of("low_right/page_view/anchor")) instanceof SelectorInputContext.ValueInnerContext anchor) {
            anchorX = Math.clamp(pageViewX(anchor, anchorX), 0, 100);
            anchorY = Math.clamp(pageViewY(anchor, anchorY), 0, 100);
        }
        if (x == view.x() && y == view.y() && anchorX == view.anchorX() && anchorY == view.anchorY()) return;
        recordHistory();
        pageView(selectedPage, new EditorProject.PageViewData(view.mode(), x, y, anchorX, anchorY));
    }

    private void selector(@NotNull UserUpdateSelectorInputEvent event) {
        if (publishingSelectionControls) return;
        ActiveMenu menu = active;
        if (menu == null || event.user() != menu.user()) return;
        SelectorInputContext context = event.context();
        if (!(context.innerContexts().get(Identifier.of("top_right/position/value")) instanceof SelectorInputContext.ValueInnerContext posContext))
            return;
        if (!(context.innerContexts().get(Identifier.of("top_right/size/value")) instanceof SelectorInputContext.ValueInnerContext sizeContext))
            return;
        Integer[] posValues = posContext.values();
        Integer[] sizeValues = sizeContext.values();
        if (posValues.length < 3 || sizeValues.length < 3) return;
        if (posValues[0] == null || posValues[2] == null || sizeValues[0] == null || sizeValues[2] == null) return;
        VirtualPage page = selected();
        VirtualElement<?> element = selectedElement();
        if (page != null && element == null) {
            element = page.selected();
            selectedElement = element == null ? null : element.identifier();
        }
        if (page == null || element == null) return;
        Vec2i size = element.size();
        Vec2i position = element.position();
        int width = horizontal(sizeContext, size.x());
        int height = vertical(sizeContext, size.y());
        int x = horizontal(posContext, position.x());
        int y = vertical(posContext, position.y());
        Vec2i newSize = Vec2i.of(
                Math.clamp(width, 1, page.size().x()),
                Math.clamp(height, 1, page.size().y())
        );
        Vec2i newPosition = Vec2i.of(
                Math.clamp(x, 0, Math.max(0, page.size().x() - newSize.x())),
                Math.clamp(y, 0, Math.max(0, page.size().y() - newSize.y()))
        );
        if (element.size().equals(newSize) && element.position().equals(newPosition)) return;
        recordHistory();
        if (!element.size().equals(newSize)) element.size(newSize);
        if (!element.position().equals(newPosition)) element.position(newPosition);
        page.markDirty();
        if (newSize.x() != width || newSize.y() != height || newPosition.x() != x || newPosition.y() != y) {
            publishSelectionControls(newSize, newPosition);
        }
    }

    private int posValues(@NotNull Integer[] values, int index) {
        return values[index];
    }

    private int pageViewX(@NotNull SelectorInputContext.ValueInnerContext context, int current) {
        if (context.active() == 2 || context.active() == 3) return posValues(context.values(), context.active());
        return current;
    }

    private int pageViewY(@NotNull SelectorInputContext.ValueInnerContext context, int current) {
        if (context.active() == 0 || context.active() == 1) return posValues(context.values(), context.active());
        return current;
    }

    private int horizontal(@NotNull SelectorInputContext.ValueInnerContext context, int current) {
        if (context.active() == 0 || context.active() == 3) return posValues(context.values(), context.active());
        return current;
    }

    private int vertical(@NotNull SelectorInputContext.ValueInnerContext context, int current) {
        if (context.active() == 1 || context.active() == 2) return posValues(context.values(), context.active());
        return current;
    }

    private boolean elementExists(@NotNull Identifier identifier) {
        for (VirtualPage page : pages.values()) {
            if (page.element(identifier) != null) return true;
        }
        return false;
    }
}
