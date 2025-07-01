package pizzart.listings;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.legacy.client.CommonColor;
import wily.legacy.client.RecipeInfo;
import wily.legacy.client.screen.*;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.ScreenUtil;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static wily.legacy.client.screen.RecipeIconHolder.getActualItem;

public class RecipeViewerScreen extends ItemViewerScreen {
    protected final Consumer<RecipeViewerScreen> applyRecipes;
    protected final Consumer<RecipeViewerScreen> applyClose;
    protected final HashMap<ItemStack, RecipeInfo<CraftingRecipe>> itemToRecipe;
    protected final List<RecipeInfo<CraftingRecipe>> sortedRecipes;
    protected final List<ItemStack> selectedItems;
    protected final List<ItemStack> initialItems;
    protected final List<ItemStack> defaultItems;
    protected final String groupId;
    protected final Panel tooltipBox = Panel.tooltipBoxOf(panel, 188);
    protected final EditBox searchBox = new EditBox(Minecraft.getInstance().font, 0, 0, 200, 20, LegacyComponents.SEARCH_ITEMS);
    protected List<Optional<Ingredient>> ingredientsGrid;

    public RecipeViewerScreen(Screen parent, String groupId, List<RecipeInfo<CraftingRecipe>> allRecipes, List<RecipeInfo.Filter> defaultRecipes, List<RecipeInfo.Filter> activeRecipes, Consumer<RecipeViewerScreen> applyRecipes, Consumer<RecipeViewerScreen> applyClose, Component component) {
        super(parent, s -> Panel.centered(s, 325, 245), component);
        this.groupId = groupId;
        this.applyRecipes = applyRecipes;
        this.applyClose = applyClose;
        this.itemToRecipe = new HashMap<>();
        allRecipes.forEach(r-> {
            if (!r.isInvalid()) this.itemToRecipe.put(r.getResultItem(), r);
        });
        this.sortedRecipes = this.itemToRecipe.values().stream().sorted(Comparator.comparing(r->r.getResultItem().getDisplayName().getString())).toList();
        this.ingredientsGrid = new ArrayList<>(Collections.nCopies(9, Optional.empty()));
        this.initialItems = new ArrayList<>();
        this.selectedItems = new ArrayList<>();
        this.defaultItems = new ArrayList<>();
        if (defaultRecipes != null) defaultRecipes.forEach(f->f.addRecipes(this.itemToRecipe.values(), r->this.defaultItems.add(r.getResultItem())));
        if (activeRecipes != null) activeRecipes.forEach(f->f.addRecipes(this.itemToRecipe.values(), r->this.selectedItems.add(r.getResultItem())));
        this.initialItems.addAll(this.defaultItems);
        this.initialItems.addAll(this.selectedItems);

        menu.slots.clear();
        for (int i = 0; i < layerSelectionGrid.getContainerSize(); i++) {
            menu.slots.add(LegacySlotDisplay.override(new Slot(layerSelectionGrid, i, 23 + i % 10 * 27, 50 + i / 10 * 27), new LegacySlotDisplay() {
                public int getWidth() {
                    return 27;
                }
                public int getHeight() {
                    return 27;
                }
            }));
        }

        searchBox.setResponder(s -> {
            fillLayerGrid();
            scrolledList.set(0);
        });
        searchBox.setMaxLength(50);
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
    protected void addLayerItems() {}

    @Override
    public void onClose() {
        super.onClose();
        applyClose.accept(this);
    }

    @Override
    public void fillLayerGrid() {
        Stream<RecipeInfo<CraftingRecipe>> stream = (!searchBox.getValue().isEmpty() ? sortedRecipes.stream().sorted((i, j) -> -compareDistances(i.getName().getString(), j.getName().getString(), searchBox.getValue())) : sortedRecipes.stream().sorted((i, j) -> -compareContaining(i.getResultItem(), j.getResultItem(), initialItems)));
        List<ItemStack> list = LegacyListingsClient.onlyModded ? stream.filter(r->!r.getId().getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)).map(RecipeInfo::getResultItem).toList() : stream.map(RecipeInfo::getResultItem).toList();
        for (int i = 0; i < layerSelectionGrid.getContainerSize(); i++) {
            int index = scrolledList.get() * 50 + i;
            layerSelectionGrid.setItem(i, list.size() > index ? itemToRecipe.containsKey(list.get(index)) ? list.get(index) : ItemStack.EMPTY : ItemStack.EMPTY);
        }
        scrolledList.max = Math.max(0, (list.size() - 1) / layerSelectionGrid.getContainerSize());
    }

