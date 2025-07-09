package pizzart.listings;

import com.google.common.base.Charsets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.util.GsonHelper;
import org.apache.commons.io.FileUtils;
import wily.factoryapi.util.DynamicUtil;
import wily.factoryapi.util.ListMap;
import wily.legacy.client.LegacyCraftingTabListing;
import wily.legacy.client.LegacyCreativeTabListing;
import wily.legacy.client.RecipeInfo;
import wily.legacy.client.screen.LegacyTabButton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
//? if >1.20.1
/*import java.util.Optional;*/

public class ListingPack {
    public static final Codec<String> GROUP_CODEC = Codec.STRING.fieldOf("group").codec();
    public static final Codec<List<RecipeInfo.Filter>> RECIPES_CODEC = RecipeInfo.Filter.CODEC.xmap(f->f instanceof RecipeInfo.Filter.ItemId i ? new RecipeInfo.Filter.Id(i.id()) : f, f->f instanceof RecipeInfo.Filter.ItemId i ? new RecipeInfo.Filter.Id(i.id()) : f).listOf().fieldOf("recipes").codec();
    public static final Codec<List<Pair<String, List<RecipeInfo.Filter>>>> LISTING_CODEC = Codec.pair(GROUP_CODEC, RECIPES_CODEC).listOf();
    public static final Codec<LegacyCraftingTabListing> CRAFTING_LISTING_CODEC = RecordCodecBuilder.create(i -> i.group(
                    ResourceLocation.CODEC.fieldOf("id").forGetter(LegacyCraftingTabListing::id),
                    DynamicUtil.getComponentCodec().optionalFieldOf("name",null).forGetter(LegacyCraftingTabListing::name),
                    ResourceLocation.CODEC.fieldOf("icon").forGetter(l-> l.iconHolder().content() instanceof ResourceLocation r ? r : ResourceLocation.tryBuild("legacy", "icon/structures")),
                    LISTING_CODEC.fieldOf("listing").forGetter(l->l.craftings().entrySet().stream().map(e->Pair.of(e.getKey(), e.getValue())).toList()))
            .apply(i, (id, name, icon, list) -> new LegacyCraftingTabListing(id, name, new LegacyTabButton.IconHolder<>(icon, LegacyTabButton.DEFAULT_ICON_TYPE, icon), list.stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)))));

    public static final String LISTING_PACKS = "listing_packs";
    public static final Path LISTING_PACKS_PATH = Minecraft.getInstance().gameDirectory.toPath().resolve(LISTING_PACKS);

    public enum SaveStatus {
        OK,
        FILE_EXISTS,
        IO_ERROR,
        EMPTY,
    }

    public enum Type {
        CUSTOM,
        ADDITIVE,
        FULL,
    }

    public ListMap<ResourceLocation, LegacyCraftingTabListing> craftingTabs;
    public ListMap<ResourceLocation, LegacyCreativeTabListing> creativeTabs;
    public String name;
    public String desc;
    public int packFormat;
    public boolean initialized;
    public Type type;

    public ListingPack() {
        this(new ListMap<>(), new ListMap<>(), "listing_pack", "A listing pack", 15, false, Type.FULL);
    }

    public ListingPack(String name, boolean initialized) {
        this(new ListMap<>(), new ListMap<>(), name, "A listing pack", 15, initialized, Type.FULL);
    }

    public ListingPack(ListMap<ResourceLocation, LegacyCraftingTabListing> craftingTabs, ListMap<ResourceLocation, LegacyCreativeTabListing> creativeTabs, String name, String desc, int packFormat, boolean initialized, Type type) {
        this.craftingTabs = craftingTabs;
        this.creativeTabs = creativeTabs;
        this.name = name;
        this.desc = desc;
        this.packFormat = packFormat;
        this.initialized = initialized;
        this.type = type;
    }

    public boolean modifiesOldGroup() {
        for (Map.Entry<ResourceLocation, LegacyCraftingTabListing> entry : craftingTabs.entrySet()) {
            if (!LegacyCraftingTabListing.map.containsKey(entry.getKey())) continue;
            for (Map.Entry<String, List<RecipeInfo.Filter>> groupEntry : entry.getValue().craftings().entrySet()) {
                Map<String, List<RecipeInfo.Filter>> oldCraftings = LegacyCraftingTabListing.map.get(entry.getKey()).craftings();
                if (!oldCraftings.containsKey(groupEntry.getKey())) continue;
                for (RecipeInfo.Filter filter : groupEntry.getValue()) {
                    if (oldCraftings.get(groupEntry.getKey()).contains(filter)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean containsOldListing() {
        for (Map.Entry<ResourceLocation, LegacyCraftingTabListing> entry : craftingTabs.entrySet()) {
            if (LegacyCraftingTabListing.map.containsKey(entry.getKey())) return false;
        }
        return true;
    }

    public boolean isEmpty() {
        for (LegacyCraftingTabListing listing : craftingTabs.values()) {
            for (List<RecipeInfo.Filter> group : listing.craftings().values()) {
                if (!group.isEmpty()) return false;
            }
        }
        return true;
    }

    public SaveStatus save(boolean overwrite) {
        return save(LISTING_PACKS_PATH, overwrite);
    }

    public SaveStatus save(Path path, boolean overwrite){
        if (craftingTabs.isEmpty()) {
            LegacyListings.LOGGER.warn("Crafting map is empty!");
            return SaveStatus.EMPTY;
        }

        Path packDir = path.resolve(name);
        Path p = packDir.resolve("assets").resolve("legacy").resolve("crafting_tab_listing.json");
        Path metaPath = packDir.resolve("pack.mcmeta");
        if (!Files.exists(p.getParent())) {
            try {
                FileUtils.createParentDirectories(p.toFile());
            } catch (IOException e) {
                LegacyListings.LOGGER.warn("Failed to make parent directories for file {}", p, e);
            }
        }
        if (Files.exists(p) || Files.exists(metaPath)) {
            if (!overwrite) return SaveStatus.FILE_EXISTS;
            else {
                try {
                    if (Files.exists(p)) Files.delete(p);
                    if (Files.exists(metaPath)) Files.delete(metaPath);
                } catch (IOException e) {
                    LegacyListings.LOGGER.warn("Failed to delete file for overwrite",e);
                    return SaveStatus.IO_ERROR;
                }
            }
        }


        try (JsonWriter w = new JsonWriter(Files.newBufferedWriter(p, Charsets.UTF_8))){
            w.setIndent("  ");
            DataResult<JsonElement> dataResult = CRAFTING_LISTING_CODEC.listOf().encodeStart(JsonOps.INSTANCE, craftingTabs.values().stream().toList());
//            DataResult<JsonElement> dataResult = LegacyCraftingTabListing.CODEC.listOf().encodeStart(JsonOps.INSTANCE, craftingTabs.values().stream().toList());
            GsonHelper.writeValue(w, dataResult.result().orElseThrow(), null);
            try (JsonWriter metaW = new JsonWriter(Files.newBufferedWriter(metaPath, Charsets.UTF_8))) {
                metaW.setIndent("  ");
                //? if <1.21.1 {
                JsonObject metaResult = PackMetadataSection.TYPE.toJson(new PackMetadataSection(Component.literal(desc), packFormat));
                //?} else if <1.21.5 {
                /*JsonObject metaResult = PackMetadataSection.TYPE.toJson(new PackMetadataSection(Component.literal(desc), packFormat, Optional.empty()));
                *///?} else {
                /*JsonElement metaResult = PackMetadataSection.CODEC.encodeStart(JsonOps.INSTANCE, new PackMetadataSection(Component.literal(desc), packFormat, Optional.empty())).result().orElseThrow();
                *///?}
                JsonObject full = new JsonObject();
                full.add("pack", metaResult);
                GsonHelper.writeValue(metaW, full, null);
            } catch (Exception e) {
                LegacyListings.LOGGER.warn(e);
            }
        } catch (IOException | NoSuchElementException e) {
            LegacyListings.LOGGER.warn("Failed to write {}, this listing pack won't be saved",p,e);
            return SaveStatus.IO_ERROR;
        }
        return SaveStatus.OK;
    }
}
