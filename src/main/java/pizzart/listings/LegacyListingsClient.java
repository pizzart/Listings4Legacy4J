package pizzart.listings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import wily.factoryapi.FactoryAPIClient;
import wily.legacy.client.screen.RenderableVList;

public class LegacyListingsClient {
    private static boolean wasPaused = false;
    public static boolean onlyModded = false;
    public static boolean onlyNotAdded = false;

    public static void init() {
        FactoryAPIClient.postTick(LegacyListingsClient::postTick);
    }

    public static void postTick(Minecraft minecraft) {
        if (!minecraft.isPaused()) wasPaused = false;
        if (minecraft.screen instanceof PauseScreen pauseScreen && minecraft.isPaused() && !wasPaused) {
            RenderableVList.Access listAccess = (RenderableVList.Access) pauseScreen;
            RenderableVList list = listAccess.getRenderableVList();
            list.addRenderables(list.renderables.size() - 1, Button.builder(Component.literal("Listings"), b -> minecraft.setScreen(new PacksScreen(pauseScreen))).build());
            listAccess.renderableVListInit();
            wasPaused = true;
        }
    }
}