package pizzart.listings;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
//? if >1.21.1
/*import net.minecraft.world.entity.EntityEquipment;*/
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.network.CommonRecipeManager;
import wily.factoryapi.util.ListMap;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyCraftingTabListing;
import wily.legacy.client.LegacyCreativeTabListing;
import wily.legacy.client.RecipeInfo;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.*;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.ScreenUtil;

import java.util.*;
import java.util.function.Consumer;

import static wily.legacy.client.screen.ControlTooltip.*;
import static wily.legacy.client.screen.ControlTooltip.COMPOUND_ICON_FUNCTION;

public class CraftingPreviewScreen extends LegacyCraftingScreen {
    protected final List<List<String>> groupsByTab;
    private final ListingsScreen parent;
    private final List<ResourceLocation> allTabsRes;
    public final ListingPack pack;
    public final boolean editExisting;
    public final List<RecipeInfo<CraftingRecipe>> allRecipes;
    public final List<RecipeInfo<CraftingRecipe>> elsewhereRecipes;
    public CraftingPreviewScreen(ListingsScreen parent, Player player, ListingPack pack) {
        super(LegacyCraftingMenu.playerCraftingMenu((int) Minecraft.getInstance().getWindow().getWindow(), new Inventory(player/*? if >1.21.1 {*//*, new EntityEquipment()*//*?}*/)), new Inventory(player/*? if >1.21.1 {*//*, new EntityEquipment()*//*?}*/), Component.literal("Crafting Screen Preview"), false);
        this.parent = parent;
        this.pack = pack;
        this.editExisting = pack.type != ListingPack.Type.ADDITIVE;
        this.groupsByTab = new ArrayList<>();
        this.allTabsRes = new ArrayList<>();
        this.elsewhereRecipes = new ArrayList<>();

        //? if >=1.20.5
        /*CraftingInput input = container.asCraftInput();*/
        this.allRecipes = CommonRecipeManager.byType(RecipeType.CRAFTING).stream().map(h-> RecipeInfo.create(h./*? if >1.20.1 {*//*id()*//*?} else {*/getId()/*?}*/, h/*? if >1.20.1 {*//*.value()*//*?}*/,h/*? if >1.20.1 {*//*.value()*//*?}*/ instanceof ShapedRecipe rcp ? LegacyCraftingMenu.updateShapedIngredients(new ArrayList<>(ingredientsGrid), LegacyCraftingMenu.getRecipeOptionalIngredients(rcp), 3, rcp.getWidth(), rcp.getHeight()) : h/*? if >1.20.1 {*//*.value()*//*?}*/ instanceof ShapelessRecipe r ? LegacyCraftingMenu.getRecipeOptionalIngredients(r) : Collections.emptyList(),h/*? if >1.20.1 {*//*.value()*//*?}*/.isSpecial() ? ItemStack.EMPTY : h/*? if >1.20.1 {*//*.value()*//*?}*/.assemble(/*? if <1.20.5 {*/container/*?} else {*//*input*//*?}*/,Minecraft.getInstance().level.registryAccess()))).filter(h->h.getOptionalIngredients().size() <= ingredientsGrid.size()).toList();
        this.menu.inventoryActive = false;

        reloadScreen(0, 0, 0);
    }

