package me.combimagnetron.sunscreen.neo.editor.tool;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.sunscreen.neo.cursor.CursorStyle;
import me.combimagnetron.sunscreen.neo.graphic.Canvas;
import me.combimagnetron.sunscreen.neo.graphic.text.Text;
import me.combimagnetron.sunscreen.neo.render.engine.context.RenderContext;
import org.jetbrains.annotations.NotNull;

public interface Tool {

    @NotNull Identifier identifier();

    @NotNull Text displayName();

    default @NotNull CursorStyle cursor(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
        return CursorStyle.pointer();
    }

    default void press(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
    }

    default void drag(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
    }

    default void release(@NotNull ToolContext context, @NotNull ToolPointer pointer) {
    }

    default void cancel(@NotNull ToolContext context) {
    }

    default @NotNull Canvas overlay(@NotNull ToolContext context, @NotNull RenderContext renderContext) {
        return Canvas.empty(context.preview().viewSize());
    }

}
