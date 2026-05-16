package me.combimagnetron.sunscreen.neo.editor.project;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.passport.util.math.Vec2i;
import me.combimagnetron.sunscreen.SunscreenLibrary;
import me.combimagnetron.sunscreen.neo.element.GenericInteractableModernElement;
import me.combimagnetron.sunscreen.neo.element.impl.ButtonElement;
import me.combimagnetron.sunscreen.neo.element.impl.DropdownElement;
import me.combimagnetron.sunscreen.neo.element.impl.SelectorElement;
import me.combimagnetron.sunscreen.neo.element.impl.SliderElement;
import me.combimagnetron.sunscreen.neo.element.impl.text.TextFieldElement;
import me.combimagnetron.sunscreen.neo.graphic.Canvas;
import me.combimagnetron.sunscreen.neo.graphic.NineSlice;
import me.combimagnetron.sunscreen.neo.theme.ModernTheme;
import me.combimagnetron.sunscreen.neo.theme.decorator.Target;
import me.combimagnetron.sunscreen.neo.theme.decorator.ThemeDecorator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PluginThemeStore {
    private PluginThemeStore() {
    }

    public static @NotNull List<PluginThemeDefinition> themes() throws IOException {
        Path folder = folder();
        Files.createDirectories(folder);
        try (var paths = Files.list(folder)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".xml"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(PluginThemeStore::readUnchecked)
                    .toList();
        }
    }

    public static @NotNull PluginThemeDefinition createTheme() throws IOException {
        int index = themes().size() + 1;
        Identifier identifier = Identifier.of("sunscreen", "theme/custom_" + index);
        while (Files.exists(path(identifier))) {
            index++;
            identifier = Identifier.of("sunscreen", "theme/custom_" + index);
        }
        PluginThemeDefinition theme = PluginThemeDefinition.theme(identifier, "Custom Theme " + index);
        save(theme);
        return theme;
    }

    public static @Nullable PluginThemeDefinition find(@NotNull Identifier identifier) throws IOException {
        for (PluginThemeDefinition theme : themes()) {
            if (theme.identifier().equals(identifier)) return theme;
        }
        return null;
    }

    public static @NotNull ModernTheme compile(@NotNull PluginThemeDefinition definition) throws IOException {
        ModernTheme theme = ModernTheme.theme(definition.identifier());
        for (ThemeComponentDefinition component : definition.components()) {
            Target<?> target = target(component.type());
            if (target == null) continue;
            ThemeDecorator.StateNineSliceThemeDecorator decorator = ThemeDecorator.stated(target);
            state(component, "normal", GenericInteractableModernElement.ElementPhase.DEFAULT, decorator);
            state(component, "hover", GenericInteractableModernElement.ElementPhase.HOVER, decorator);
            state(component, "selected", GenericInteractableModernElement.ElementPhase.CLICK, decorator);
            state(component, "active", GenericInteractableModernElement.ElementPhase.CLICK, decorator);
            theme.decorator(decorator);
        }
        return theme;
    }

    public static @NotNull ThemeComponentDefinition createComponentFromSelection(@NotNull PluginThemeDefinition theme, @NotNull EditorAssetStore.AssetData asset, @NotNull EditorThemeBuilderState selection, @NotNull String type) throws IOException {
        int index = theme.components().size() + 1;
        Identifier identifier = Identifier.of("sunscreen", "theme/component/" + type + "_" + index);
        ThemeComponentDefinition component = ThemeComponentDefinition.component(identifier, display(type) + " " + index, type);
        ThemeComponentDefinition.StateDefinition normal = new ThemeComponentDefinition.StateDefinition(
                "normal",
                asset.identifier(),
                selection.regionPosition(),
                selection.regionSize(),
                selection.top(),
                selection.bottom(),
                selection.left(),
                selection.right()
        );
        component.state("normal", normal)
                .state("hover", normal)
                .state("selected", normal)
                .state("active", normal);
        theme.component(component);
        save(theme);
        return component;
    }

    public static void save(@NotNull PluginThemeDefinition theme) throws IOException {
        Files.createDirectories(folder());
        Document document = document();
        Element root = document.createElement("theme");
        root.setAttribute("version", "1");
        document.appendChild(root);
        text(document, root, "id", theme.identifier().string());
        text(document, root, "displayName", theme.displayName());
        Element components = child(document, root, "components");
        for (ThemeComponentDefinition component : theme.components()) {
            Element componentElement = child(document, components, "component");
            componentElement.setAttribute("type", component.type());
            text(document, componentElement, "id", component.identifier().string());
            text(document, componentElement, "displayName", component.displayName());
            for (ThemeComponentDefinition.StateDefinition state : component.states().values()) {
                Element stateElement = child(document, componentElement, "state");
                stateElement.setAttribute("name", state.state());
                text(document, stateElement, "asset", state.asset().string());
                vec(document, stateElement, "position", state.position());
                vec(document, stateElement, "size", state.size());
                Element slice = child(document, stateElement, "slice");
                slice.setAttribute("top", Integer.toString(state.top()));
                slice.setAttribute("bottom", Integer.toString(state.bottom()));
                slice.setAttribute("left", Integer.toString(state.left()));
                slice.setAttribute("right", Integer.toString(state.right()));
            }
        }
        write(document, path(theme.identifier()));
    }

    private static @NotNull PluginThemeDefinition readUnchecked(@NotNull Path path) {
        try {
            return read(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static @NotNull PluginThemeDefinition read(@NotNull Path path) throws IOException {
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path.toFile());
            Element root = document.getDocumentElement();
            PluginThemeDefinition theme = PluginThemeDefinition.theme(ProjectFileHandler.identifier(text(root, "id")), text(root, "displayName"));
            Element components = child(root, "components");
            if (components == null) return theme;
            for (Element componentElement : children(components, "component")) {
                ThemeComponentDefinition component = ThemeComponentDefinition.component(
                        ProjectFileHandler.identifier(text(componentElement, "id")),
                        text(componentElement, "displayName"),
                        componentElement.getAttribute("type")
                );
                for (Element stateElement : children(componentElement, "state")) {
                    Element slice = child(stateElement, "slice");
                    component.state(stateElement.getAttribute("name"), new ThemeComponentDefinition.StateDefinition(
                            stateElement.getAttribute("name"),
                            ProjectFileHandler.identifier(text(stateElement, "asset")),
                            vec(requiredChild(stateElement, "position")),
                            vec(requiredChild(stateElement, "size")),
                            slice == null ? 1 : integer(slice.getAttribute("top"), 1),
                            slice == null ? 1 : integer(slice.getAttribute("bottom"), 1),
                            slice == null ? 1 : integer(slice.getAttribute("left"), 1),
                            slice == null ? 1 : integer(slice.getAttribute("right"), 1)
                    ));
                }
                theme.component(component);
            }
            return theme;
        } catch (Exception exception) {
            throw new IOException("Failed to read plugin theme " + path, exception);
        }
    }

    private static void state(@NotNull ThemeComponentDefinition component, @NotNull String state, @NotNull GenericInteractableModernElement.ElementPhase phase, @NotNull ThemeDecorator.StateNineSliceThemeDecorator decorator) throws IOException {
        ThemeComponentDefinition.StateDefinition definition = component.states().get(state);
        if (definition == null) return;
        decorator.state(phase, nineSlice(definition));
    }

    private static @NotNull NineSlice nineSlice(@NotNull ThemeComponentDefinition.StateDefinition definition) throws IOException {
        EditorAssetStore.AssetData asset = asset(definition.asset());
        if (asset == null) throw new IOException("Missing theme asset " + definition.asset());
        Canvas source = Canvas.file(SunscreenLibrary.library().path().resolve("assets").resolve(asset.file()));
        Canvas region = source.sub(definition.position(), definition.size());
        Vec2i corner = Vec2i.of(Math.max(1, definition.left()), Math.max(1, definition.top()));
        Vec2i top = Vec2i.of(Math.max(1, definition.size().x() - definition.left() - definition.right()), Math.max(1, definition.top()));
        Vec2i left = Vec2i.of(Math.max(1, definition.left()), Math.max(1, definition.size().y() - definition.top() - definition.bottom()));
        return NineSlice.nineSlice(region, corner, top, left);
    }

    private static @Nullable EditorAssetStore.AssetData asset(@NotNull Identifier identifier) throws IOException {
        for (EditorAssetStore.AssetData asset : EditorAssetStore.assets()) {
            if (asset.identifier().equals(identifier)) return asset;
        }
        return null;
    }

    private static Target<?> target(@NotNull String type) {
        return switch (type) {
            case "button" -> Target.typed(ButtonElement.class);
            case "slider" -> Target.typed(SliderElement.class);
            case "selector" -> Target.typed(SelectorElement.class);
            case "dropdown" -> Target.typed(DropdownElement.class);
            case "textfield" -> Target.typed(TextFieldElement.class);
            default -> null;
        };
    }

    private static @NotNull String display(@NotNull String type) {
        return switch (type) {
            case "textfield" -> "Text Field";
            case "dropdown" -> "Dropdown";
            default -> Character.toUpperCase(type.charAt(0)) + type.substring(1);
        };
    }

    private static @NotNull Path folder() {
        return SunscreenLibrary.library().path().resolve("themes");
    }

    private static @NotNull Path path(@NotNull Identifier identifier) {
        return folder().resolve(ProjectFileHandler.fileName(identifier) + ".xml");
    }

    private static @NotNull Document document() throws IOException {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (Exception exception) {
            throw new IOException("Failed to create theme XML", exception);
        }
    }

    private static void write(@NotNull Document document, @NotNull Path path) throws IOException {
        try {
            var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(document), new StreamResult(path.toFile()));
        } catch (Exception exception) {
            throw new IOException("Failed to write theme XML " + path, exception);
        }
    }

    private static @NotNull Element child(@NotNull Document document, @NotNull Element parent, @NotNull String tag) {
        Element child = document.createElement(tag);
        parent.appendChild(child);
        return child;
    }

    private static void text(@NotNull Document document, @NotNull Element parent, @NotNull String tag, @NotNull String value) {
        Element child = child(document, parent, tag);
        child.setTextContent(value);
    }

    private static void vec(@NotNull Document document, @NotNull Element parent, @NotNull String tag, @NotNull Vec2i value) {
        Element child = child(document, parent, tag);
        child.setAttribute("x", Integer.toString(value.x()));
        child.setAttribute("y", Integer.toString(value.y()));
    }

    private static @NotNull Vec2i vec(@NotNull Element element) {
        return Vec2i.of(integer(element.getAttribute("x"), 0), integer(element.getAttribute("y"), 0));
    }

    private static Element child(@NotNull Element parent, @NotNull String tag) {
        for (Element child : children(parent, tag)) return child;
        return null;
    }

    private static @NotNull Element requiredChild(@NotNull Element parent, @NotNull String tag) {
        Element child = child(parent, tag);
        if (child == null) throw new IllegalArgumentException("Missing " + tag);
        return child;
    }

    private static @NotNull List<Element> children(@NotNull Element parent, @NotNull String tag) {
        List<Element> elements = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && element.getTagName().equals(tag)) elements.add(element);
        }
        return elements;
    }

    private static @NotNull String text(@NotNull Element parent, @NotNull String tag) {
        Element child = child(parent, tag);
        return child == null ? "" : child.getTextContent();
    }

    private static int integer(@NotNull String value, int fallback) {
        if (value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
