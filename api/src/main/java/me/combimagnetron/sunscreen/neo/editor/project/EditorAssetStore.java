package me.combimagnetron.sunscreen.neo.editor.project;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.sunscreen.SunscreenLibrary;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EditorAssetStore {
    private static final String INDEX_FILE = "index.xml";

    private EditorAssetStore() {
    }

    public static @NotNull Path importFolder() {
        return SunscreenLibrary.library().path().resolve("Drop to Import");
    }

    public static @NotNull List<AssetData> assets() throws IOException {
        Path index = root().resolve(INDEX_FILE);
        if (!Files.exists(index)) return List.of();
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(index.toFile());
            Element root = document.getDocumentElement();
            List<AssetData> assets = new ArrayList<>();
            for (Element asset : children(root, "asset")) {
                assets.add(new AssetData(asset.getAttribute("type"), ProjectFileHandler.identifier(text(asset, "id")), text(asset, "file"), text(asset, "role")));
            }
            return assets;
        } catch (Exception exception) {
            throw new IOException("Failed to read asset index", exception);
        }
    }

    public static @NotNull AssetData importAsset(@NotNull Path source) throws IOException {
        Files.createDirectories(importFolder());
        String fileName = source.getFileName().toString();
        String type = type(fileName);
        Path targetFolder = switch (type) {
            case "image" -> root().resolve("images");
            case "sunscreen" -> root().resolve("sunscreen");
            default -> root().resolve("rejected");
        };
        Files.createDirectories(targetFolder);
        Path target = unique(targetFolder.resolve(sanitized(fileName)));
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        AssetData asset = new AssetData(type, assetIdentifier(target), root().relativize(target).toString().replace('\\', '/'), "unidentified");
        if (!type.equals("unsupported")) append(asset);
        return asset;
    }

    private static void append(@NotNull AssetData asset) throws IOException {
        List<AssetData> assets = new ArrayList<>(assets());
        assets.removeIf(existing -> existing.identifier().equals(asset.identifier()));
        assets.add(asset);
        write(assets);
    }

    private static void write(@NotNull List<AssetData> assets) throws IOException {
        try {
            Files.createDirectories(root());
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = document.createElement("assets");
            document.appendChild(root);
            for (AssetData asset : assets) {
                Element element = document.createElement("asset");
                element.setAttribute("type", asset.type());
                root.appendChild(element);
                text(document, element, "id", asset.identifier().string());
                text(document, element, "file", asset.file());
                text(document, element, "role", asset.role());
            }
            var transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(document), new StreamResult(root().resolve(INDEX_FILE).toFile()));
        } catch (Exception exception) {
            throw new IOException("Failed to write asset index", exception);
        }
    }

    private static @NotNull String type(@NotNull String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image";
        if (lower.endsWith(".sunscreen-asset.xml") || lower.endsWith(".sunscreen-theme.xml")) return "sunscreen";
        return "unsupported";
    }

    private static @NotNull Path unique(@NotNull Path path) {
        if (!Files.exists(path)) return path;
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String name = dot == -1 ? fileName : fileName.substring(0, dot);
        String extension = dot == -1 ? "" : fileName.substring(dot);
        int index = 2;
        Path candidate;
        do {
            candidate = path.getParent().resolve(name + "_" + index + extension);
            index++;
        } while (Files.exists(candidate));
        return candidate;
    }

    private static @NotNull String sanitized(@NotNull String fileName) {
        return fileName.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static @NotNull Identifier assetIdentifier(@NotNull Path path) {
        String relative = root().relativize(path).toString().replace('\\', '/');
        int dot = relative.lastIndexOf('.');
        if (dot != -1) relative = relative.substring(0, dot);
        return Identifier.of("sunscreen", "asset/" + relative.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/_-]", "_"));
    }

    private static @NotNull Path root() {
        return SunscreenLibrary.library().path().resolve("assets");
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
        for (Element child : children(parent, tag)) return child.getTextContent();
        return "";
    }

    private static void text(@NotNull Document document, @NotNull Element parent, @NotNull String tag, @NotNull String value) {
        Element child = document.createElement(tag);
        child.setTextContent(value);
        parent.appendChild(child);
    }

    public record AssetData(@NotNull String type, @NotNull Identifier identifier, @NotNull String file, @NotNull String role) {
    }
}
