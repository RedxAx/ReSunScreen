package me.combimagnetron.sunscreen.neo.editor.project;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.passport.util.math.Vec2i;
import org.jetbrains.annotations.NotNull;
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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ProjectFileHandler {
    public static final String PROJECT_FILE = ".project.xml";
    private static final int VERSION = 3;

    private ProjectFileHandler() {
    }

    public static void write(@NotNull EditorProject project, @NotNull Path projectFolder) throws IOException {
        Files.createDirectories(projectFolder.resolve("pages"));
        Files.createDirectories(projectFolder.resolve("assets"));

        Document document = document();
        Element root = document.createElement("project");
        root.setAttribute("version", String.valueOf(VERSION));
        document.appendChild(root);

        Element metadata = child(document, root, "metadata");
        text(document, metadata, "identifier", project.identifier().string());
        text(document, metadata, "displayName", project.displayName());
        Identifier selectedPage = project.selectedPage();
        text(document, metadata, "selectedPage", selectedPage == null ? "" : selectedPage.string());

        Element theme = child(document, root, "theme");
        theme.setAttribute("mode", "builtin");
        text(document, theme, "id", project.themeId().string());

        Element pages = child(document, root, "pages");
        for (EditorProject.PageData page : project.pages()) {
            String file = "pages/" + fileName(page.identifier()) + ".xml";
            Element pageReference = child(document, pages, "page");
            pageReference.setAttribute("root", Boolean.toString(page.identifier().equals(project.selectedPage())));
            text(document, pageReference, "id", page.identifier().string());
            text(document, pageReference, "file", file);
            writePage(page, projectFolder.resolve(file));
        }

        writeDocument(document, projectFolder.resolve(PROJECT_FILE));
    }

    public static @NotNull EditorProject read(@NotNull Path projectFolder) throws IOException {
        Document document = readDocument(projectFolder.resolve(PROJECT_FILE));
        Element root = document.getDocumentElement();
        Element metadata = requiredChild(root, "metadata");
        EditorProject project = new EditorProject(text(metadata, "displayName"), identifier(text(metadata, "identifier")));
        String selectedPage = text(metadata, "selectedPage");
        if (!selectedPage.isBlank()) project.selectedPage(identifier(selectedPage));

        Element theme = child(root, "theme");
        if (theme != null) {
            String themeId = text(theme, "id");
            if (!themeId.isBlank()) project.themeId(identifier(themeId));
        }

        Element pages = requiredChild(root, "pages");
        for (Element pageReference : children(pages, "page")) {
            String file = text(pageReference, "file");
            if (file.isBlank()) continue;
            project.page(readPage(projectFolder.resolve(file)));
        }
        return project;
    }

    private static void writePage(@NotNull EditorProject.PageData page, @NotNull Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Document document = document();
        Element root = document.createElement("page");
        document.appendChild(root);
        text(document, root, "id", page.identifier().string());
        text(document, root, "displayName", page.displayName());
        vec(document, root, "position", page.position());
        vec(document, root, "size", page.size());
        writeView(document, root, page.view() == null ? EditorProject.PageViewData.center() : page.view());

        Element elements = child(document, root, "elements");
        for (EditorProject.ElementData element : page.elements()) {
            writeElement(document, elements, element);
        }
        writeDocument(document, path);
    }

    private static @NotNull EditorProject.PageData readPage(@NotNull Path path) throws IOException {
        Element root = readDocument(path).getDocumentElement();
        Identifier identifier = identifier(text(root, "id"));
        String displayName = text(root, "displayName");
        Vec2i position = vec(requiredChild(root, "position"));
        Vec2i size = vec(requiredChild(root, "size"));
        EditorProject.PageViewData view = readView(child(root, "view"));
        List<EditorProject.ElementData> elements = new ArrayList<>();
        Element elementsRoot = child(root, "elements");
        if (elementsRoot != null) {
            for (Element element : children(elementsRoot, "element")) {
                elements.add(readElement(element));
            }
        }
        return new EditorProject.PageData(identifier, displayName, position, size, elements, view);
    }

    private static void writeElement(@NotNull Document document, @NotNull Element parent, @NotNull EditorProject.ElementData element) {
        Element root = child(document, parent, "element");
        root.setAttribute("type", element.type());
        text(document, root, "id", element.identifier().string());
        vec(document, root, "position", element.position());
        vec(document, root, "size", element.size());
        if (element.text() != null) text(document, root, "text", element.text());
        vec(document, root, "textPosition", element.textPosition());
        text(document, root, "textColor", Integer.toString(element.textColor()));
        text(document, root, "selected", Integer.toString(element.selected()));
        text(document, root, "height", Integer.toString(element.height()));
        text(document, root, "pixelScale", Integer.toString(element.pixelScale()));
        vec(document, root, "canvasSize", element.canvasSize());
        text(document, root, "pixels", pixels(element.pixels()));

        Element entries = child(document, root, "entries");
        for (EditorProject.EntryData entry : element.entries()) {
            Element entryElement = child(document, entries, "entry");
            text(document, entryElement, "text", entry.text());
            text(document, entryElement, "color", Integer.toString(entry.color()));
        }
    }

    private static @NotNull EditorProject.ElementData readElement(@NotNull Element root) {
        List<EditorProject.EntryData> entries = new ArrayList<>();
        Element entriesRoot = child(root, "entries");
        if (entriesRoot != null) {
            for (Element entry : children(entriesRoot, "entry")) {
                entries.add(new EditorProject.EntryData(text(entry, "text"), integer(text(entry, "color"), 0)));
            }
        }
        return new EditorProject.ElementData(
                identifier(text(root, "id")),
                root.getAttribute("type"),
                vec(requiredChild(root, "position")),
                vec(requiredChild(root, "size")),
                nullableText(root, "text"),
                vec(requiredChild(root, "textPosition")),
                integer(text(root, "textColor"), 0),
                integer(text(root, "selected"), 0),
                integer(text(root, "height"), 0),
                entries,
                integer(text(root, "pixelScale"), 1),
                vec(requiredChild(root, "canvasSize")),
                pixels(text(root, "pixels"))
        );
    }

    private static void writeView(@NotNull Document document, @NotNull Element parent, @NotNull EditorProject.PageViewData view) {
        Element root = child(document, parent, "view");
        root.setAttribute("mode", view.mode());
        text(document, root, "x", Integer.toString(view.x()));
        text(document, root, "y", Integer.toString(view.y()));
        text(document, root, "anchorX", Integer.toString(view.anchorX()));
        text(document, root, "anchorY", Integer.toString(view.anchorY()));
    }

    private static @NotNull EditorProject.PageViewData readView(Element root) {
        if (root == null) return EditorProject.PageViewData.center();
        return new EditorProject.PageViewData(
                root.getAttribute("mode"),
                integer(text(root, "x"), 0),
                integer(text(root, "y"), 0),
                integer(text(root, "anchorX"), 50),
                integer(text(root, "anchorY"), 50)
        );
    }

    private static @NotNull Document document() throws IOException {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (Exception exception) {
            throw new IOException("Failed to create XML document", exception);
        }
    }

    private static @NotNull Document readDocument(@NotNull Path path) throws IOException {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path.toFile());
        } catch (Exception exception) {
            throw new IOException("Failed to read XML document " + path, exception);
        }
    }

    private static void writeDocument(@NotNull Document document, @NotNull Path path) throws IOException {
        try {
            Files.createDirectories(path.getParent());
            var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(document), new StreamResult(path.toFile()));
        } catch (Exception exception) {
            if (exception instanceof UncheckedIOException unchecked) throw unchecked.getCause();
            throw new IOException("Failed to write XML document " + path, exception);
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

    private static void vec(@NotNull Document document, @NotNull Element parent, @NotNull String tag, @NotNull Vec2i vec) {
        Element child = child(document, parent, tag);
        child.setAttribute("x", Integer.toString(vec.x()));
        child.setAttribute("y", Integer.toString(vec.y()));
    }

    private static @NotNull Vec2i vec(@NotNull Element element) {
        return Vec2i.of(integer(element.getAttribute("x"), 0), integer(element.getAttribute("y"), 0));
    }

    private static @NotNull String text(@NotNull Element parent, @NotNull String tag) {
        Element child = child(parent, tag);
        return child == null ? "" : child.getTextContent();
    }

    private static String nullableText(@NotNull Element parent, @NotNull String tag) {
        Element child = child(parent, tag);
        return child == null ? null : child.getTextContent();
    }

    private static @NotNull Element requiredChild(@NotNull Element parent, @NotNull String tag) {
        Element child = child(parent, tag);
        if (child == null) throw new IllegalArgumentException("Missing XML element " + tag);
        return child;
    }

    private static Element child(@NotNull Element parent, @NotNull String tag) {
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && element.getTagName().equals(tag)) return element;
        }
        return null;
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

    private static int integer(@NotNull String value, int fallback) {
        if (value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static @NotNull String pixels(int @NotNull [] pixels) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < pixels.length; i++) {
            if (i > 0) builder.append(',');
            builder.append(pixels[i]);
        }
        return builder.toString();
    }

    private static int @NotNull [] pixels(@NotNull String value) {
        if (value.isBlank()) return new int[0];
        String[] parts = value.split(",");
        int[] pixels = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            pixels[i] = integer(parts[i].trim(), 0);
        }
        return pixels;
    }

    public static @NotNull String fileName(@NotNull Identifier identifier) {
        return identifier.string().replace(':', '_').replace('/', '_');
    }

    public static @NotNull Identifier identifier(@NotNull String value) {
        return value.contains(":") ? Identifier.split(value) : Identifier.of(value);
    }
}
