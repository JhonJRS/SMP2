package net.sirplop.aetherworks.block;
import com.rekindled.embers.util.CapabilityCompat;

import com.rekindled.embers.block.DialBaseBlock;
import com.rekindled.embers.util.DecimalFormats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.sirplop.aetherworks.AWRegistry;
import net.sirplop.aetherworks.Aetherworks;
import net.sirplop.aetherworks.capabilities.AWCapabilities;
import net.sirplop.aetherworks.api.capabilities.IHeatCapability;
import net.sirplop.aetherworks.blockentity.HeatDialBlockEntity;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class HeatDialBlock extends DialBaseBlock {

    public static final String DIAL_TYPE = "heat";

    public HeatDialBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    protected void getBEData(Direction direction, ArrayList<Component> text, BlockEntity blockEntity, int maxLines) {
        if (blockEntity instanceof HeatDialBlockEntity dial && dial.display) {
            text.add(formatHeat(dial.heat, dial.capacity));
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState pState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos.relative(state.getValue(FACING), -1));
        if (blockEntity != null) {
            IHeatCapability cap = CapabilityCompat.getCapability(blockEntity, AWCapabilities.HEAT_CAPABILITY, state.getValue(FACING).getOpposite()).orElse(CapabilityCompat.getCapability(blockEntity, AWCapabilities.HEAT_CAPABILITY, null).orElse(null));
            if (cap != null) {
                if (cap.getHeat() >= cap.getHeatCapacity())
                    return 15;
                else if (cap.getHeat() < 1)
                    return 0;
                return (int) (Math.ceil(14.0 * cap.getHeat() / cap.getHeatCapacity()));
            }
        }
        return 0;
    }

    @OnlyIn(Dist.CLIENT)
    public static MutableComponent formatHeat(double ember, double emberCapacity) {
        DecimalFormat emberFormat = DecimalFormats.getDecimalFormat(Aetherworks.MODID + ".decimal_format.heat");
        return Component.translatable(Aetherworks.MODID + ".tooltip.heatdial.heat", emberFormat.format(ember), emberFormat.format(emberCapacity));
    }

    //@Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return AWRegistry.HEAT_DIAL_ENTITY.get().create(pPos, pState);
    }

    @Override
    public String getDialType() {
        return DIAL_TYPE;
    }
}
