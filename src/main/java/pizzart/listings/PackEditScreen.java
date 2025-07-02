package pizzart.listings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.legacy.client.CommonColor;
import wily.legacy.client.screen.ConfirmationScreen;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.PanelVListScreen;

public class PackEditScreen extends PanelVListScreen {
    public PackEditScreen(Screen parent, String packName) {
        super(parent, s-> Panel.centered(s,255,98,0,0), Component.literal("Edit Pack"));
        renderableVList.addRenderable(SimpleLayoutRenderable.createDrawString(Component.literal("Listing Pack Name"),0,1,2,9, CommonColor.INVENTORY_GRAY_TEXT.get(),false));
        EditBox renameBox = new EditBox(Minecraft.getInstance().font, width / 2 - 100, 0, 200, 20, Component.literal("Listing Pack Name"));
        renameBox.setValue(packName);
        renderableVList.addRenderable(renameBox);
        renderableVList.addRenderable(Button.builder(Component.literal("Edit Listings"), b -> minecraft.setScreen(new ListingsScreen(this))).build());
        renderableVList.addRenderable(Button.builder(Component.literal("Save Pack"), b -> onCreate(renameBox.getValue())).build());
        onClose = s -> {
            LegacyListingsClient.craftingTabs.clear();
            LegacyListingsClient.creativeTabs.clear();
        };
    }

    @Override
    public void onClose() {
        if (LegacyListingsClient.craftingTabs.isEmpty() && LegacyListingsClient.creativeTabs.isEmpty()) {
            super.onClose();
        }
        else {
            minecraft.setScreen(new ConfirmationScreen(this, Component.literal("Unsaved Changes"), Component.literal("There are unsaved changes in the pack. Do you wish to exit without saving?"), s->{
                super.onClose();
            }));
        }
    }

    public void onCreate(String packName) {
        if (StringUtil.isNullOrEmpty(packName)) return;
        LegacyListingsClient.savePack(LegacyListingsClient.LISTING_PACKS_PATH, true, packName, LegacyListingsClient.craftingTabs, LegacyListingsClient.creativeTabs);
        super.onClose();
    }
}
