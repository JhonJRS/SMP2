package net.sirplop.aetherworks.recipe;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.sirplop.aetherworks.recipe.RecipeWrapper;

public class MetalFormerContext extends RecipeWrapper {

    public IFluidHandler fluids;
    public int temperature;

    public MetalFormerContext(IItemHandlerModifiable inv, IFluidHandler fluids, int temperature) {
        super(inv);
        this.fluids = fluids;
        this.temperature = temperature;
    }
}
