package net.sirplop.aetherworks.client;

import com.rekindled.embers.particle.GlowParticleOptions;
import com.rekindled.embers.util.Misc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.sirplop.aetherworks.item.AetherShield;
import net.sirplop.aetherworks.util.Utils;

import java.util.HashSet;

@OnlyIn(Dist.CLIENT)
public class AetherShieldReflectHandler {

    private final static HashSet<Projectile> affectedProjectiles = new HashSet<>();

    public static void setReflected(Projectile entity) {
        entity.addTag(AetherShield.SHIELD_TAG);
        affectedProjectiles.add(entity);
    }

    private static final GlowParticleOptions GLOW = new GlowParticleOptions(Misc.colorFromInt(Utils.AETHERIUM_PROJECTILE_COLOR.getRGB()), 4f, 5);
    @SubscribeEvent
    public static void onUpdateEvent(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        assert level != null;
        for (Projectile e : affectedProjectiles) {
            Vec3 dist = e.position().subtract(e.xOld, e.yOld, e.zOld);
            int partCount = (int)Math.ceil(dist.length() * 2);
            float lerp = 1f / (float)partCount;
            for (int i = 0; i < partCount; i++) {
                Vec3 pos = e.getPosition(lerp * i);
                level.addParticle(GLOW, pos.x, pos.y, pos.z, 0, -0.05, 0);
            }
        }
    }

    public static void onLevelUnload(LevelEvent.Unload event)
    {
        if ((event.getLevel() instanceof ServerLevel))
            return;
        affectedProjectiles.clear();
    }

    @SubscribeEvent
    public static void onEntityLeaveEvent(EntityLeaveLevelEvent event) {
        Entity ent = event.getEntity();
        if (ent instanceof Projectile p) {
            affectedProjectiles.remove(p);
        }
    }
}
