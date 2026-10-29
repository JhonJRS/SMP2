package net.sirplop.aetherworks.research;

import com.rekindled.embers.gui.GuiCodex;
import com.rekindled.embers.research.ResearchBase;
import com.rekindled.embers.util.Vec2i;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedList;

public class ResearchShowImage extends ResearchBase {
    LinkedList<DisplayImage> displayImages = new LinkedList<DisplayImage>();

    public ResearchShowImage(String location, ItemStack icon, double x, double y) {
        super(location, icon, x, y);
    }

    public ResearchShowImage(String location, ItemStack icon, Vec2i pos) {
        this(location, icon, pos.x, pos.y);
    }

    public ResearchShowImage addImage(DisplayImage image) {
        displayImages.add(image);
        return this;
    }

    @Override
    public void renderPageContent(GuiGraphics graphics, GuiCodex gui, int basePosX, int basePosY, Font fontRenderer) {
        super.renderPageContent(graphics, gui, basePosX, basePosY, fontRenderer);
        for (DisplayImage image : displayImages) {
            int slotX = basePosX + image.pagePosX + 43;
            int slotY = basePosY + image.pagePosY + 43;
            graphics.blit(image.resource, slotX, slotY, image.uvOffsetX, image.uvOffsetY, image.uvWidth, image.uvHeight, image.width, image.height);
        }
    }

    public static class DisplayImage {
        public ResourceLocation resource;
        public int pagePosX;
        public int pagePosY;
        public int uvWidth;
        public int uvHeight;
        public int uvOffsetX;
        public int uvOffsetY;
        public int width;
        public int height;

        public DisplayImage(ResourceLocation resource, int posX, int posY, int uvOffsetX, int uvOffsetY, int uvWidth, int uvHeight, int width, int height) {
            this.resource = resource;
            this.pagePosX = posX;
            this.pagePosY = posY;
            this.uvOffsetX = uvOffsetX;
            this.uvOffsetY = uvOffsetY;
            this.uvWidth = uvWidth;
            this.uvHeight = uvHeight;
            this.width = width;
            this.height = height;
        }
    }
}