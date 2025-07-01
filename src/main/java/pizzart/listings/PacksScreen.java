package pizzart.listings;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.PanelVListScreen;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.io.File;
import java.util.Arrays;
import java.util.function.Consumer;

public class PacksScreen extends PanelVListScreen {
    public PacksScreen(Screen parent) {
        super(parent, s-> Panel.centered( s,255, 240,0,24), Component.literal("Listing Packs"));
        renderableVList.addRenderable(new PackButton(Component.literal("New Pack"), b -> minecraft.setScreen(new PackCreateScreen(this))));
        File packsDir = LegacyListingsClient.LISTING_PACKS_PATH.toFile();
        if (packsDir.exists()) {
            File[] dirs = packsDir.listFiles(File::isDirectory);
            if (dirs != null)
                Arrays.stream(dirs).forEach(f -> renderableVList.addRenderable(new PackButton(Component.literal(f.getName()), b -> {

                })));
            //new String[]{"json"}
        }
//        manager.getLanguages().forEach(((s, languageInfo) -> renderableVList.addRenderable(new AbstractButton(0,0,260,20,languageInfo.toComponent()) {
//            @Override
//            protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
//                super.renderWidget(guiGraphics, i, j, f);
//                if (manager.getSelected().equals(s)) setFocused(true);
//            }
//
//            @Override
//            public void onPress() {
//                selectedLang = s;
//            }
//            @Override
//            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
//                defaultButtonNarrationText(narrationElementOutput);
//            }
//        })));
    }

//    public static String getSystemLanguageCode(){
//        String auto = Locale.getDefault().toString().toLowerCase(Locale.ENGLISH);
//        return Minecraft.getInstance().getLanguageManager().getLanguage(auto) != null ? auto : "en_us";
//    }
//
//    @Override
//    public void onClose() {
//        if (selectedLang != null && !minecraft.getLanguageManager().getSelected().equals(selectedLang)) {
//            minecraft.getLanguageManager().setSelected(selectedLang);
//            minecraft.options.languageCode = selectedLang;
//            this.minecraft.reloadResourcePacks();
//            minecraft.options.save();
//        }
//        super.onClose();
//    }

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
//        addRenderableWidget(LegacyConfigWidgets.createWidget(LegacyOptions.of(minecraft.options.forceUnicodeFont()),panel.x + 10, panel.y + 10, panel.width - 20, v->{}));
    }

    public class PackButton extends AbstractButton {
        private final Consumer<PackButton> doOnPress;
        public PackButton(Component component, Consumer<PackButton> onPress) {
            super(0, 0, 200, 30, component);
            this.doOnPress = onPress;
        }

        @Override
        public void onPress() {
            doOnPress.accept(this);
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
