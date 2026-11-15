package com.findaries.horror;

import com.findaries.horror.block.DreamPortalBlock;
import com.findaries.horror.entity.BlueStrangerEntity;
import com.findaries.horror.entity.FlowerManEntity;
import com.findaries.horror.entity.CaveDemonEntity;
import com.findaries.horror.entity.EyeObjectEntity;
import com.findaries.horror.entity.FakePlayerEntity;
import com.findaries.horror.entity.NevelimEntity;
import com.findaries.horror.entity.OneEyeGiantEntity;
import com.findaries.horror.entity.PillarEntity;
import com.findaries.horror.entity.RedDemonEntity;
import com.findaries.horror.entity.SheEntity;
import com.findaries.horror.entity.SkyPersonEntity;
import com.findaries.horror.entity.WandererEntity;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SoundType;
import com.findaries.horror.entity.Danger12Entity;
import com.findaries.horror.entity.LongLegsEntity;
import com.findaries.horror.entity.WatcherEntity;
import com.findaries.horror.event.Journey;
import com.findaries.horror.world.DreamChunkGenerator;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeSpawnEggItem;
import com.findaries.horror.world.Structures;
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
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, FinDaries.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FinDaries.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FinDaries.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FinDaries.MODID);
    /** Everything shown in the creative tab, in order. */
    private static final List<RegistryObject<Item>> TAB_ITEMS = new ArrayList<>();
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FinDaries.MODID);
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS = DeferredRegister.create(Registries.CHUNK_GENERATOR, FinDaries.MODID);

    // ---- blocks -------------------------------------------------------------------------------
    /** TV-static portal: overworld -> wonderland.avi (and back from the arrival point). */
    public static final RegistryObject<Block> WONDERLAND_PORTAL = portal("wonderland_portal", Journey.Dest.WONDERLAND, MapColor.COLOR_LIGHT_GRAY, 11);
    /** Dark portal inside the wooden cabin: wonderland -> the long wooden corridor. */
    public static final RegistryObject<Block> HALL_PORTAL = portal("hall_portal", Journey.Dest.HALLWAY, MapColor.COLOR_BROWN, 5);
    /** Portal made of coloured dots: wonderflowers -> cage_wonderland. */
    public static final RegistryObject<Block> DOTS_PORTAL = portal("dots_portal", Journey.Dest.CAGE, MapColor.COLOR_MAGENTA, 10);
    /** White portal in the wall of the cage: back to the overworld, grants "alive". */
    public static final RegistryObject<Block> WHITE_PORTAL = portal("white_portal", Journey.Dest.ALIVE, MapColor.SNOW, 15);

    /** Portal to the Hellway (found in hell portals and in the "wrong" houses instead of a bed). */
    public static final RegistryObject<Block> HELLWAY_PORTAL = portal("hellway_portal", Journey.Dest.HELLWAY, MapColor.COLOR_RED, 9);
    /** The door-textured portal at the bottom of a three block deep hole: overworld -> stonehell. */
    public static final RegistryObject<Block> DOOR_PORTAL = portal("door_portal", Journey.Dest.STONEHELL, MapColor.COLOR_BROWN, 8);
    /** Portal "in the shape of freedom" in the broken window of the_wonderdream. */
    public static final RegistryObject<Block> FREEDOM_PORTAL = portal("freedom_portal", Journey.Dest.ALIVE, MapColor.SNOW, 15);
    /** Wooden room portal: stonehell / the wrong house -> the_wonderdream. */
    public static final RegistryObject<Block> WONDERDREAM_PORTAL = portal("wonderdream_portal", Journey.Dest.WONDERDREAM, MapColor.COLOR_BROWN, 10);

    public static final RegistryObject<Block> RED_COBBLESTONE = BLOCKS.register("red_cobblestone",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0F, 6.0F).sound(SoundType.STONE)));
    /** Blood-red cobblestone the Hellway is made of. */
    public static final RegistryObject<Block> BLOOD_COBBLESTONE = BLOCKS.register("blood_cobblestone",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0F, 6.0F).sound(SoundType.STONE)));
    /** Two block tall red flower with an eye, left behind by a Flower Man. */
    public static final RegistryObject<Block> EYE_FLOWER = BLOCKS.register("eye_flower",
            () -> new DoublePlantBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollission().instabreak()
                    .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.NONE).pushReaction(PushReaction.DESTROY)));

    static {
        TAB_ITEMS.add(ITEMS.register("red_cobblestone", () -> new BlockItem(RED_COBBLESTONE.get(), new Item.Properties())));
        TAB_ITEMS.add(ITEMS.register("blood_cobblestone", () -> new BlockItem(BLOOD_COBBLESTONE.get(), new Item.Properties())));
        TAB_ITEMS.add(ITEMS.register("eye_flower", () -> new DoubleHighBlockItem(EYE_FLOWER.get(), new Item.Properties())));
    }

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

    public static final RegistryObject<EntityType<WandererEntity>> WANDERER = ENTITIES.register("wanderer",
            () -> EntityType.Builder.<WandererEntity>of(WandererEntity::new, MobCategory.MISC)
                    .sized(0.9F, 5.2F).clientTrackingRange(12).updateInterval(2).fireImmune().build("wanderer"));
    public static final RegistryObject<EntityType<SheEntity>> SHE = ENTITIES.register("she",
            () -> EntityType.Builder.<SheEntity>of(SheEntity::new, MobCategory.MISC)
                    .sized(0.5F, 3.9F).clientTrackingRange(16).updateInterval(1).fireImmune().build("she"));
    public static final RegistryObject<EntityType<FlowerManEntity>> FLOWER_MAN = ENTITIES.register("flower_man",
            () -> EntityType.Builder.<FlowerManEntity>of(FlowerManEntity::new, MobCategory.MISC)
                    .sized(0.8F, 3.1F).clientTrackingRange(12).updateInterval(2).fireImmune().build("flower_man"));
    public static final RegistryObject<EntityType<RedDemonEntity>> RED_DEMON = ENTITIES.register("red_demon",
            () -> EntityType.Builder.<RedDemonEntity>of(RedDemonEntity::new, MobCategory.MISC)
                    .sized(0.5F, 4.0F).clientTrackingRange(16).updateInterval(1).fireImmune().build("red_demon"));
    public static final RegistryObject<EntityType<SkyPersonEntity>> SKY_PERSON = ENTITIES.register("sky_person",
            () -> EntityType.Builder.<SkyPersonEntity>of(SkyPersonEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).clientTrackingRange(10).build("sky_person"));
    public static final RegistryObject<EntityType<PillarEntity>> PILLAR = ENTITIES.register("pillar",
            () -> EntityType.Builder.<PillarEntity>of(PillarEntity::new, MobCategory.MISC)
                    .sized(0.9F, 3.0F).clientTrackingRange(12).updateInterval(2).fireImmune().build("pillar"));
    public static final RegistryObject<EntityType<FakePlayerEntity>> FAKE_PLAYER = ENTITIES.register("fake_player",
            () -> EntityType.Builder.<FakePlayerEntity>of(FakePlayerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).clientTrackingRange(16).updateInterval(1).fireImmune().build("fake_player"));
    public static final RegistryObject<EntityType<CaveDemonEntity>> CAVE_DEMON = ENTITIES.register("cave_demon",
            () -> EntityType.Builder.<CaveDemonEntity>of(CaveDemonEntity::new, MobCategory.MISC)
                    .sized(0.45F, 4.6F).clientTrackingRange(16).updateInterval(1).fireImmune().build("cave_demon"));
    public static final RegistryObject<EntityType<OneEyeGiantEntity>> ONE_EYE_GIANT = ENTITIES.register("one_eye_giant",
            () -> EntityType.Builder.<OneEyeGiantEntity>of(OneEyeGiantEntity::new, MobCategory.MISC)
                    .sized(2.2F, 9.0F).clientTrackingRange(24).updateInterval(1).fireImmune().build("one_eye_giant"));
    public static final RegistryObject<EntityType<NevelimEntity>> NEVELIM = ENTITIES.register("nevelim",
            () -> EntityType.Builder.<NevelimEntity>of(NevelimEntity::new, MobCategory.MISC)
                    .sized(0.5F, 5.6F).clientTrackingRange(16).updateInterval(1).fireImmune().build("nevelim"));
    public static final RegistryObject<EntityType<EyeObjectEntity>> EYE = ENTITIES.register("eye",
            () -> EntityType.Builder.<EyeObjectEntity>of(EyeObjectEntity::new, MobCategory.MISC)
                    .sized(1.6F, 1.6F).clientTrackingRange(16).updateInterval(1).fireImmune().build("eye"));

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

    public static final RegistryObject<Item> WANDERER_EGG = egg("wanderer", WANDERER, 0x101018, 0x2A3550);
    public static final RegistryObject<Item> SHE_EGG = egg("she", SHE, 0x000000, 0x3A3A3A);
    public static final RegistryObject<Item> FLOWER_MAN_EGG = egg("flower_man", FLOWER_MAN, 0x7A5536, 0x55B030);
    public static final RegistryObject<Item> RED_DEMON_EGG = egg("red_demon", RED_DEMON, 0x5A0808, 0xFF3020);
    public static final RegistryObject<Item> SKY_PERSON_EGG = egg("sky_person", SKY_PERSON, 0x8A8A8A, 0xD03030);
    public static final RegistryObject<Item> PILLAR_EGG = egg("pillar", PILLAR, 0x0B0B10, 0xE8D24A);
    public static final RegistryObject<Item> FAKE_PLAYER_EGG = egg("fake_player", FAKE_PLAYER, 0x3C7A4A, 0xC8A27A);
    public static final RegistryObject<Item> CAVE_DEMON_EGG = egg("cave_demon", CAVE_DEMON, 0x4A0606, 0xFF3B18);
    public static final RegistryObject<Item> ONE_EYE_GIANT_EGG = egg("one_eye_giant", ONE_EYE_GIANT, 0x20212A, 0xF0D020);
    public static final RegistryObject<Item> NEVELIM_EGG = egg("nevelim", NEVELIM, 0xD9DEE6, 0x30343C);
    public static final RegistryObject<Item> EYE_EGG = egg("eye", EYE, 0xF2F2EE, 0x201018);

    public static final RegistryObject<Item> EVENT_TRIANGLE = eventItem("triangle");
    public static final RegistryObject<Item> EVENT_SLAB = eventItem("slab");
    public static final RegistryObject<Item> EVENT_UFO = eventItem("ufo");
    public static final RegistryObject<Item> EVENT_MUSHROOMS = eventItem("mushrooms");
    public static final RegistryObject<Item> EVENT_TOWER = eventItem("tower");
    public static final RegistryObject<Item> EVENT_LONGLEGS = eventItem("longlegs");
    public static final RegistryObject<Item> EVENT_PORTAL = eventItem("portal");
    public static final RegistryObject<Item> EVENT_TRIANGLE_WANDERER = eventItem("triangle_wanderer");
    public static final RegistryObject<Item> EVENT_TRIANGLE_LYING = eventItem("triangle_lying");
    public static final RegistryObject<Item> EVENT_TRIANGLE_STAR = eventItem("triangle_star");
    public static final RegistryObject<Item> EVENT_TRIANGLE_EYES = eventItem("triangle_eyes");
    public static final RegistryObject<Item> EVENT_CASTLE = eventItem("castle");
    public static final RegistryObject<Item> EVENT_HELL_PORTAL = eventItem("hell_portal");
    public static final RegistryObject<Item> EVENT_WRONG_HOUSE = eventItem("wrong_house");
    public static final RegistryObject<Item> EVENT_SPACESHIP = eventItem("spaceship");
    public static final RegistryObject<Item> EVENT_FLOWER_MAN = eventItem("flower_man");
    public static final RegistryObject<Item> EVENT_PILLAR = eventItem("pillar");
    public static final RegistryObject<Item> EVENT_HOLE = eventItem("hole");
    public static final RegistryObject<Item> EVENT_CAVE_DEMON = eventItem("cave_demon");
    public static final RegistryObject<Item> EVENT_NEVELIM = eventItem("nevelim");
    public static final RegistryObject<Item> EVENT_EYE = eventItem("eye");
    public static final RegistryObject<Item> EVENT_GIANT_NEVELIM = eventItem("giant_nevelim");
    public static final RegistryObject<Item> EVENT_FAKE_PLAYER = eventItem("fake_player");
    public static final RegistryObject<Item> EVENT_ONE_EYE_GIANT = eventItem("one_eye_giant");

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.findaries"))
            .icon(() -> new ItemStack(LONGLEGS_EGG.get()))
            .displayItems((parameters, output) -> {
                for (RegistryObject<Item> item : TAB_ITEMS) {
                    output.accept(item.get());
                }
                output.accept(Structures.noteBook());
                output.accept(Structures.book("note", "???", SheEntity.NOTE));
            })
            .build());

    // ---- sounds -------------------------------------------------------------------------------
    public static final RegistryObject<SoundEvent> CHIRP = sound("chirp");
    public static final RegistryObject<SoundEvent> HUM = sound("hum");
    public static final RegistryObject<SoundEvent> STEPS = sound("steps");
    public static final RegistryObject<SoundEvent> SCREAM = sound("scream");

    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(FinDaries.rl(name)));
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
        event.put(WANDERER.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0D).build());
        event.put(SHE.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0D).build());
        event.put(FLOWER_MAN.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0D).build());
        event.put(RED_DEMON.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0D).build());
        event.put(SKY_PERSON.get(), SkyPersonEntity.attributes().build());
        event.put(PILLAR.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0D).build());
        event.put(FAKE_PLAYER.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0D).build());
        event.put(CAVE_DEMON.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0D).build());
        event.put(ONE_EYE_GIANT.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 500.0D).build());
        event.put(NEVELIM.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0D).build());
        event.put(EYE.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0D).build());
    }

    private ModRegistry() {
    }
}
