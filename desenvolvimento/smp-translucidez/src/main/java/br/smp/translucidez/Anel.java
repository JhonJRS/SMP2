package br.smp.translucidez;

import java.lang.reflect.Method;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * O "Ring of Invisibility" do Iron's Spells, apesar do nome, revela criaturas invisíveis a quem o usa. Sem dependência
 * de compilação: reflexão em ItemRegistry.INVISIBILITY_RING e CurioBaseItem.isEquippedBy, resolvida uma vez.
 */
public final class Anel {
    private Anel() {}

    private static boolean resolvido = false;
    private static Supplier<?> anel;
    private static Method equipado;
    private static int cacheTick = -1;
    private static boolean cacheValor = false;

    private static void resolver() {
        resolvido = true;
        try {
            Class<?> reg = Class.forName("io.redspace.ironsspellbooks.registries.ItemRegistry");
            anel = (Supplier<?>) reg.getField("INVISIBILITY_RING").get(null);
            equipado = Class.forName("io.redspace.ironsspellbooks.item.curios.CurioBaseItem").getMethod("isEquippedBy", LivingEntity.class);
        } catch (Throwable t) {
            anel = null;
            SmpTranslucidez.LOG.info("Iron's Spells sem Ring of Invisibility acessível ({}); só a opacidade padrão", t.toString());
        }
    }

    /** O jogador local usa o anel? (consultado no máximo uma vez por tick) */
    public static boolean jogadorLocalUsa() {
        if (!resolvido) resolver();
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (anel == null || p == null) return false;
        if (p.tickCount == cacheTick) return cacheValor;
        cacheTick = p.tickCount;
        try {
            cacheValor = (Boolean) equipado.invoke(anel.get(), p);
        } catch (Throwable t) {
            cacheValor = false;
        }
        return cacheValor;
    }
}
