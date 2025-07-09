package pizzart.listings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.util.ListMap;
import wily.legacy.client.LegacyCraftingTabListing;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.RecipeInfo;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.PanelVListScreen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ListingsScreen extends PanelVListScreen {
    public ListingsScreen(Screen parent, ListingPack pack, Consumer<PanelVListScreen> onClose) {
        super(parent, s -> Panel.centered(s, 255, 40, 0, 0), Component.literal("Edit Listings"));
        this.onClose = onClose;
        if (Minecraft.getInstance().player == null) return;
        renderableVList.addRenderable(Button.builder(Component.literal("Crafting"), b -> setPreviewScreen(pack)).build());
    }

    public void setPreviewScreen(ListingPack pack) {
        boolean mod = LegacyOptions.modCraftingTabs.get();
        boolean vanilla = LegacyOptions.vanillaTabs.get();
        LegacyOptions.modCraftingTabs.set(false);
        LegacyOptions.vanillaTabs.set(false);
        if (!pack.initialized) {
            pack.initialized = true;
            if (pack.type == ListingPack.Type.FULL) {
                for (Map.Entry<ResourceLocation, LegacyCraftingTabListing> entry : LegacyCraftingTabListing.map.entrySet()) {
                    ListMap<String, List<RecipeInfo.Filter>> newMap = new ListMap<>();
                    entry.getValue().craftings().forEach((key, value) -> newMap.put(key, new ArrayList<>(value)));
                    LegacyCraftingTabListing listing = new LegacyCraftingTabListing(entry.getValue().id(), entry.getValue().name(), entry.getValue().iconHolder(), newMap);
                    pack.craftingTabs.put(entry.getKey(), listing);
                }
            } else if (pack.type == ListingPack.Type.CUSTOM) {
                ResourceLocation id = ResourceLocation.tryBuild(LegacyListings.MOD_ID, "custom");
                pack.craftingTabs.put(id, new LegacyCraftingTabListing(id, Component.literal("custom"), null, new HashMap<>()));
            }
        }
        minecraft.setScreen(new CraftingPreviewScreen(this, minecraft.player, pack));
        LegacyOptions.modCraftingTabs.set(mod);
        LegacyOptions.vanillaTabs.set(vanilla);
    }
}
