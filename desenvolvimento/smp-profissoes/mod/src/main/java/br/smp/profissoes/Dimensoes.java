package br.smp.profissoes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Portais / dimensões fechados até um ADM abrir (mod Access Denied): /smpprofissoes dimensao abrir|fechar &lt;dimensão&gt;.
 * Escreve config/access_denied/dimension/&lt;ns&gt;_&lt;dim&gt;.json com "disabled" e manda o Access Denied recarregar
 * (/sync-requirements).
 */
public final class Dimensoes {
    private Dimensoes() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static Path arquivo(ResourceLocation dim) {
        return FMLPaths.CONFIGDIR.get().resolve("access_denied").resolve("dimension").resolve(dim.getNamespace() + "_" + dim.getPath().replace('/', '_') + ".json");
    }

    static void comandos(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal(SmpProfissoes.MODID).requires(s -> s.hasPermission(2))
                .then(Commands.literal("dimensao")
                        .then(Commands.literal("abrir").then(Commands.argument("dimensao", DimensionArgument.dimension()).executes(c -> definir(c, false))))
                        .then(Commands.literal("fechar").then(Commands.argument("dimensao", DimensionArgument.dimension()).executes(c -> definir(c, true))))));
    }

    private static int definir(CommandContext<CommandSourceStack> c, boolean fechada) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ResourceLocation dim = DimensionArgument.getDimension(c, "dimensao").dimension().location();
        try {
            Path p = arquivo(dim);
            Files.createDirectories(p.getParent());
            JsonObject o = Files.exists(p) ? GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class) : new JsonObject();
            o.addProperty("dimension", dim.toString());
            o.addProperty("disabled", fechada);
            Files.writeString(p, GSON.toJson(o), StandardCharsets.UTF_8);
        } catch (Exception e) {
            c.getSource().sendFailure(Component.literal("Could not write the Access Denied file: " + e.getMessage()));
            return 0;
        }
        CommandSourceStack src = c.getSource().getServer().createCommandSourceStack().withSuppressedOutput();
        c.getSource().getServer().getCommands().performPrefixedCommand(src, "sync-requirements");
        c.getSource().sendSuccess(() -> Component.literal((fechada ? "Closed: " : "Opened: ") + dim), true);
        SmpProfissoes.LOG.info("Dimensão {} {}", dim, fechada ? "fechada" : "aberta");
        return 1;
    }
}
