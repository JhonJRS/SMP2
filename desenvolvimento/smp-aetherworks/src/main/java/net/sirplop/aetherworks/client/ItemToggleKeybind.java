package net.sirplop.aetherworks.client;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.network.MessageToggleItem;

public class ItemToggleKeybind extends KeyMapping {

    protected boolean isPressed;

    public ItemToggleKeybind(String description, int keyCode, String category) {

        super(description, keyCode, category);
        setKeyConflictContext(KeyConflictContext.IN_GAME);
    }
    @Override
    public void setDown(boolean valueIn) {
        boolean prevPressed = isPressed;
        super.setDown(valueIn);
        isPressed = valueIn;

        if (isPressed && !prevPressed) {
            MessageToggleItem.sendToServer();
        }
    }
}
