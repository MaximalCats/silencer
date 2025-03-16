package dev.hex.silencer;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class Silencer extends JavaPlugin implements Listener {

    private LiteralCommandNode<CommandSourceStack> muteCommand() {
        return Commands.literal("mute")
                .then(Commands.argument("targets", ArgumentTypes.players())
                        .executes(ctx -> {
                            final PlayerSelectorArgumentResolver targetResolver = ctx.getArgument("targets",
                                    PlayerSelectorArgumentResolver.class);
                            final List<Player> targets = targetResolver.resolve(ctx.getSource());
                            final CommandSender commandExecutor = ctx.getSource().getSender();

                            for (final Player target : targets) {
                                NamespacedKey key = new NamespacedKey(this, "isMuted");
                                PersistentDataContainer pdc = target.getPersistentDataContainer();

                                boolean isMuted = false;

                                if (pdc.has(key))
                                    isMuted = pdc.get(key, PersistentDataType.BOOLEAN);

                                pdc.set(key, PersistentDataType.BOOLEAN, !isMuted);

                                final Component component = MiniMessage.miniMessage().deserialize(
                                        String.format(getConfig().getString(isMuted ? "messages.UNMUTE_CMD_RESPONSE"
                                                : "messages.MUTE_CMD_RESPONSE"), target.getName()));

                                commandExecutor.sendMessage(component);
                            }

                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    @Override
    public void onEnable() {
        saveResource("config.yml", false);
        saveDefaultConfig();

        Bukkit.getLogger().info("Seems like we are ready to go!");

        getServer().getPluginManager().registerEvents(this, this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(muteCommand());
        });
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent ev) {

        PersistentDataContainer pdc = ev.getPlayer().getPersistentDataContainer();
        NamespacedKey pKey = new NamespacedKey(this, "isMuted");

        if (pdc.has(pKey, PersistentDataType.BOOLEAN) && pdc.get(pKey, PersistentDataType.BOOLEAN)) {
            ev.getPlayer().sendMessage(
                    MiniMessage.miniMessage().deserialize(getConfig().getString("messages.MUTE_OFFENDER_RESPONSE")));
            ev.setCancelled(true);
        }
    }
}
