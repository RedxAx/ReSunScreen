package me.combimagnetron.sunscreen.neo.editor.project;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.sunscreen.neo.TestMenuTemplate;
import me.combimagnetron.sunscreen.neo.editor.template.EditorMenuTemplate;
import me.combimagnetron.sunscreen.neo.theme.ModernTheme;
import me.combimagnetron.sunscreen.neo.theme.decorator.ThemeDecorator;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class EditorThemes {
    public static final Identifier MODERN = Identifier.of("sunscreen", "theme/modern");
    public static final Identifier TROPICAL = Identifier.of("sunscreen", "theme/tropical");

    private EditorThemes() {
    }

    public static @NotNull Identifier fromLabel(@NotNull String label) {
        return switch (label.toLowerCase(Locale.ROOT)) {
            case "tropical" -> TROPICAL;
            default -> MODERN;
        };
    }

    public static @NotNull ModernTheme theme(@NotNull Identifier identifier) {
        if (identifier.equals(TROPICAL)) return copy(TestMenuTemplate.THEME, TROPICAL);
        try {
            PluginThemeDefinition pluginTheme = PluginThemeStore.find(identifier);
            if (pluginTheme != null) return PluginThemeStore.compile(pluginTheme);
        } catch (Exception ignored) {
        }
        return copy(EditorMenuTemplate.EDITOR_THEME, MODERN);
    }

    private static @NotNull ModernTheme copy(@NotNull ModernTheme source, @NotNull Identifier identifier) {
        ModernTheme target = ModernTheme.theme(identifier);
        if (source.colorScheme() != null) target.colorScheme(source.colorScheme());
        for (ThemeDecorator decorator : source.decorators()) {
            target.decorator(decorator);
        }
        return target;
    }
}
