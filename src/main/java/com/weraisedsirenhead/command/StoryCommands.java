package com.weraisedsirenhead.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.story.PlayerStory;
import com.weraisedsirenhead.story.StoryManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * /sirenhead start   - empieza la historia: aparece el bebe en un bosque cercano.
 * /sirenhead status  - dia y misiones.
 * /sirenhead reset   - borra tu historia y elimina a la criatura (podes empezar de nuevo).
 * /sirenhead day N   - (operadores) salta al dia N, para pruebas.
 * /sirenhead locate  - (operadores) dice donde esta la criatura, para pruebas.
 */
public final class StoryCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sirenhead")
                .then(Commands.literal("start").executes(ctx -> start(ctx.getSource())))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("reset").executes(ctx -> reset(ctx.getSource())))
                .then(Commands.literal("day").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("day", IntegerArgumentType.integer(1, 5))
                                .executes(ctx -> setDay(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "day")))))
                .then(Commands.literal("locate").requires(source -> source.hasPermission(2))
                        .executes(ctx -> locate(ctx.getSource()))));
    }

    private static int start(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        switch (StoryManager.start(player)) {
            case STARTED:
                source.sendSuccess(() -> Component.translatable("command.weraisedsirenhead.started"), false);
                return Command.SINGLE_SUCCESS;
            case ALREADY_ACTIVE:
                source.sendFailure(Component.translatable("command.weraisedsirenhead.already"));
                return 0;
            case WRONG_DIMENSION:
                source.sendFailure(Component.translatable("command.weraisedsirenhead.wrong_dimension"));
                return 0;
            default:
                source.sendFailure(Component.translatable("command.weraisedsirenhead.no_space"));
                return 0;
        }
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerStory story = StoryManager.storyOf(player);
        if (story == null || !story.active) {
            source.sendFailure(Component.translatable("command.weraisedsirenhead.not_started"));
            return 0;
        }
        int done = 0;
        for (int i = 0; i < 3; i++) {
            if (story.isDone(i)) {
                done++;
            }
        }
        final int completedCount = done;
        source.sendSuccess(() -> Component.translatable("command.weraisedsirenhead.status", story.day, completedCount), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int reset(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        StoryManager.reset(player);
        source.sendSuccess(() -> Component.translatable("command.weraisedsirenhead.reset"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setDay(CommandSourceStack source, int day) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerStory story = StoryManager.storyOf(player);
        if (story == null || !story.active) {
            source.sendFailure(Component.translatable("command.weraisedsirenhead.not_started"));
            return 0;
        }
        StoryManager.setDay(player, story, day);
        source.sendSuccess(() -> Component.translatable("command.weraisedsirenhead.day_set", day), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int locate(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerStory story = StoryManager.storyOf(player);
        Entity entity = story == null || story.creature == null ? null : player.server.overworld().getEntity(story.creature);
        if (!(entity instanceof SirenHeadEntity siren)) {
            source.sendFailure(Component.translatable("command.weraisedsirenhead.no_creature"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.weraisedsirenhead.locate",
                (int) siren.getX(), (int) siren.getY(), (int) siren.getZ()), false);
        return Command.SINGLE_SUCCESS;
    }

    private StoryCommands() {
    }
}
