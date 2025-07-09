package pizzart.listings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.legacy.client.screen.RenderableVList;
//? if fabric {
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
//?} elif forge {
/*import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
*///?} elif neoforge {
/*import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
*///?}

public class LegacyListingsClient {
    public static boolean onlyModded = false;
    public static boolean onlyNotAdded = false;

    public static void init() {
        //? if fabric {
        ScreenEvents.AFTER_INIT.register((minecraft, screen, i, j)->postScreenInit(minecraft, screen));
        //?} elif forge {
        /*MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL,false, ScreenEvent.Init.Post.class, e->postScreenInit(Minecraft.getInstance(), e.getScreen()));
        *///?} elif neoforge {
        /*NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL,false, ScreenEvent.Init.Post.class, e->postScreenInit(Minecraft.getInstance(), e.getScreen()));
        *///?}
    }

    public static void postScreenInit(Minecraft minecraft, Screen screen) {
        if (screen instanceof PauseScreen pauseScreen) {
            RenderableVList.Access listAccess = (RenderableVList.Access) pauseScreen;
            RenderableVList list = listAccess.getRenderableVList();
            for (Renderable r : list.renderables) if (r instanceof AbstractButton b && b.getMessage().equals(Component.literal("Listings"))) return;
            list.addRenderables(list.renderables.size() - 1, Button.builder(Component.literal("Listings"), b -> minecraft.setScreen(new PacksScreen(pauseScreen))).build());
            listAccess.renderableVListInit();
        }
    }
}