    @Override
    protected void init() {
        super.init();
        Component title = Component.literal(groupId);
        addRenderableOnly(SimpleLayoutRenderable.createDrawString(title, panel.x + (panel.width - font.width(title)) / 2, panel.y + 7, font.width(title), 9, CommonColor.INVENTORY_GRAY_TEXT.get(), false));
        tooltipBox.init();
        scroller.setPosition(panel.x + 299, panel.y + 50);
        searchBox.setPosition(panel.x + (panel.width - searchBox.getWidth()) / 2 - 6, panel.y + 20);
        addRenderableWidget(searchBox);
        addRenderableWidget(new TickBox(panel.x + 57, panel.y + 200, LegacyListingsClient.onlyModded, b->Component.literal("Only Modded"), m->null, b->{
            LegacyListingsClient.onlyModded = b.selected;
            fillLayerGrid();
        }));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> {
            if (selectedItems.isEmpty()) {
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
        }).bounds(panel.x + 57, panel.y + 216, 200, 20).build());
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderDefaultBackground(guiGraphics, i, j, f);
        tooltipBox.render(guiGraphics, i, j, f);

        int maxSlotsWidth = tooltipBox.width / 21;
        List<ItemStack> fullItems = new ArrayList<>();
        fullItems.addAll(defaultItems);
        fullItems.addAll(selectedItems);
        for (int idx = 0; idx < fullItems.size(); idx++) {
            ItemStack item = fullItems.get(idx);
            LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.itemHolder(item, false);
            holder.setPos(tooltipBox.x + 10 + idx % maxSlotsWidth * holder.width, tooltipBox.y + 10 + idx / maxSlotsWidth * holder.height);
            FactoryGuiGraphics.of(guiGraphics).pushBufferSource(FactoryGuiGraphics.of(guiGraphics).getBufferSource());
            guiGraphics.flush();
            RenderSystem.setShaderColor(0.8f,0.8f,0.8f,1f);
            holder.render(guiGraphics, i, j, f);
            guiGraphics.flush();
            RenderSystem.setShaderColor(1f,1f,1f,1f);
            FactoryGuiGraphics.of(guiGraphics).popBufferSource();
        }
        int xDiff = (tooltipBox.width - 69) / 2;
        for (int index = 0; index < ingredientsGrid.size(); index++) {
            LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.itemHolder(tooltipBox.x + xDiff + index % 3 * 23, tooltipBox.y + 32 + Math.max((fullItems.size() - 1) / maxSlotsWidth, 1) * 21 + index / 3 * 23, 23, 23, getActualItem(ingredientsGrid.get(index)), false, new Vec3(0.5, 0.5, 0));
            holder.render(guiGraphics, i, j, f);
        }
    }

    @Override
    protected void slotClicked(Slot slot) {
        ItemStack item = slot.getItem();
        if (defaultItems.contains(item) || item.isEmpty()) {
            ScreenUtil.playSimpleUISound(LegacyRegistries.CRAFT_FAIL.get(), 1f);
            return;
        }
        if (selectedItems.contains(item)) selectedItems.remove(item);
        else selectedItems.add(item);
        ScreenUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(),1f);
    }

    @Override
    public void setHoveredSlot(Slot slot) {
        super.setHoveredSlot(slot);
        ingredientsGrid.clear();
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
        menu.slots.forEach(s -> {
            LegacyIconHolder holder = ScreenUtil.iconHolderRenderer.slotBounds(panel.x, panel.y, s);
            FactoryGuiGraphics.of(guiGraphics).disableDepthTest();
            guiGraphics.pose().translate(0, 0, -100);
            if (selectedItems.contains(s.getItem())) {
                FactoryGuiGraphics.of(guiGraphics).blitSprite(TickBox.TICK, holder.x, holder.y, 8, 8);
            } else if (defaultItems.contains(s.getItem())) {
                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacyIconHolder.WARNING_ICON, holder.x, holder.y, 8, 8);
            }
            FactoryGuiGraphics.of(guiGraphics).enableDepthTest();
        });
    }
}
