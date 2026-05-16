package me.combimagnetron.sunscreen.neo.editor.project;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.passport.util.math.Vec2i;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public record ThemeComponentDefinition(
        @NotNull Identifier identifier,
        @NotNull String displayName,
        @NotNull String type,
        @NotNull Map<String, StateDefinition> states
) {
    public static @NotNull ThemeComponentDefinition component(@NotNull Identifier identifier, @NotNull String displayName, @NotNull String type) {
        return new ThemeComponentDefinition(identifier, displayName, type, new LinkedHashMap<>());
    }

    public @NotNull ThemeComponentDefinition state(@NotNull String state, @NotNull StateDefinition definition) {
        states.put(state, definition);
        return this;
    }

    public record StateDefinition(
            @NotNull String state,
            @NotNull Identifier asset,
            @NotNull Vec2i position,
            @NotNull Vec2i size,
            int top,
            int bottom,
            int left,
            int right
    ) {
    }
}
