package me.combimagnetron.sunscreen.neo.editor.tool;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.passport.util.math.Vec2i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ToolPointer(@NotNull Vec2i raw,
                          @NotNull Vec2i local,
                          @NotNull Vec2i world,
                          @Nullable Identifier page,
                          @Nullable Vec2i pageLocal) {
}
