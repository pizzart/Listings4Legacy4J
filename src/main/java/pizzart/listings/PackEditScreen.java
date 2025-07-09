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
import wily.legacy.client.screen.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class PackEditScreen extends PanelVListScreen {
    private final ListingPack pack;
    private final Path oldPackPath;
    private final LegacySliderButton<ListingPack.Type> typeSlider;
//    public TickBox tickBox;
    public PackEditScreen(Screen parent, ListingPack pack, Path oldPackPath, Component title, Component saveText) {
        super(parent, s-> Panel.centered(s,255,172,0,16), title);
        this.pack = pack;
        this.oldPackPath = oldPackPath;
        this.typeSlider = new LegacySliderButton<>(0, 0, 200, 16, s -> {
            String typeName = switch (s.getObjectValue()) {
                case CUSTOM -> "Custom";
                case ADDITIVE -> "Additive";
                case FULL -> "Full";
            };
            return Component.literal("Pack Type: %s".formatted(typeName));
        }, s -> Tooltip.create(Component.literal("Custom: create empty pack.\nAdditive: create pack with uneditable pre-existing listings. Pack will be saved with only the listings you've added.\nFull: create pack with editable pre-existing listings. Pack will be saved with every listing. Recommended.")),
                pack.type, () -> Arrays.stream(ListingPack.Type.values()).toList(), s ->pack.type = s.getObjectValue());
        typeSlider.active = !pack.initialized;
//        tickBox = new TickBox(0, 0, true, b->Component.literal("Edit Existing Recipes"), b-> Tooltip.create(Component.literal("If disabled, disallows the editing of existing recipes (added by the game or resource packs), the listing pack will exclusively contain the items that were added in the pack.")), b->{});
        Button editButton = Button.builder(Component.literal("Edit Listings"), b -> minecraft.setScreen(new ListingsScreen(this, pack, s-> updateSlider()))).build();
        Button saveButton = Button.builder(saveText, b -> onCreate()).build();
        EditBox renameBox = new EditBox(Minecraft.getInstance().font, width / 2 - 100, 0, 200, 20, Component.literal("Listing Pack Name"));
        MultiLineEditBox descBox = new MultiLineEditBox(Minecraft.getInstance().font, width / 2 - 100, 0, 200, 40, Component.literal("Listing pack description goes here"), Component.literal("Listing Pack Description")) {
            @Override
            public boolean keyPressed(int i, int j, int k) {
                if (i == InputConstants.KEY_DOWN) {
                    PackEditScreen.this.setFocused(typeSlider);
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
        renderableVList.addRenderable(typeSlider);
        renderableVList.addRenderable(editButton);
        renderableVList.addRenderable(saveButton);
        updateSlider();
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

    public void updateSlider() {
        if (!typeSlider.active) return;
        if (pack.modifiesOldGroup()) {
            typeSlider.active = false;
            if (typeSlider.getObjectValue() == ListingPack.Type.ADDITIVE) {
                typeSlider.setTooltip(Tooltip.create(Component.literal("A pre-existing group has been modified, but the pack type is set to additive. Not sure how that happened.")));
            }
        }
    }

    public void onCreate() {
        if (pack.isEmpty()) {
            minecraft.setScreen(new ConfirmationScreen(this, Component.literal("Empty Pack"), Component.literal("The pack is empty, it will not be saved. Continue?"), s-> super.onClose()));
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
