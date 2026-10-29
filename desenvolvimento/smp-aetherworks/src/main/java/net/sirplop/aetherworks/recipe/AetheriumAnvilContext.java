package net.sirplop.aetherworks.recipe;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.sirplop.aetherworks.recipe.RecipeWrapper;

public class AetheriumAnvilContext extends RecipeWrapper {

    public int temperature;

    public AetheriumAnvilContext(IItemHandlerModifiable inv, int temperature) {
        super(inv);
        this.temperature = temperature;
    }
}