    public int getActualTab() {
        return Math.min(page.get() * getMaxTabCount() + craftingTabList.selectedTab, allTabsRes.size() - 1);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_O) {
            minecraft.setScreen(new ConfirmationScreen(this, 230, 129, Component.literal("Tab Options"), Component.empty()) {
                @Override
                protected void addButtons() {
                    renderableVList.addRenderable(Button.builder(Component.literal("Cancel"), b->minecraft.setScreen(parent)).build());
                    renderableVList.addRenderable(Button.builder(Component.literal("Create Tab"), b->minecraft.setScreen(new TabOptionsScreen(parent, Component.literal("New Tab"), Component.literal("Tab Name"), null, s->{
                        String newId = s.idBox.getValue();
                        ResourceLocation res = ResourceLocation.tryParse(newId.contains(":") ? newId : LegacyListings.MOD_ID + ":" + newId);
                        LegacyCraftingTabListing listing = new LegacyCraftingTabListing(res, Component.literal(s.renameBox.getValue()), LegacyCraftingTabListing.map.getByIndex(0).iconHolder(), new HashMap<>());
                        pack.craftingTabs.put(res, listing);
                        minecraft.setScreen(parent);
                        reloadScreen();
                    }))).build());
                    ResourceLocation id = allTabsRes.get(getActualTab());
                    LegacyCraftingTabListing prevListing = getOldListing(id);
                    Button editButton = Button.builder(Component.literal("Edit Tab"), b->minecraft.setScreen(new TabOptionsScreen(this, Component.literal("Edit Tab"), Component.literal("Tab Name"), prevListing, s->{
                        String newId = s.idBox.getValue();
                        ResourceLocation res = ResourceLocation.tryParse(newId.contains(":") ? newId : LegacyListings.MOD_ID + ":" + newId);
                        LegacyCraftingTabListing listing = new LegacyCraftingTabListing(res, Component.literal(s.renameBox.getValue()), prevListing.iconHolder(), prevListing.craftings());
                        pack.craftingTabs.put(res, listing);
                        pack.craftingTabs.remove(id);
                        minecraft.setScreen(parent);
                        reloadScreen();
                    }))).build();
                    Button deleteButton = Button.builder(Component.literal("Delete Tab"), b->minecraft.setScreen(new ConfirmationScreen(this, Component.literal("Delete Tab"), Component.literal("Are you sure you want to delete this tab? All of your groups in it will be lost!"), s->{
                        pack.craftingTabs.remove(id);
                        minecraft.setScreen(parent);
                        reloadScreen();
                    }))).build();
                    editButton.active = getActualTab() >= LegacyCreativeTabListing.map.size() - 1 || editExisting;
                    deleteButton.active = getActualTab() >= LegacyCreativeTabListing.map.size() - 1 || (editExisting && allTabsRes.size() > 1);
                    renderableVList.addRenderable(editButton);
                    renderableVList.addRenderable(deleteButton);
                }
            });
            return true;
        }
        if (keyCode == InputConstants.KEY_X) {
            ResourceLocation id = allTabsRes.get(getActualTab());
            LegacyCraftingTabListing listing = getOldListing(id);
            newGroup(listing);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public LegacyCraftingTabListing getOldListing(ResourceLocation id) {
        return LegacyCraftingTabListing.map.containsKey(id) && !editExisting ? LegacyCraftingTabListing.map.get(id) : pack.craftingTabs.getOrDefault(id, null);
    }

    public void editGroup(LegacyCraftingTabListing listing, String group, @Nullable List<RecipeInfo.Filter> defaultRecipes, boolean isNew) {
        List<RecipeInfo.Filter> listingRecipes = listing.craftings().getOrDefault(group, new ArrayList<>());
        minecraft.setScreen(new RecipeViewerScreen(this, group, allRecipes, elsewhereRecipes, defaultRecipes, listingRecipes, s -> {
            if ((s.selectedItems.isEmpty() && s.defaultItems.isEmpty()) || s.groupId == null) {
                listing.craftings().remove(group);
            } else {
                if (!editExisting) s.selectedItems.removeAll(s.defaultItems);
                if (!s.groupId.equals(group)) listing.craftings().remove(group);
                if (!s.selectedItems.isEmpty() || (LegacyCraftingTabListing.map.containsKey(listing.id()) && LegacyCraftingTabListing.map.get(listing.id()).craftings().containsKey(group))) {
                    List<RecipeInfo.Filter> craftings = s.selectedItems.stream().map(i -> (RecipeInfo.Filter) new RecipeInfo.Filter.Id(s.itemToRecipe.get(i).getId())).toList();
                    listing.craftings().put(s.groupId, craftings);
                }
            }
            reloadScreen();
        }, s-> {
            if (s.selectedItems.isEmpty() && s.defaultItems.isEmpty() && isNew) {
                listing.craftings().remove(group);
            }
            reloadScreen();
        }, Component.literal("Edit Group")));
    }

    public void editGroup(int groupIndex) {
        ResourceLocation id = allTabsRes.get(getActualTab());
        LegacyCraftingTabListing oldListing = getOldListing(id);
        LegacyCraftingTabListing newListing;

        if (oldListing == null) return;

        if (!pack.craftingTabs.containsKey(id)) {
            newListing = new LegacyCraftingTabListing(oldListing.id(), oldListing.name(), oldListing.iconHolder(), new HashMap<>());
            pack.craftingTabs.put(id, newListing);
        } else {
            newListing = pack.craftingTabs.get(id);
        }
        if (groupIndex >= groupsByTab.get(getActualTab()).size()) {
            newGroup(newListing);
            return;
        }
        String group = groupsByTab.get(getActualTab()).get(groupIndex);
        List<RecipeInfo.Filter> oldRecipes = LegacyCraftingTabListing.map.containsKey(id) && !editExisting ? oldListing.craftings().get(group) : null;
        editGroup(newListing, group, oldRecipes, false);
    }

    public void newGroup(LegacyCraftingTabListing listing) {
        EditBox renameBox = new EditBox(Minecraft.getInstance().font, 0,0,200,20, Component.literal("Group ID"));
        minecraft.setScreen(new ConfirmationScreen(this, 230, 120, Component.literal("New Group"), Component.literal("Group ID"), b -> {
            listing.craftings().put(renameBox.getValue(), new ArrayList<>());
            editGroup(listing, renameBox.getValue(), null, true);
        }) {
            @Override
            protected void init() {
                super.init();
                renameBox.setValue("group_id");
                renameBox.setPosition(panel.getX() + 15, panel.getY() + 45);
                renameBox.setResponder(s-> okButton.active = !StringUtil.isNullOrEmpty(s));
                addRenderableWidget(renameBox);
            }
        });
    }

    private void reloadScreen() {
        reloadScreen(getActualTab(), selectedCraftingButton, craftingButtonsOffset.get());
    }

    private void reloadScreen(int tabIdx, int buttonIdx, int offsetIdx) {
//        minecraft.setScreen(parent);
//        parent.setPreviewScreen(pack, page.get() * getMaxTabCount() + craftingTabList.selectedTab, selectedCraftingButton, craftingButtonsOffset.get());
        allTabsRes.clear();
        recipesByTab.clear();
        groupsByTab.clear();
        craftingTabList.tabButtons.clear();
        page.max = 0;

        if (!editExisting) {
            for (int i = 0; i < LegacyCraftingTabListing.map.size(); i++) {
                allTabsRes.add(LegacyCraftingTabListing.map.getKeyByIndex(i));
            }
        }
        for (int i = 0; i < pack.craftingTabs.size(); i++) {
            ResourceLocation res = pack.craftingTabs.getKeyByIndex(i);
            if (!allTabsRes.contains(res)) allTabsRes.add(res);
        }

        for (ResourceLocation res : allTabsRes) {
            LegacyCraftingTabListing listing = LegacyCraftingTabListing.map.containsKey(res) && !editExisting ? LegacyCraftingTabListing.map.get(res) : pack.craftingTabs.get(res);
            List<List<RecipeInfo<CraftingRecipe>>> groups = new ArrayList<>();
            List<String> groupNames = new ArrayList<>();
            ListMap<String, List<RecipeInfo.Filter>> fullGroups = new ListMap<>();
            if (LegacyCraftingTabListing.map.containsKey(res) && !editExisting)
                LegacyCraftingTabListing.map.get(res).craftings().forEach((s,l)->fullGroups.put(s, new ArrayList<>(l)));
            if (pack.craftingTabs.containsKey(res)) {
                Map<String, List<RecipeInfo.Filter>> map = pack.craftingTabs.get(res).craftings();
                fullGroups.forEach((s, l) -> {
                    if (map.containsKey(s)) l.addAll(map.get(s));
                });
                map.forEach(fullGroups::putIfAbsent);
            }
            fullGroups.forEach((s,l)->{
                List<RecipeInfo<CraftingRecipe>> group = new ArrayList<>();
                l.forEach(v->v.addRecipes(allRecipes,group::add));
                group.removeIf(i->i.isInvalid() || i.getOptionalIngredients().size() > ingredientsGrid.size());
                if (!group.isEmpty() || (pack.craftingTabs.containsKey(res) && pack.craftingTabs.get(res).craftings().containsKey(s))) {
                    groups.add(group);
                    groupNames.add(s);
                    elsewhereRecipes.addAll(group);
                }
            });

            recipesByTab.add(groups);
            groupsByTab.add(groupNames);
            craftingTabList.addTabButton(43, LegacyTabButton.Type.MIDDLE, listing.icon(), listing.name(), t->resetElements());
        }
        tabIdx = Math.min(tabIdx, allTabsRes.size());

        craftingTabList.selectedTab = Math.min(tabIdx % getMaxTabCount(), craftingTabList.tabButtons.size() - 1);
        page.set(Math.min(tabIdx / getMaxTabCount(), page.max));
        resetElements(true);
        selectedCraftingButton = buttonIdx;
        craftingButtonsOffset.set(Math.min(offsetIdx, craftingButtonsOffset.max));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
//        if (getCraftingButtons().get(selectedCraftingButton) instanceof RecipeIconHolder<?> h) h.updateRecipeDisplay();
        super.render(guiGraphics, i, j, f);
    }

    //? if >1.20.1 {
    /*@Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderTransparentBackground(guiGraphics);
        renderBg(guiGraphics, f, i, j);
    }
    *///?} else {
    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        ScreenUtil.renderTransparentBackground(guiGraphics);
    }
    //?}

