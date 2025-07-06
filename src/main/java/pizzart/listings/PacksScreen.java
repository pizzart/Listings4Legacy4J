package pizzart.listings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyCraftingTabListing;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.*;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Consumer;

import static wily.legacy.client.screen.ControlTooltip.*;

public class PacksScreen extends PanelVListScreen {
    public static final Path RESOURCE_PACKS_PATH = Minecraft.getInstance().gameDirectory.toPath().resolve("resourcepacks");

    public PacksScreen(Screen parent) {
        super(parent, s-> Panel.centered(s,255, 240,0,24), Component.literal("Listing Packs"));
        renderableVList.addRenderable(new PackButton(Component.literal("New Pack"), b -> minecraft.setScreen(new PackEditScreen(this, new ListingPack(), null, Component.literal("New Pack"), Component.literal("Create Pack"))), null, null));
        File packsDir = ListingPack.LISTING_PACKS_PATH.toFile();
        if (packsDir.exists()) {
            File[] dirs = packsDir.listFiles(File::isDirectory);
            if (dirs != null)
                Arrays.stream(dirs).forEach(f -> renderableVList.addRenderable(new PackButton(Component.literal(f.getName()), b -> {
                    Path craftingsPath = f.toPath().resolve("assets").resolve("legacy").resolve("crafting_tab_listing.json");
                    Path metaPath = f.toPath().resolve("pack.mcmeta");
                    if (!Files.exists(craftingsPath)) {
                        minecraft.setScreen(ConfirmationScreen.createInfoScreen(this, Component.literal("Missing File"), Component.literal("The crafting_tab_listing.json file does not exist or is misplaced! (intended path: <packname>/assets/legacy/crafting_tab_listing.json)")));
                        return;
                    }
                    if (!Files.exists(metaPath)) {
                        minecraft.setScreen(ConfirmationScreen.createInfoScreen(this, Component.literal("Missing File"), Component.literal("The pack.mcmeta file is missing!")));
                        return;
                    }
                    ListingPack pack = new ListingPack(f.getName());
                    try (BufferedReader bufferedReader = Files.newBufferedReader(craftingsPath)) {
                        JsonElement element = JsonParser.parseReader(bufferedReader);
                        if (element instanceof JsonArray a) a.forEach(e-> LegacyCraftingTabListing.CODEC.parse(JsonOps.INSTANCE, e).result().ifPresent(listing -> {
                            if (pack.craftingTabs.containsKey(listing.id())) {
                                pack.craftingTabs.get(listing.id()).addFrom(listing);
                            } else if (listing.isValid())
                                pack.craftingTabs.put(listing.id(), listing);
                        }));
                        BufferedReader metadataReader = Files.newBufferedReader(metaPath);
                        JsonElement metaElement = JsonParser.parseReader(metadataReader);
                        if (metaElement instanceof JsonObject o && o.asMap().containsKey("pack")) {
                            //? if >1.20.1 {
                            PackMetadataSection meta = PackMetadataSection.CODEC.parse(JsonOps.INSTANCE, o.asMap().get("pack")).result().orElseThrow();
                            pack.desc = meta.description().getString();
                            pack.packFormat = meta.packFormat();
                            //?} else {
                            /*PackMetadataSection meta = PackMetadataSection.TYPE.fromJson(o.getAsJsonObject("pack"));
                            pack.desc = meta.getDescription().getString();
                            pack.packFormat = meta.getPackFormat();
                            *///?}
                        }
                        minecraft.setScreen(new PackEditScreen(this, pack, f.toPath(), Component.literal("Edit Pack"), Component.literal("Save Pack")));
                    } catch (IOException e) {
                        minecraft.setScreen(ConfirmationScreen.createInfoScreen(this, Component.literal("Read Failure"), Component.literal(e.getMessage())));
                    } catch (NullPointerException e) {
                        minecraft.setScreen(ConfirmationScreen.createInfoScreen(this, Component.literal("Read Failure"), Component.literal("Null pointer exception! Your listings file can't be read properly.")));
                    }
                }, b->{
                    try {
                        FileUtils.deleteDirectory(f);
                    } catch (IOException e) {
                        minecraft.setScreen(ConfirmationScreen.createInfoScreen(this, Component.literal("Deletion Failure"), Component.literal(e.getMessage())));
                    }
                }, b->{
                    Path resPack = RESOURCE_PACKS_PATH.resolve(f.getName());
                    if (resPack.toFile().exists()) {
                        try {
                            FileUtils.deleteDirectory(resPack.toFile());
                        } catch (IOException e) {
                            minecraft.setScreen(ConfirmationScreen.createInfoScreen(this, Component.literal("Deletion Failure"), Component.literal(e.getMessage())));
                            return;
                        }
                    }
                    try {
                        FileUtils.copyDirectory(f, resPack.toFile());
                    } catch (IOException e) {
                        minecraft.setScreen(ConfirmationScreen.createInfoScreen(this, Component.literal("Copy Failure"), Component.literal(e.getMessage())));
                    }
                })));
        }
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(accessor, guiGraphics, true);
        panel.render(guiGraphics, i, j, f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL_RECESS, accessor.getInteger("panelRecess.x", panel.x + 9), accessor.getInteger("panelRecess.y", panel.y + 9), accessor.getInteger("panelRecess.width", panel.width - 18), accessor.getInteger("panelRecess.height", panel.height - 18));
    }

    @Override
    protected void init() {
        panel.init();
        addRenderableOnly(panel);
        addRenderableOnly(((guiGraphics, i, j, f) -> FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL_RECESS,panel.x + 6, panel.y + 6, panel.width - 12, panel.height - 12)));
        getRenderableVList().init(panel.x + 10,panel.y + 10,panel.width - 20,panel.height - 20);
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer
                .add(OPTION::get, ()-> getFocused() instanceof PackButton b && b.doOnDelete != null ? Component.literal("Delete") : null)
                .add(EXTRA::get, ()-> getFocused() instanceof PackButton b && b.doOnDelete != null ? Component.literal("Copy") : null);
    }

    public class PackButton extends AbstractButton {
        private final Consumer<PackButton> doOnPress;
        @Nullable
        public final Consumer<PackButton> doOnDelete;
        @Nullable
        public final Consumer<PackButton> doOnCopy;
        public PackButton(Component component, @NotNull Consumer<PackButton> onPress, @Nullable Consumer<PackButton> onDelete, @Nullable Consumer<PackButton> onCopy) {
            super(0, 0, 200, 30, component);
            this.doOnPress = onPress;
            this.doOnDelete = onDelete;
            this.doOnCopy = onCopy;
        }

        @Override
        public void onPress() {
            doOnPress.accept(this);
        }

        @Override
        public boolean keyPressed(int i, int j, int k) {
            if (i == InputConstants.KEY_O && doOnDelete != null) {
                minecraft.setScreen(new ConfirmationScreen(PacksScreen.this, Component.literal("Delete Pack"), Component.literal("Are you sure you want to delete this pack?"), s->{
                    doOnDelete.accept(this);
                    minecraft.setScreen(parent);
                }));
                return true;
            }
            if (i == InputConstants.KEY_X && doOnCopy != null) {
                minecraft.setScreen(new ConfirmationScreen(PacksScreen.this, Component.literal("Apply Pack"), Component.literal("Copy pack to the resourcepack folder?"), s->{
                    doOnCopy.accept(this);
                    minecraft.setScreen(parent);
                }));
                return true;
            }
            return super.keyPressed(i, j, k);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }

        @Override
        protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
            ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), this.getX() + 35, this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), j, true);
        }
    }
}
