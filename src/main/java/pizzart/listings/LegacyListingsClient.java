package pizzart.listings;

import com.google.common.base.Charsets;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.util.DynamicUtil;
import wily.factoryapi.util.ListMap;
import wily.legacy.client.LegacyCraftingTabListing;
import wily.legacy.client.LegacyCreativeTabListing;
import wily.legacy.client.RecipeInfo;
import wily.legacy.client.screen.LegacyTabButton;
import wily.legacy.client.screen.RenderableVList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LegacyListingsClient {
    public enum SaveStatus {
        OK,
        FILE_EXISTS,
        IO_ERROR,
        EMPTY,
    }
    private boolean wasPaused = false;

    public static final Codec<String> GROUP_CODEC = Codec.STRING.fieldOf("group").codec();
    public static final Codec<List<RecipeInfo.Filter>> RECIPES_CODEC = RecipeInfo.Filter.CODEC.listOf().fieldOf("recipes").codec();
    public static final Codec<List<Pair<String, List<RecipeInfo.Filter>>>> LISTING_CODEC = Codec.pair(GROUP_CODEC, RECIPES_CODEC).listOf();
    public static final Codec<LegacyCraftingTabListing>  CRAFTING_LISTING_CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(LegacyCraftingTabListing::id),
            DynamicUtil.getComponentCodec().optionalFieldOf("name",null).forGetter(LegacyCraftingTabListing::name),
            LegacyTabButton.ICON_HOLDER_CODEC.optionalFieldOf("icon",null).forGetter(LegacyCraftingTabListing::iconHolder),
            LISTING_CODEC.fieldOf("listing").forGetter(l->l.craftings().entrySet().stream().map(e->Pair.of(e.getKey(), e.getValue())).toList()))
            .apply(i, (id, name, icon, list) -> new LegacyCraftingTabListing(id, name, icon, list.stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)))));

    public static final String LISTING_PACKS = "listing_packs";
    public static final Path LISTING_PACKS_PATH = Minecraft.getInstance().gameDirectory.toPath().resolve(LISTING_PACKS);
    public static final String MOD_ID = "listings";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static ListMap<ResourceLocation, LegacyCraftingTabListing> craftingTabs = new ListMap<>();
    public static ListMap<ResourceLocation, LegacyCreativeTabListing> creativeTabs = new ListMap<>();
    public static boolean onlyModded = false;
    public static boolean onlyNotAdded = false;

    public static void init() {
        FactoryAPIClient.postTick(this::postTick);
    }

    public void postTick(Minecraft minecraft) {
        if (!minecraft.isPaused()) wasPaused = false;
        if (minecraft.screen instanceof PauseScreen pauseScreen && minecraft.isPaused() && !wasPaused) {
            RenderableVList.Access listAccess = (RenderableVList.Access) pauseScreen;
            RenderableVList list = listAccess.getRenderableVList();
            list.addRenderables(list.renderables.size() - 1, Button.builder(Component.literal("Listings"), b -> minecraft.setScreen(new PacksScreen(pauseScreen))).build());
            listAccess.renderableVListInit();
            wasPaused = true;
        }
    }

    public static SaveStatus savePack(Path path, boolean overwrite, String name, Map<ResourceLocation, LegacyCraftingTabListing> craftingTabListingMap, Map<ResourceLocation, LegacyCreativeTabListing> creativeTabListingMap){
        if (craftingTabListingMap.isEmpty()) {
            LOGGER.warn("Crafting map is empty!");
            return SaveStatus.EMPTY;
        }

//        } else FileUtils.listFiles(path.toFile(), new String[]{"json"},true).forEach(File::delete);

        Path packDir = path.resolve(name);
        Path p = packDir.resolve("crafting_tab_listing.json");
        if (!Files.exists(packDir)) {
            try {
                FileUtils.createParentDirectories(p.toFile());
            } catch (IOException e) {
                LOGGER.warn("Failed to make parent directories for file {}", p, e);
            }
        }
//        if (Files.exists(p)) {
//            if (!overwrite) return SaveStatus.FILE_EXISTS;
//            else {
//                try {
//                    Files.delete(p);
//                } catch (IOException e) {
//                    LOGGER.warn("Failed to write {}, this listing pack won't be saved",p,e);
//                    return SaveStatus.IO_ERROR;
//                }
//            }
//        }


        try (JsonWriter w = new JsonWriter(Files.newBufferedWriter(p, Charsets.UTF_8))){
            w.setSerializeNulls(false);
            w.setIndent("  ");
//            GsonHelper.writeValue(w, LegacyCraftingTabListing.CODEC.listOf().encodeStart(JsonOps.INSTANCE, craftingTabListingMap.values().stream().toList()).result().orElseThrow(), null);
            DataResult<JsonElement> dataResult = CRAFTING_LISTING_CODEC.listOf().encodeStart(JsonOps.INSTANCE, craftingTabListingMap.values().stream().toList());
            GsonHelper.writeValue(w, dataResult.result().orElseThrow(), null);
        } catch (IOException e) {
            LOGGER.warn("Failed to write {}, this listing pack won't be saved",p,e);
            return SaveStatus.IO_ERROR;
        }
        return SaveStatus.OK;
    }
}