    @Override
    protected void addCraftingButtons() {
        for (int i = 0; i < 12; i++) {
            int index = i;

            RecipeIconHolder<CraftingRecipe> h;
            craftingButtons.add(h = new RecipeIconHolder<>(leftPos + 13 + i * 27, topPos + 38) {
                @Override
                public void render(GuiGraphics graphics, int i, int j, float f) {
                    if (isFocused()) selectedCraftingButton = index;
                    super.render(graphics, i, j, f);
                }

                @Override
                public void renderItem(GuiGraphics graphics, int i, int j, float f) {
                    if (!isValidIndex()) return;
                    renderItem(graphics, itemIcon, x, y, false);
                }

                @Override
                public void renderTooltip(Minecraft minecraft, GuiGraphics graphics, ItemStack stack, int i, int j) {
                    super.renderTooltip(minecraft, graphics, stack, i, j);
                    if (stack.isEmpty() && hasGroup()) Legacy4JClient.applyFontOverrideIf(ScreenUtil.is720p(), MOJANGLES_11_FONT, b->graphics.renderTooltip(minecraft.font, Component.literal("Group is empty! May cause issues.").withStyle(ChatFormatting.ITALIC), i, j));
                }

                @Override
                protected boolean canCraft(RecipeInfo<CraftingRecipe> rcp) {
                    return false;
                }

                private boolean hasGroup() {
                    List<List<RecipeInfo<CraftingRecipe>>> list = recipesByTab.get(getActualTab());
                    return list.size() > craftingButtonsOffset.get() + index;
                }

                protected List<RecipeInfo<CraftingRecipe>> getRecipes() {
                    List<List<RecipeInfo<CraftingRecipe>>> list = recipesByTab.get(getActualTab());
                    return list.size() <= craftingButtonsOffset.get() + index ? Collections.emptyList() : list.get(craftingButtonsOffset.get() + index);
                }

                @Override
                protected void toggleCraftableRecipes() {}

                @Override
                public boolean keyPressed(int i, int j, int k) {
                    if (controlCyclicNavigation(i, index, craftingButtons, craftingButtonsOffset, scrollRenderer, CraftingPreviewScreen.this)) return true;
                    return super.keyPressed(i, j, k);
                }

                protected void updateRecipeDisplay(RecipeInfo<CraftingRecipe> rcp) {
                    scrollableRenderer.scrolled.set(0);
                    resultStack = getFocusedResult();
                    clearIngredients(ingredientsGrid);
                    if (rcp == null) return;
                    for (int i = 0; i < rcp.getOptionalIngredients().size(); i++)
                        ingredientsGrid.set(i, rcp.getOptionalIngredients().get(i));
                }

                @Override
                public void onPress() {
                    if (isFocused() && isValidIndex()) editGroup(selectedCraftingButton + craftingButtonsOffset.get());
                }

                @Override
                public void craft() {}

                @Override
                public @Nullable Component getAction(Context context) {
                    return context.actionOfContext(KeyContext.class,c-> c.key() == InputConstants.KEY_RETURN && hasGroup() && isFocused() ? Component.literal("Edit Group") : null);
                }
            });
            h.offset = LegacyCraftingMenu.DEFAULT_INVENTORY_OFFSET;
        }
    }

