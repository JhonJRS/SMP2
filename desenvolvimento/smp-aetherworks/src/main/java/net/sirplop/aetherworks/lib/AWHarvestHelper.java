package net.sirplop.aetherworks.lib;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.sirplop.aetherworks.Aetherworks;

import java.util.*;

public class AWHarvestHelper {

    private static final Map<UUID, AWHarvestNode> nodes = new HashMap<UUID, AWHarvestNode>();

    public static void onServerTick(LevelTickEvent.Post event)
    {
        if (event.getLevel().isClientSide())
            return;
        Stack<UUID> toRemove = new Stack<>();
        for (Map.Entry<UUID, AWHarvestNode> uuidawHarvestNodeEntry : nodes.entrySet())
        {
            uuidawHarvestNodeEntry.getValue().tick();
            if (uuidawHarvestNodeEntry.getValue().isInvalid())
            {
                toRemove.add(uuidawHarvestNodeEntry.getKey());
            }
        }

        while (!toRemove.isEmpty())
        {
            nodes.remove(toRemove.pop());
        }
    }

    public static void onLevelUnload(LevelEvent.Unload event)
    {
        if (!(event.getLevel() instanceof ServerLevel))
            return;
        Stack<UUID> toRemove = new Stack<>();
        for (Map.Entry<UUID, AWHarvestNode> uuidawHarvestNodeEntry : nodes.entrySet())
        {
            if (uuidawHarvestNodeEntry.getValue().level == event.getLevel())
            {
                toRemove.add(uuidawHarvestNodeEntry.getKey());
            }
        }

        while (!toRemove.isEmpty())
        {
            nodes.remove(toRemove.pop());
        }
    }

    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        Stack<UUID> toRemove = new Stack<>();
        for (Map.Entry<UUID, AWHarvestNode> uuidawHarvestNodeEntry : nodes.entrySet())
        {
            if (uuidawHarvestNodeEntry.getValue().harvester == player)
            {
                toRemove.add(uuidawHarvestNodeEntry.getKey());
            }
        }

        while (!toRemove.isEmpty())
        {
            nodes.remove(toRemove.pop());
        }
    }

    public static boolean alreadyHasPlayer(Player player) {
        return nodes.containsKey(player.getUUID());
    }


    public static boolean addNode(Player invoker, AWHarvestNode node)
    {
        UUID playerID = invoker.getUUID();
        if (nodes.containsKey(playerID))
        {
            return false;
        }

        node.initNode();
        if (node.isInvalid())
        {
            return false;
        }

        nodes.put(playerID, node);
        return true;
    }
}
