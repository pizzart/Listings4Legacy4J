package pizzart.listings;

//? if forge {
/*import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;
*///?} else if neoforge {
/*import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
*///?}

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//? if forge || neoforge
/*@Mod(LegacyListings.MOD_ID)*/
public class LegacyListings {
    public static final String MOD_ID = "listings";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public void init() {}

    public LegacyListings() {
        //? if forge || neoforge {
        /*if (FMLEnvironment.dist == Dist.CLIENT)
            LegacyListingsClient.init();
        *///?}
    }
}
