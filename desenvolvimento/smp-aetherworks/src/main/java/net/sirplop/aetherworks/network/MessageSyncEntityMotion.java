package net.sirplop.aetherworks.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sirplop.aetherworks.network.NetworkEvent;
import net.sirplop.aetherworks.client.AetherShieldReflectHandler;
import org.joml.Vector3f;

import java.util.function.Supplier;

public class MessageSyncEntityMotion {
    private final int syncTarget;
    private final Vector3f deltaMotion;
    private final float yRot;
    private final float yRotO;

    public MessageSyncEntityMotion(Entity ent, Vector3f deltaMotion, float yRot, float yRotO) {
        this.syncTarget = ent.getId();
        this.deltaMotion = deltaMotion;
        this.yRot = yRot;
        this.yRotO = yRotO;
    }
    public MessageSyncEntityMotion(int syncTarget, Vector3f deltaMotion, float yRot, float yRotO) {
        this.syncTarget = syncTarget;
        this.deltaMotion = deltaMotion;
        this.yRot = yRot;
        this.yRotO = yRotO;
    }

    public static void encode(MessageSyncEntityMotion msg, FriendlyByteBuf buf)
    {
        buf.writeInt(msg.syncTarget);
        buf.writeVector3f(msg.deltaMotion);
        buf.writeFloat(msg.yRot);
        buf.writeFloat(msg.yRotO);
    }

    public static MessageSyncEntityMotion decode(FriendlyByteBuf buf) {

        return new MessageSyncEntityMotion(buf.readInt(), buf.readVector3f(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(MessageSyncEntityMotion msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> handleClient(msg));
        }
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(MessageSyncEntityMotion msg) {
        Entity ent = Minecraft.getInstance().level.getEntity(msg.syncTarget);
        ent.setDeltaMovement(new Vec3(msg.deltaMotion.x, msg.deltaMotion.y, msg.deltaMotion.z));
        ent.setYRot(msg.yRot);
        ent.yRotO = msg.yRotO;
        AetherShieldReflectHandler.setReflected((Projectile)ent);
    }
}
