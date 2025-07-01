package pizzart.listings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.legacy.client.CommonColor;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.PanelVListScreen;

public class PackCreateScreen extends PanelVListScreen {
    public PackCreateScreen(Screen parent) {
        super(parent, s-> Panel.centered(s,255, 98,0,0), Component.literal("New Pack"));
        renderableVList.addRenderable(SimpleLayoutRenderable.createDrawString(Component.literal("Listing Pack Name"),0,1,2,9, CommonColor.INVENTORY_GRAY_TEXT.get(),false));
        EditBox renameBox = new EditBox(Minecraft.getInstance().font, width / 2 - 100, 0, 200, 20, Component.literal("Listing Pack Name"));
        renameBox.setValue("listing_pack");
        renderableVList.addRenderable(renameBox);
        renderableVList.addRenderable(Button.builder(Component.literal("Edit Listings"), b -> minecraft.setScreen(new ListingsScreen(this))).build());
        renderableVList.addRenderable(Button.builder(Component.literal("Create Pack"), b -> onCreate(renameBox.getValue())).build());
        onClose = s -> {
            LegacyListingsClient.craftingTabs.clear();
            LegacyListingsClient.creativeTabs.clear();
        };
    }

    public void onCreate(String packName) {
        if (StringUtil.isNullOrEmpty(packName)) return;
        LegacyListingsClient.savePack(LegacyListingsClient.LISTING_PACKS_PATH, true, packName, LegacyListingsClient.craftingTabs, LegacyListingsClient.creativeTabs);
//        if (status == LegacyListingsClient.SaveStatus.FILE_EXISTS) {
//            System.out.println("overwrite?");
//            minecraft.setScreen(new ConfirmationScreen(this, Component.literal("Pack Exists"), Component.literal("Do you wish to overwrite the existing pack?"), s -> {
//                LegacyListingsClient.SaveStatus newStatus = LegacyListingsClient.savePack(LegacyListingsClient.LISTING_PACKS_PATH, true, name, LegacyListingsClient.craftingTabs, LegacyListingsClient.creativeTabs);
//                if (newStatus == LegacyListingsClient.SaveStatus.IO_ERROR) {
//                    minecraft.setScreen(ConfirmationScreen.createInfoScreen(this, Component.literal("Error"), Component.literal("An error occured while saving!")));
//                }
//            }));
//        }
//        else if (status == LegacyListingsClient.SaveStatus.IO_ERROR) {
//            System.out.println("io error");
//            minecraft.setScreen(ConfirmationScreen.createInfoScreen(this, Component.literal("Error"), Component.literal("An error occured while saving!")));
//        }
        LegacyListingsClient.craftingTabs.values().forEach(l->System.out.println(l.craftings()));
        onClose();
    }
}