    @Override
    protected boolean canCraft(List<Optional< Ingredient >> ingredients, boolean isFocused) {
        return false;
    }

    @Override
    public boolean hasTypeTabList() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void onClose() {
        ScreenUtil.playBackSound();
        this.minecraft.setScreen(parent);
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        ControlTooltip.setupDefaultButtons(renderer,this);
        ControlTooltip.setupDefaultContainerScreen(renderer, (LegacyMenuAccess<?>) this);
        renderer.
                add(EXTRA::get, ()-> Component.literal("Add Group")).
                add(OPTION::get, ()-> Component.literal("Tab Options")).
                add(()-> ControlType.getActiveType().isKbm() ? COMPOUND_ICON_FUNCTION.apply(new Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT),ControlTooltip.PLUS_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET),ControlTooltip.SPACE_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET)}) : COMPOUND_ICON_FUNCTION.apply(new Icon[]{ControllerBinding.LEFT_TRIGGER.getIcon(),ControlTooltip.SPACE_ICON, ControllerBinding.RIGHT_TRIGGER.getIcon()}),()->hasTypeTabList() ? LegacyComponents.TYPE : null).
                addCompound(()-> new Icon[]{ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET) : ControllerBinding.LEFT_BUMPER.getIcon(),ControlTooltip.SPACE_ICON, ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET) : ControllerBinding.RIGHT_BUMPER.getIcon()},()->LegacyComponents.GROUP).
                add(()-> page.max > 0 ? ControlType.getActiveType().isKbm() ? COMPOUND_ICON_FUNCTION.apply(new Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT),ControlTooltip.PLUS_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT),ControlTooltip.SPACE_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT)}) : ControllerBinding.RIGHT_STICK.getIcon() : null,()->LegacyComponents.PAGE);
    }

    class TabOptionsScreen extends ConfirmationScreen {
        private final Consumer<TabOptionsScreen> confirmAction;
        @Nullable
        private final LegacyCraftingTabListing tabListing;
        private final EditBox renameBox = new EditBox(Minecraft.getInstance().font, width / 2 - 100, 0, 200, 20, Component.literal("Tab Name"));
        private final EditBox idBox = new EditBox(Minecraft.getInstance().font, width / 2 - 100, 0, 200, 20, Component.literal("Tab Name"));
        TabOptionsScreen(Screen parent, Component title, Component message, @Nullable LegacyCraftingTabListing tab, Consumer<TabOptionsScreen> confirmAction) {
            super(parent, 230, 129, title, message, (b)->{});
            this.parent = parent;
            this.tabListing = tab;
            this.confirmAction = confirmAction;
        }

        @Override
        protected void addButtons() {
            renameBox.setValue(tabListing != null ? tabListing.name().getString() : "Crafting Tab");
            idBox.setValue(tabListing != null ? tabListing.id().toString() : "tab_id");
            renderableVList.addRenderable(renameBox);
            renderableVList.addRenderable(idBox);
            renderableVList.addRenderable(Button.builder(Component.translatable("gui.ok"), b-> {
                minecraft.setScreen(parent);
                confirmAction.accept(this);
            }).build());
            renderableVList.addRenderable(Button.builder(Component.translatable("gui.cancel"), b-> minecraft.setScreen(parent)).bounds(panel.x + 15, panel.y + panel.height - 96,200,20).build());
        }
    }
}
