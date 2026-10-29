import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;
import java.io.FileReader;
import java.io.File;
import java.util.List;

public class TesteEmotes {
    public static void main(String[] a) throws Exception {
        int ok = 0, falha = 0;
        File[] fs = new File(a[0]).listFiles((d, n) -> n.endsWith(".json"));
        java.util.Arrays.sort(fs);
        for (File f : fs) {
            try (FileReader r = new FileReader(f)) {
                List<KeyframeAnimation> l = AnimationSerializing.deserializeAnimation(r);
                KeyframeAnimation k = l.get(0);
                if (k.endTick <= 0 || !k.isPlayingAt(1)) throw new IllegalStateException("animação vazia");
                ok++;
                System.out.println("OK  " + f.getName() + "  end=" + k.endTick + " loop=" + k.isInfinite + " name=" + k.extraData.get("name"));
            } catch (Exception e) {
                falha++;
                System.out.println("FALHA " + f.getName() + ": " + e);
            }
        }
        System.out.println(ok + " ok, " + falha + " falhas");
        System.exit(falha == 0 ? 0 : 1);
    }
}
