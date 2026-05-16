package me.combimagnetron.sunscreen.neo.editor.project;

import me.combimagnetron.passport.util.data.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record PluginThemeDefinition(
        @NotNull Identifier identifier,
        @NotNull String displayName,
        @NotNull List<ThemeComponentDefinition> components
) {
    public static @NotNull PluginThemeDefinition theme(@NotNull Identifier identifier, @NotNull String displayName) {
        return new PluginThemeDefinition(identifier, displayName, new ArrayList<>());
    }

    public @NotNull PluginThemeDefinition component(@NotNull ThemeComponentDefinition component) {
        components.add(component);
        return this;
    }
}
