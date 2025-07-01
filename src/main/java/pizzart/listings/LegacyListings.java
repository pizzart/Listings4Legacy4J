package pizzart.listings;

//? if forge || neoforge
/*@Mod(LegacyListings.MOD_ID)*/
public class LegacyListings {
    public static final String MOD_ID = "legacy";

    public void init() {}

    public LegacyListings() {
        //? if forge || neoforge {
        /*if (FMLEnvironment.dist == Dist.CLIENT)
            LegacyListingsClient.init();
        *///?}
    }
}
