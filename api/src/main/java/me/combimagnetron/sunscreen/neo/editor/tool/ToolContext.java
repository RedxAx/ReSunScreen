package me.combimagnetron.sunscreen.neo.editor.tool;

import me.combimagnetron.sunscreen.neo.editor.EditorController;
import me.combimagnetron.sunscreen.neo.editor.element.MenuPreviewElement;
import me.combimagnetron.sunscreen.neo.editor.virtual.VirtualElement;
import me.combimagnetron.sunscreen.neo.editor.virtual.VirtualPage;
import me.combimagnetron.sunscreen.neo.editor.virtual.argument.ElementConstructionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ToolContext(@NotNull EditorController controller,
                          @NotNull MenuPreviewElement preview,
                          @Nullable VirtualPage page,
                          @Nullable VirtualElement<?> element,
                          @Nullable ElementConstructionProvider<?> provider) {
}
