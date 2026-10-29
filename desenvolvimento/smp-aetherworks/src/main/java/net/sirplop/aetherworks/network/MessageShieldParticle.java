package net.sirplop.aetherworks.network;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rekindled.embers.particle.GlowParticleOptions;
import com.rekindled.embers.util.Misc;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sirplop.aetherworks.network.NetworkEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.function.Supplier;

public class MessageShieldParticle {
    private final Vector3f point;
    private final float rotX;
    private final float rotY;
    private final byte numberOfParticles;
    private final int packedColor;

    public MessageShieldParticle(Vector3f point, float rotX, float rotY, byte numberOfParticles, int packedColor) {
        this.numberOfParticles = numberOfParticles;
        this.packedColor = packedColor;
        this.point = point;
        this.rotX = rotX;
        this.rotY = rotY;
    }

    public static void encode(MessageShieldParticle msg, FriendlyByteBuf buf) {
        buf.writeVector3f(msg.point);
        buf.writeFloat(msg.rotX);
        buf.writeFloat(msg.rotY);
        buf.writeByte(msg.numberOfParticles);
        buf.writeInt(msg.packedColor);
    }

    public static MessageShieldParticle decode(FriendlyByteBuf buf) {
        return new MessageShieldParticle(buf.readVector3f(), buf.readFloat(), buf.readFloat(), buf.readByte(), buf.readInt());
    }
    public static void handle(MessageShieldParticle msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> spawnParticles(msg));
        }
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static void spawnParticles(MessageShieldParticle msg) {
        Level level = Minecraft.getInstance().level;
        assert level != null;


        PoseStack poseStack = new PoseStack();
        poseStack.translate(msg.point.x, msg.point.y, msg.point.z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(0));
        poseStack.mulPose(Axis.YP.rotation((float)-Math.toRadians(msg.rotY)));
        poseStack.mulPose(Axis.XP.rotation((float)Math.toRadians(msg.rotX)));
        Matrix4f matrix4f = poseStack.last().pose();

        Vector3f color = Misc.colorFromInt(msg.packedColor);
        GlowParticleOptions glow = new GlowParticleOptions(color, 1.25f, 10);
        double rotInc = (Math.PI * 2) / (double)msg.numberOfParticles;
        double randStart = level.random.nextFloat() * (0.087);

        for (int i = 0; i < msg.numberOfParticles; i++) {
            float x = (float)Math.cos((rotInc * i) + randStart);
            float y = (float)Math.sin((rotInc * i) + randStart);

            Vector3f point = matrix4f.transformPosition(new Vector3f(x, y,  0));
            Vector3f speed = matrix4f.transformDirection(new Vector3f(x * 0.25f, y, -0.5f));
            level.addParticle(glow, false, point.x, point.y, point.z,
                    speed.x, speed.y, speed.z);
        }
    }
}
