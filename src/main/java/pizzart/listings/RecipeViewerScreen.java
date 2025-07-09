package pizzart.listings;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.client.*;
import wily.legacy.client.screen.*;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.ScreenUtil;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static wily.legacy.client.screen.ControlTooltip.EXTRA;
import static wily.legacy.client.screen.RecipeIconHolder.getActualItem;

public class RecipeViewerScreen extends PanelBackgroundScreen implements LegacyMenuAccess<AbstractContainerMenu> {
    public static final Container layerSelectionGrid = new SimpleContainer(50);
    public final List<ItemStack> layerItems = new ArrayList<>();
    protected final Stocker.Sizeable scrolledList = new Stocker.Sizeable(0);

    protected final AbstractContainerMenu menu;
    protected final LegacyScroller scroller = LegacyScroller.create(135, ()->scrolledList);
    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel, 188);
    protected final EditBox searchBox = new EditBox(Minecraft.getInstance().font, 0, 0, 200, 20, LegacyComponents.SEARCH_ITEMS);
    protected Slot hoveredSlot = null;
    protected final Consumer<RecipeViewerScreen> applyRecipes;
    protected final Consumer<RecipeViewerScreen> applyClose;
    protected final HashMap<ItemStack, RecipeInfo<CraftingRecipe>> itemToRecipe;
    protected final List<RecipeInfo<CraftingRecipe>> sortedRecipes;
    protected final List<ItemStack> elsewhereItems;
    protected final List<ItemStack> selectedItems;
    protected final List<ItemStack> initialItems;
    protected final List<ItemStack> defaultItems;
    protected final List<Slot> draggedSlots;
    protected String groupId;
    protected int hoveredPreviewItem = -1;
    protected int selectedPreviewItem = -1;
    protected List<Optional<Ingredient>> ingredientsGrid;

    public RecipeViewerScreen(Screen parent, String groupId, List<RecipeInfo<CraftingRecipe>> allRecipes, List<RecipeInfo<CraftingRecipe>> elsewhereRecipes, List<RecipeInfo.Filter> defaultRecipes, List<RecipeInfo.Filter> activeRecipes, Consumer<RecipeViewerScreen> applyRecipes, Consumer<RecipeViewerScreen> applyClose, Component component) {
        super(parent, s -> Panel.centered(s, 325, 245), component);
        this.groupId = groupId;
        this.applyRecipes = applyRecipes;
        this.applyClose = applyClose;
        this.itemToRecipe = new HashMap<>();
        allRecipes.forEach(r-> {
            if (!r.isInvalid()) this.itemToRecipe.put(r.getResultItem(), r);
        });
        this.sortedRecipes = this.itemToRecipe.values().stream().sorted(Comparator.comparing(r->r.getResultItem().getDisplayName().getString())).toList();
        this.elsewhereItems = elsewhereRecipes.stream().map(RecipeInfo::getResultItem).toList();
        this.ingredientsGrid = new ArrayList<>(Collections.nCopies(9, Optional.empty()));
        this.initialItems = new ArrayList<>();
        this.selectedItems = new ArrayList<>();
        this.defaultItems = new ArrayList<>();
        if (defaultRecipes != null) defaultRecipes.forEach(f->f.addRecipes(this.itemToRecipe.values(), r->this.defaultItems.add(r.getResultItem())));
        if (activeRecipes != null) activeRecipes.forEach(f->f.addRecipes(this.itemToRecipe.values(), r->this.selectedItems.add(r.getResultItem())));
        this.initialItems.addAll(this.defaultItems);
        this.initialItems.addAll(this.selectedItems);
        this.draggedSlots = new ArrayList<>();

        this.menu = new AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player player, int i) {
                return null;
            }
            @Override
            public boolean stillValid(Player player) {
                return false;
            }
        };

        for (int i = 0; i < layerSelectionGrid.getContainerSize(); i++) {
            this.menu.slots.add(LegacySlotDisplay.override(new Slot(layerSelectionGrid, i, 23 + i % 10 * 27, 50 + i / 10 * 27), new LegacySlotDisplay() {
                public int getWidth() {
                    return 27;
                }
                public int getHeight() {
                    return 27;
                }
            }));
        }

        this.scrolledList.max = Math.max(0, (layerItems.size() - 1) / layerSelectionGrid.getContainerSize());

        this.searchBox.setResponder(s -> {
            fillLayerGrid();
            scrolledList.set(0);
        });
        this.searchBox.setMaxLength(50);
    }

    public static int compareDistances(String str1, String str2, String input) {
        double dist1 = StringUtils.getJaroWinklerDistance(str1, input);
        double dist2 = StringUtils.getJaroWinklerDistance(str2, input);
        return Double.compare(dist1, dist2);
    }

    public static <T> int compareContaining(T x, T y, List<T> list) {
        boolean contains1 = list.contains(x);
        boolean contains2 = list.contains(y);
        return Boolean.compare(contains1, contains2);
    }

    @Override
    public void onClose() {
        super.onClose();
        applyClose.accept(this);
    }

    public void fillLayerGrid() {
        String query = searchBox.getValue().strip().toLowerCase();
        Stream<RecipeInfo<CraftingRecipe>> stream = sortedRecipes.stream();
        stream = LegacyListingsClient.onlyNotAdded ? stream.filter(r->!elsewhereItems.contains(r.getResultItem())) : stream;
        stream = LegacyListingsClient.onlyModded ? stream.filter(r->!r.getId().getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) : stream;
        stream = query.startsWith("@") ? stream.filter(r->r.getId().getNamespace().toLowerCase().contains(query.replace("@", ""))) : stream;
        stream = !query.isEmpty() && !query.startsWith("@") ?
                stream
                        .filter(r->r.getResultItem().getDisplayName().getString().toLowerCase().contains(query))
                        .sorted((i, j) -> -compareDistances(i.getName().getString(), j.getName().getString(), query)) :
                stream
                        .sorted((i, j) -> -compareContaining(i.getResultItem(), j.getResultItem(), initialItems));
        List<ItemStack> list = stream.map(RecipeInfo::getResultItem).toList();
        for (int i = 0; i < layerSelectionGrid.getContainerSize(); i++) {
            int index = scrolledList.get() * 50 + i;
            layerSelectionGrid.setItem(i, list.size() > index ? itemToRecipe.containsKey(list.get(index)) ? list.get(index) : ItemStack.EMPTY : ItemStack.EMPTY);
        }
        scrolledList.max = Math.max(0, (list.size() - 1) / layerSelectionGrid.getContainerSize());
    }

    @Override
    protected void init() {
        panel.init();
        scroller.setPosition(panel.x + 299, panel.y + 23);
        scroller.offset(new Vec3(0.5f, 0, 0));
        tooltipBox.init();
        fillLayerGrid();
        Component title = Component.literal(groupId);
        addRenderableOnly(SimpleLayoutRenderable.createDrawString(title, panel.x + (panel.width - font.width(title)) / 2, panel.y + 9, font.width(title), 9, CommonColor.INVENTORY_GRAY_TEXT.get(), false));
        scroller.setPosition(panel.x + 299, panel.y + 50);
        searchBox.setPosition(panel.x + (panel.width - searchBox.getWidth()) / 2, panel.y + 22);
        addRenderableWidget(searchBox);
        addRenderableWidget(new TickBox(panel.x + 57, panel.y + 186, LegacyListingsClient.onlyNotAdded, b->Component.literal("Only Not Added"), m->null, b->{
            LegacyListingsClient.onlyNotAdded = b.selected;
            fillLayerGrid();
            scrolledList.set(0);
        }));
        addRenderableWidget(new TickBox(panel.x + 57, panel.y + 200, LegacyListingsClient.onlyModded, b->Component.literal("Only Modded"), m->null, b->{
            LegacyListingsClient.onlyModded = b.selected;
            fillLayerGrid();
            scrolledList.set(0);
        }));
        Button doneButton = Button.builder(Component.translatable("gui.done"), b -> {
            if (selectedItems.isEmpty() && defaultItems.isEmpty()) {
                minecraft.setScreen(new ConfirmationScreen(null, Component.literal("No Recipes Selected"), Component.literal("No recipes have been selected, as such the group will be deleted. Continue?"), s->{
                    super.onClose();
                    applyRecipes.accept(this);
                }) {
                    @Override
                    public void onClose() {
                        super.onClose();
                        minecraft.setScreen(RecipeViewerScreen.this);
                    }
                });
            } else {
                super.onClose();
                applyRecipes.accept(this);
            }
        }).bounds(panel.x + 57, panel.y + 216, 200, 20).build();
        addRenderableWidget(doneButton);
        setFocused(doneButton);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(accessor, guiGraphics, false);
        tooltipBox.render(guiGraphics, i, j, f);
        panel.render(guiGraphics, i, j, f);
        renderScroll(guiGraphics,i,j,f);

        int maxSlotsWidth = tooltipBox.width / 21;
        List<ItemStack> fullItems = new ArrayList<>();
        fullItems.addAll(defaultItems);
        fullItems.addAll(selectedItems);
        hoveredPreviewItem = -1;
        for (int idx = 0; idx < fullItems.size(); idx++) {
            ItemStack item = fullItems.get(idx);
            LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.itemHolder(item, false);
            holder.setPos(tooltipBox.x + 10 + idx % maxSlotsWidth * holder.width, tooltipBox.y + 10 + idx / maxSlotsWidth * holder.height);
            boolean hovered = ScreenUtil.isMouseOver(i, j, holder.getXCorner(), holder.getYCorner(), holder.width, holder.height);
            if (!defaultItems.contains(item)) {
                holder.render(guiGraphics, i, j, f);
            } else {
                FactoryGuiGraphics.of(guiGraphics).setColor(0.7f, 0.7f, 0.7f, 1f);
                holder.render(guiGraphics, i, j, f);
                FactoryGuiGraphics.of(guiGraphics).setColor(1f,1f,1f,1f);
            }
            if (selectedItems.contains(item) && !defaultItems.contains(item)) {
                if (hovered) {
                    holder.renderHighlight(guiGraphics);
                    hoveredPreviewItem = selectedItems.indexOf(item);
                }
                if (selectedPreviewItem == selectedItems.indexOf(item)) {
                    FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacyIconHolder.SELECT_ICON_HIGHLIGHT,holder.x - 4,holder.y - 4,holder.width + 6,holder.height + 6);
                }
            }
        }
        int xDiff = (tooltipBox.width - 69) / 2;
        for (int index = 0; index < ingredientsGrid.size(); index++) {
            LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.itemHolder(tooltipBox.x + xDiff + index % 3 * 23, tooltipBox.y + 32 + Math.max((fullItems.size() - 1) / maxSlotsWidth, 1) * 21 + index / 3 * 23, 23, 23, getActualItem(ingredientsGrid.get(index)), false, new Vec3(0.5, 0.5, 0));
            holder.render(guiGraphics, i, j, f);
        }
    }

    protected void slotClicked(Slot slot) {
        ItemStack item = slot.getItem();
        if (defaultItems.contains(item) || item.isEmpty()) {
            ScreenUtil.playSimpleUISound(LegacyRegistries.CRAFT_FAIL.get(), 1f);
            return;
        }
        if (selectedItems.contains(item)) selectedItems.remove(item);
        else selectedItems.add(item);
        draggedSlots.add(slot);
        ScreenUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(),1f);
    }

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        if (scroller.mouseClicked(d, e, i)) fillLayerGrid();
        if (hoveredSlot != null) slotClicked(hoveredSlot);
        if (hoveredPreviewItem != -1) {
            if (selectedPreviewItem == hoveredPreviewItem) selectedPreviewItem = -1;
            else selectedPreviewItem = hoveredPreviewItem;
            ScreenUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(),1f);
        }
        return super.mouseClicked(d, e, i);
    }

    @Override
    public boolean mouseDragged(double d, double e, int i, double f, double g) {
        if (hoveredSlot != null && hoveredSlot.hasItem() && !draggedSlots.contains(hoveredSlot)) {
            draggedSlots.add(hoveredSlot);
            slotClicked(hoveredSlot);
        }
        return super.mouseDragged(d, e, i, f, g);
    }

    @Override
    public boolean mouseReleased(double d, double e, int i) {
        scroller.mouseReleased(d, e, i);
        draggedSlots.clear();
        return super.mouseReleased(d, e, i);
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*//*, double f*//*?}*/, double g) {
        if (scroller.mouseScrolled(g)) fillLayerGrid();
        return super.mouseScrolled(d, e/*? if >1.20.1 {*//*, f*//*?}*/, g);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (i == InputConstants.KEY_RETURN && selectedPreviewItem != -1) {
            selectedPreviewItem = -1;
            ScreenUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(),1f);
        }
        if ((i == InputConstants.KEY_LEFT || i == InputConstants.KEY_RIGHT) && selectedPreviewItem != -1) {
            boolean left = i == InputConstants.KEY_LEFT;
            if (left && selectedPreviewItem == 0 || !left && selectedPreviewItem >= selectedItems.size() - 1) {
                ScreenUtil.playSimpleUISound(LegacyRegistries.CRAFT_FAIL.get(), 1f);
            } else {
                int offset = left ? -1 : 1;
                ItemStack oldItem = selectedItems.get(selectedPreviewItem + offset);
                selectedItems.set(selectedPreviewItem + offset, selectedItems.get(selectedPreviewItem));
                selectedItems.set(selectedPreviewItem, oldItem);
                selectedPreviewItem += offset;
                ScreenUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(), true);
            }
            return true;
        }
        if (i == InputConstants.KEY_O && !searchBox.isFocused()) {
            EditBox renameBox = new EditBox(Minecraft.getInstance().font, width / 2 - 100, 0, 200, 20, Component.literal("Group ID"));
            minecraft.setScreen(new ConfirmationScreen(null, Component.literal("Group Options"), Component.empty(), s->{}) {
                @Override
                protected void addButtons() {
                    renderableVList.addRenderable(Button.builder(Component.literal("Cancel"), b->minecraft.setScreen(RecipeViewerScreen.this)).build());
                    Button renameButton = Button.builder(Component.literal("Rename Group"), b->minecraft.setScreen(new ConfirmationScreen(this, Component.literal("Rename Group"), Component.empty(), s->{
                        groupId = renameBox.getValue();
                    }) {
                        @Override
                        protected void addButtons() {
                            Button okButton = Button.builder(Component.translatable("gui.ok"), b-> {
                                okAction.accept(this);
                                minecraft.setScreen(parent);
                            }).build();
                            renameBox.setValue(groupId);
                            renameBox.setResponder(s-> okButton.active = !StringUtil.isNullOrEmpty(s.strip()));
                            renderableVList.addRenderable(renameBox);
                            renderableVList.addRenderable(okButton);
                            renderableVList.addRenderable(Button.builder(Component.translatable("gui.cancel"), b-> minecraft.setScreen(parent)).bounds(panel.x + 15, panel.y + panel.height - 96,200,20).build());
                        }
                    })).build();
                    Button deleteButton = Button.builder(Component.literal("Delete Group"), b->minecraft.setScreen(new ConfirmationScreen(this, Component.literal("Delete Group"), Component.literal("Are you sure you want to delete this group?"), s->{
                        groupId = null;
                        applyRecipes.accept(RecipeViewerScreen.this);
                        RecipeViewerScreen.super.onClose();
                    }))).build();
                    renameButton.active = defaultItems.isEmpty();
                    deleteButton.active = renameButton.active;
                    renderableVList.addRenderable(renameButton);
                    renderableVList.addRenderable(deleteButton);
                }
                @Override
                public void onClose() {
                    super.onClose();
                    minecraft.setScreen(RecipeViewerScreen.this);
                }
            });
        }
        return super.keyPressed(i, j, k);
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        ControlTooltip.setupDefaultScreen(renderer, this).add(EXTRA::get, () -> Component.literal("Options"));
    }

    public void setHoveredSlot(Slot slot) {
        if (this.hoveredSlot != slot) ingredientsGrid.clear();
        this.hoveredSlot = slot;
        if (slot != null) {
            ItemStack item = slot.getItem();
            if (!item.isEmpty()) ingredientsGrid.addAll(itemToRecipe.get(item).getOptionalIngredients());
        }
        if (ingredientsGrid.size() < 9)
            ingredientsGrid.addAll(Collections.nCopies(9 - ingredientsGrid.size(), Optional.empty()));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        setHoveredSlot(null);
        menu.slots.forEach(s -> {
            LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.slotBoundsWithItem(panel.x, panel.y, s);
            holder.render(guiGraphics, i, j, f);
            if (holder.isHovered) {
                if (s.isHighlightable()) holder.renderHighlight(guiGraphics);
                setHoveredSlot(s);
            }
            FactoryScreenUtil.enableBlend();
            FactoryScreenUtil.disableDepthTest();
            if (selectedItems.contains(s.getItem())) {
                FactoryGuiGraphics.of(guiGraphics).blitSprite(TickBox.TICK, holder.x, holder.y, 8, 8);
            } else if (defaultItems.contains(s.getItem())) {
                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacyIconHolder.WARNING_ICON, holder.x, holder.y, 8, 8);
            } else if (elsewhereItems.contains(s.getItem()) && !initialItems.contains(s.getItem())) {
                FactoryGuiGraphics.of(guiGraphics).setColor(0.1f, 0.9f, 0.1f, 0.4f);
                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacyIconHolder.WARNING_ICON, holder.x, holder.y, 8, 8);
                FactoryGuiGraphics.of(guiGraphics).setColor(1f,1f,1f,1f);
            }
            FactoryScreenUtil.enableDepthTest();
            FactoryScreenUtil.disableBlend();
        });
        if ((hoveredSlot != null && !hoveredSlot.getItem().isEmpty()) || hoveredPreviewItem != -1) {
            ItemStack item = hoveredPreviewItem != -1 ? selectedItems.get(hoveredPreviewItem) : hoveredSlot.getItem();
            List<Component> tooltipLines = item.getTooltipLines(/*? if >1.20.1 {*//*Item.TooltipContext.of(minecraft.level), *//*?}*/minecraft.player, TooltipFlag.NORMAL);
            tooltipLines.add(Component.literal(itemToRecipe.get(item).getId().toString()).withStyle(ChatFormatting.DARK_GRAY));
            guiGraphics.renderTooltip(font, tooltipLines, item.getTooltipImage(), i, j);
        }
    }

    protected void renderScroll(GuiGraphics guiGraphics, int i, int j, float f) {
        scroller.render(guiGraphics, i, j, f);
    }

    @Override
    public AbstractContainerMenu getMenu() {
        return menu;
    }

    @Override
    public ScreenRectangle getMenuRectangle() {
        return new ScreenRectangle(panel.x, panel.y, panel.width, panel.height);
    }

    @Override
    public boolean isOutsideClick(int i) {
        return false;
    }

    @Override
    public Slot getHoveredSlot() {
        return hoveredSlot;
    }

    @Override
    public int getTipXDiff() {
        return 0;
    }
}
