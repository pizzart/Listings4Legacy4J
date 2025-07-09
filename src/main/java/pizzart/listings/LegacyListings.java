package pizzart.listings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//? if >1.20.1 {
/*import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.component.DataComponentType;
*///?}

//? if forge {
/*import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;
*///?} else if neoforge {
/*import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
*///?}

//? if forge || neoforge
/*@Mod(LegacyListings.MOD_ID)*/
public class LegacyListings {
    public static final String MOD_ID = "listings";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    //? if >1.20.1 {
    /*public static final DataComponentType<ResourceLocation> RECIPE_ID_COMPONENT = DataComponentType.<ResourceLocation>builder().persistent(ResourceLocation.CODEC).build();
    *///?}

    public void init() {}

    public LegacyListings() {
        //? if forge || neoforge {
        /*if (FMLEnvironment.dist == Dist.CLIENT) {
            init();
            LegacyListingsClient.init();
        }
        *///?}
    }
}
