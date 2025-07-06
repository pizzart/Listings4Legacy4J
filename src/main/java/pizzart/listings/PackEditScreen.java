package pizzart.listings;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.legacy.client.CommonColor;
import wily.legacy.client.screen.ConfirmationScreen;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.PanelVListScreen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PackEditScreen extends PanelVListScreen {
    public ListingPack pack;
    public Path oldPackPath;
    public PackEditScreen(Screen parent, ListingPack pack, Path oldPackPath, Component title, Component saveText) {
        super(parent, s-> Panel.centered(s,255,160,0,0), title);
        this.pack = pack;
        this.oldPackPath = oldPackPath;
        Button editButton = Button.builder(Component.literal("Edit Listings"), b -> minecraft.setScreen(new ListingsScreen(this, pack))).build();
        Button saveButton = Button.builder(saveText, b -> onCreate()).build();
        EditBox renameBox = new EditBox(Minecraft.getInstance().font, width / 2 - 100, 0, 200, 20, Component.literal("Listing Pack Name"));
        MultiLineEditBox descBox = new MultiLineEditBox(Minecraft.getInstance().font, width / 2 - 100, 0, 200, 40, Component.literal("Listing pack description goes here"), Component.literal("Listing Pack Description")) {
            @Override
            public boolean keyPressed(int i, int j, int k) {
                if (i == InputConstants.KEY_DOWN) {
                    PackEditScreen.this.setFocused(editButton);
                    return true;
                }
                if (i == InputConstants.KEY_UP) {
                    PackEditScreen.this.setFocused(renameBox);
                    return true;
                }
                return super.keyPressed(i, j, k);
            }
        };
        renameBox.setValue(pack.name);
        descBox.setValue(pack.desc);
        renameBox.setResponder(s-> {
            pack.name = s;
            saveButton.active = !StringUtil.isNullOrEmpty(s.strip());
            saveButton.setTooltip(!saveButton.active ? Tooltip.create(Component.literal("Name must not be empty!")) : null);
        });
        descBox.setValueListener(s-> pack.desc = s);
        renderableVList.addRenderable(SimpleLayoutRenderable.createDrawString(Component.literal("Pack Name"),0,1,2,9, CommonColor.INVENTORY_GRAY_TEXT.get(),false));
        renderableVList.addRenderable(renameBox);
        renderableVList.addRenderable(SimpleLayoutRenderable.createDrawString(Component.literal("Pack Description"),0,1,2,9, CommonColor.INVENTORY_GRAY_TEXT.get(),false));
        renderableVList.addRenderable(descBox);
        renderableVList.addRenderable(editButton);
        renderableVList.addRenderable(saveButton);
    }

    @Override
    public void onClose() {
        if (pack.craftingTabs.isEmpty() && pack.creativeTabs.isEmpty()) {
            super.onClose();
        }
        else {
            minecraft.setScreen(new ConfirmationScreen(this, Component.literal("Unsaved Changes"), Component.literal("There are unsaved changes in the pack. Do you wish to exit without saving?"), s-> super.onClose()));
        }
    }

    public void onCreate() {
        if (pack.craftingTabs.isEmpty()) {
            minecraft.setScreen(new ConfirmationScreen(this, Component.literal("Empty Listing"), Component.literal("The listing is empty, as such the pack will not be saved. Continue?"), s-> super.onClose()));
        } else {
            if (oldPackPath != null && !pack.name.equals(oldPackPath.getFileName().toString())) {
                try {
                    Files.delete(oldPackPath);
                } catch (IOException e) {
                    LegacyListings.LOGGER.warn("Failed to delete old pack {}",oldPackPath,e);
                }
            }
            pack.save(true);
            super.onClose();
        }
    }
}
