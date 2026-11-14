package com.findiaries.horror;

import com.findiaries.horror.block.DreamPortalBlock;
import com.findiaries.horror.entity.BlueStrangerEntity;
import com.findiaries.horror.entity.Danger12Entity;
import com.findiaries.horror.entity.LongLegsEntity;
import com.findiaries.horror.entity.WatcherEntity;
import com.findiaries.horror.event.Journey;
import com.findiaries.horror.world.DreamChunkGenerator;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeSpawnEggItem;
import com.findiaries.horror.world.Structures;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, FinDiaries.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FinDiaries.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FinDiaries.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FinDiaries.MODID);
    /** Everything shown in the creative tab, in order. */
    private static final List<RegistryObject<Item>> TAB_ITEMS = new ArrayList<>();
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FinDiaries.MODID);
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS = DeferredRegister.create(Registries.CHUNK_GENERATOR, FinDiaries.MODID);

    // ---- blocks -------------------------------------------------------------------------------
    /** TV-static portal: overworld -> wonderland.avi (and back from the arrival point). */
    public static final RegistryObject<Block> WONDERLAND_PORTAL = portal("wonderland_portal", Journey.Dest.WONDERLAND, MapColor.COLOR_LIGHT_GRAY, 11);
    /** Dark portal inside the wooden cabin: wonderland -> the long wooden corridor. */
    public static final RegistryObject<Block> HALL_PORTAL = portal("hall_portal", Journey.Dest.HALLWAY, MapColor.COLOR_BROWN, 5);
    /** Portal made of coloured dots: wonderflowers -> cage_wonderland. */
    public static final RegistryObject<Block> DOTS_PORTAL = portal("dots_portal", Journey.Dest.CAGE, MapColor.COLOR_MAGENTA, 10);
    /** White portal in the wall of the cage: back to the overworld, grants "alive". */
    public static final RegistryObject<Block> WHITE_PORTAL = portal("white_portal", Journey.Dest.ALIVE, MapColor.SNOW, 15);

    private static RegistryObject<Block> portal(String name, Journey.Dest dest, MapColor color, int light) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> new DreamPortalBlock(dest, BlockBehaviour.Properties.of()
                .mapColor(color)
                .noCollission()
                .strength(-1.0F, 3600000.0F)
                .noLootTable()
                .noOcclusion()
                .lightLevel(s -> light)
                .pushReaction(PushReaction.BLOCK)));
        TAB_ITEMS.add(ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties())));
        return block;
    }

    // ---- entities -----------------------------------------------------------------------------
    public static final RegistryObject<EntityType<LongLegsEntity>> LONGLEGS = ENTITIES.register("kill000145236",
            () -> EntityType.Builder.<LongLegsEntity>of(LongLegsEntity::new, MobCategory.MISC)
                    .sized(0.6F, 5.0F).clientTrackingRange(12).updateInterval(2).fireImmune().build("kill000145236"));
    public static final RegistryObject<EntityType<WatcherEntity>> WATCHER = ENTITIES.register("watcher",
            () -> EntityType.Builder.<WatcherEntity>of(WatcherEntity::new, MobCategory.MISC)
                    .sized(0.5F, 3.4F).clientTrackingRange(12).updateInterval(2).fireImmune().build("watcher"));
    public static final RegistryObject<EntityType<Danger12Entity>> DANGER_12 = ENTITIES.register("danger_12",
            () -> EntityType.Builder.<Danger12Entity>of(Danger12Entity::new, MobCategory.MISC)
                    .sized(0.5F, 3.75F).clientTrackingRange(16).updateInterval(1).fireImmune().build("danger_12"));
    public static final RegistryObject<EntityType<BlueStrangerEntity>> BLUE_STRANGER = ENTITIES.register("blue_stranger",
            () -> EntityType.Builder.<BlueStrangerEntity>of(BlueStrangerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 3.5F).clientTrackingRange(12).updateInterval(2).fireImmune().build("blue_stranger"));

    // ---- items: spawn eggs and event triggers --------------------------------------------------
    private static RegistryObject<Item> egg(String name, Supplier<? extends EntityType<? extends Mob>> type, int base, int spots) {
        RegistryObject<Item> item = ITEMS.register(name + "_spawn_egg", () -> new ForgeSpawnEggItem(type, base, spots, new Item.Properties()));
        TAB_ITEMS.add(item);
        return item;
    }

    private static RegistryObject<Item> eventItem(String event) {
        RegistryObject<Item> item = ITEMS.register("event_" + event, () -> new EventItem(event, new Item.Properties().stacksTo(1)));
        TAB_ITEMS.add(item);
        return item;
    }

    public static final RegistryObject<Item> LONGLEGS_EGG = egg("kill000145236", LONGLEGS, 0x00A8A8, 0x4A3ABF);
    public static final RegistryObject<Item> WATCHER_EGG = egg("watcher", WATCHER, 0x0B0D18, 0xF2E23A);
    public static final RegistryObject<Item> DANGER_12_EGG = egg("danger_12", DANGER_12, 0x050508, 0x2850FF);
    public static final RegistryObject<Item> BLUE_STRANGER_EGG = egg("blue_stranger", BLUE_STRANGER, 0x0E1C5C, 0x3F76E4);

    public static final RegistryObject<Item> EVENT_TRIANGLE = eventItem("triangle");
    public static final RegistryObject<Item> EVENT_SLAB = eventItem("slab");
    public static final RegistryObject<Item> EVENT_UFO = eventItem("ufo");
    public static final RegistryObject<Item> EVENT_MUSHROOMS = eventItem("mushrooms");
    public static final RegistryObject<Item> EVENT_TOWER = eventItem("tower");
    public static final RegistryObject<Item> EVENT_LONGLEGS = eventItem("longlegs");
    public static final RegistryObject<Item> EVENT_PORTAL = eventItem("portal");

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.findiaries"))
            .icon(() -> new ItemStack(LONGLEGS_EGG.get()))
            .displayItems((parameters, output) -> {
                for (RegistryObject<Item> item : TAB_ITEMS) {
                    output.accept(item.get());
                }
                output.accept(Structures.noteBook());
            })
            .build());

    // ---- sounds -------------------------------------------------------------------------------
    public static final RegistryObject<SoundEvent> CHIRP = sound("chirp");
    public static final RegistryObject<SoundEvent> HUM = sound("hum");
    public static final RegistryObject<SoundEvent> STEPS = sound("steps");

    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(FinDiaries.rl(name)));
    }

    // ---- world --------------------------------------------------------------------------------
    public static final RegistryObject<Codec<? extends ChunkGenerator>> DREAM_GENERATOR =
            GENERATORS.register("dream", () -> DreamChunkGenerator.CODEC);

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
        ENTITIES.register(bus);
        SOUNDS.register(bus);
        GENERATORS.register(bus);
    }

    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(LONGLEGS.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0D).build());
        event.put(WATCHER.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0D).build());
        event.put(DANGER_12.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0D).build());
        event.put(BLUE_STRANGER.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0D).build());
    }

    private ModRegistry() {
    }
}
