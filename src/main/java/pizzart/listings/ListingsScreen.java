package pizzart.listings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.PanelVListScreen;

public class ListingsScreen extends PanelVListScreen {
    public ListingsScreen(Screen parent) {
        super(parent, s -> Panel.centered(s, 255, 40, 0, 0), Component.literal("Edit Listings"));
        if (Minecraft.getInstance().player == null) return;
        renderableVList.addRenderable(Button.builder(Component.literal("Crafting"), b -> setPreviewScreen()).build());
    }

    public void setPreviewScreen(int tab, int button, int offset) {
        boolean mod = LegacyOptions.modCraftingTabs.get();
        boolean vanilla = LegacyOptions.vanillaTabs.get();
        LegacyOptions.modCraftingTabs.set(false);
        LegacyOptions.vanillaTabs.set(false);
        minecraft.setScreen(new CraftingPreviewScreen(this, minecraft.player, tab, button, offset));
        LegacyOptions.modCraftingTabs.set(mod);
        LegacyOptions.vanillaTabs.set(vanilla);
    }
    public void setPreviewScreen() {
        setPreviewScreen(0, 0, 0);
    }
}
