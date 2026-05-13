package me.combimagnetron.sunscreen.command;

import me.combimagnetron.sunscreen.SunscreenLibrary;
import me.combimagnetron.sunscreen.neo.editor.EditorController;
import me.combimagnetron.sunscreen.neo.editor.project.EditorProject;
import me.combimagnetron.sunscreen.neo.editor.project.EditorProjectStore;
import me.combimagnetron.sunscreen.neo.editor.template.EditorProjectDisplayMenuTemplate;
import me.combimagnetron.sunscreen.user.SunscreenUser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Named;
import revxrsal.commands.annotation.SuggestWith;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.CommandPlaceholder;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.command.CommandActor;
import revxrsal.commands.node.ExecutionContext;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

@Command("sunscreen")
public class SunscreenCommand {

    @CommandPlaceholder
    public void sunscreen(@NotNull CommandActor actor) {
        SunscreenUser<?> user = SunscreenLibrary.library().users().user(actor.uniqueId()).orElseThrow();
        TextComponent.Builder builder = Component.text();
        builder.append(Component.text("Sunscreen Commands").style(Style.style(TextColor.fromHexString("#4D9BE6"), TextDecoration.BOLD))).append(Component.newline());
        builder.append(Component.text(" ● /sunscreen ").style(Style.style(TextColor.fromHexString("#F9C22B"))).append(Component.text("editor").style(Style.style(TextColor.fromHexString("#F79617"), TextDecoration.UNDERLINED))));
        builder.append(Component.newline());
        builder.append(Component.text(" ● /sunscreen ").style(Style.style(TextColor.fromHexString("#F9C22B"))).append(Component.text("editor save").style(Style.style(TextColor.fromHexString("#F79617"), TextDecoration.UNDERLINED))));
        builder.append(Component.newline());
        builder.append(Component.text(" ● /sunscreen ").style(Style.style(TextColor.fromHexString("#F9C22B"))).append(Component.text("display").style(Style.style(TextColor.fromHexString("#F79617"), TextDecoration.UNDERLINED))));
        user.message(builder.asComponent());
    }

    @Subcommand("editor")
    public void editor(@NotNull CommandActor actor) {
        SunscreenUser<?> user = SunscreenLibrary.library().users().user(actor.uniqueId()).orElseThrow();
        EditorController controller = new EditorController(user, actor.uniqueId());
        controller.start();
    }

    @Subcommand("editor save")
    public void save(@NotNull CommandActor actor) {
        SunscreenUser<?> user = SunscreenLibrary.library().users().user(actor.uniqueId()).orElseThrow();
        EditorController controller = EditorController.controller(actor.uniqueId());
        if (controller == null) {
            user.message(Component.text("No editor project is open."));
            return;
        }
        try {
            Path path = EditorProjectStore.save(controller.project());
            user.message(Component.text("Saved project to " + path.getFileName()));
        } catch (IOException exception) {
            SunscreenLibrary.library().logger().error("Failed to save editor project", exception);
            user.message(Component.text("Failed to save project."));
        }
    }

    @Subcommand("display")
    public void display(@NotNull CommandActor actor, @Named("projectName") @Default("") @SuggestWith(ProjectSuggestions.class) @NotNull String projectName, @Named("pageName") @Default("") @SuggestWith(PageSuggestions.class) @NotNull String pageName) {
        SunscreenUser<?> user = SunscreenLibrary.library().users().user(actor.uniqueId()).orElseThrow();
        try {
            EditorProject project = projectName.isBlank() ? EditorProjectStore.latest() : EditorProjectStore.load(projectName);
            if (project == null) {
                user.message(Component.text("No saved project found."));
                return;
            }
            if (!pageName.isBlank() && project.page(pageName) == null) {
                user.message(Component.text("Page not found."));
                return;
            }
            user.open(new EditorProjectDisplayMenuTemplate(project, pageName));
        } catch (IOException exception) {
            SunscreenLibrary.library().logger().error("Failed to open saved editor project", exception);
            user.message(Component.text("Failed to open saved project."));
        }
    }

    public static class ProjectSuggestions implements SuggestionProvider<CommandActor> {

        @Override
        public @NotNull Collection<String> getSuggestions(@NotNull ExecutionContext<CommandActor> context) {
            try {
                return EditorProjectStore.projects();
            } catch (IOException ignored) {
                return List.of();
            }
        }
    }

    public static class PageSuggestions implements SuggestionProvider<CommandActor> {

        @Override
        public @NotNull Collection<String> getSuggestions(@NotNull ExecutionContext<CommandActor> context) {
            String project = context.getResolvedArgumentOrNull("projectName");
            try {
                return EditorProjectStore.pages(project == null ? "" : project);
            } catch (IOException ignored) {
                return List.of();
            }
        }
    }

}
