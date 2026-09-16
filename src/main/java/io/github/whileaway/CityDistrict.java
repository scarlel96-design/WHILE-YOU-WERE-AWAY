package io.github.whileaway;

import io.github.whileaway.core.CityLayout;
import io.github.whileaway.core.CityArtLayout;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** One shared district per world; bounded batches, persisted cursor, no rebuild on entry. */
public final class CityDistrict extends SavedData {
    public static final ResourceKey<Level> KEY=ResourceKey.create(Registries.DIMENSION,WhileAway.id("quiet_city"));
    public static final BlockPos ARRIVAL=new BlockPos(40,65,68), RETURN_LIGHT=new BlockPos(40,65,72);
    private int cursor;
    private int layoutVersion=2;
    private boolean started;
    public int layoutVersion(){return layoutVersion;}
    public java.util.List<CityLayout.Cell> plan(){return layoutVersion==1?CityLayout.CELLS:CityArtLayout.CELLS;}
    public boolean ready(){return cursor==plan().size();}
    public int cursor(){return cursor;}
    public void start(){if(!started){started=true;setDirty();}}
    public static CityDistrict get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(CityDistrict::new,CityDistrict::load,null),"whileaway_district_v1");
    }
    public static CityDistrict load(CompoundTag t,HolderLookup.Provider registry) {
        int version=t.getInt("version");
        if(version!=1&&version!=2)throw new IllegalStateException("District layout version mismatch");
        var d=new CityDistrict();d.layoutVersion=version;d.cursor=t.getInt("cursor");d.started=t.getBoolean("started");
        if(d.cursor<0||d.cursor>d.plan().size())throw new IllegalStateException("District cursor invalid");
        return d;
    }
    @Override public CompoundTag save(CompoundTag t,HolderLookup.Provider registry) {
        t.putInt("version",layoutVersion);t.putInt("cursor",cursor);t.putBoolean("started",started);return t;
    }
    public int advance(ServerLevel level,int budget) {
        if(!level.dimension().equals(KEY))throw new IllegalArgumentException("District placement requires quiet_city");
        int count=0;
        while(started&&!ready()&&count<Math.min(512,Math.max(0,budget))) {
            var c=plan().get(cursor);var pos=new BlockPos(c.x(),CityLayout.BASE_Y+c.y(),c.z());
            level.getChunkAt(pos);
            level.setBlock(pos,state(c.kind()),2);
            cursor++;count++;
        }
        if(count>0)setDirty();return count;
    }
    public static BlockState state(CityLayout.Kind kind) {
        return switch(kind) {
            case ROAD -> Blocks.GRAY_CONCRETE.defaultBlockState();
            case LINE -> Blocks.YELLOW_TERRACOTTA.defaultBlockState();
            case PAVING -> Blocks.POLISHED_ANDESITE.defaultBlockState();
            case WALL -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            case TRIM -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            case GLASS -> Blocks.GRAY_STAINED_GLASS.defaultBlockState();
            case FLOOR -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
            case LAMP -> Blocks.SEA_LANTERN.defaultBlockState();
            case POST -> Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState();
            case BENCH -> Blocks.DARK_OAK_SLAB.defaultBlockState();
            case GATE -> WhileAway.RETURN_LIGHT.get().defaultBlockState();
            case INTAKE -> WhileAway.CITY_INTAKE.get().defaultBlockState();
            case HOME -> WhileAway.CITY_HOME.get().defaultBlockState();
            case SIREN -> WhileAway.CITY_SIREN.get().defaultBlockState();
            case AIR -> Blocks.AIR.defaultBlockState();
            case BRICK -> Blocks.BRICKS.defaultBlockState();
            case PLASTER -> Blocks.CALCITE.defaultBlockState();
            case COPPER -> Blocks.OXIDIZED_CUT_COPPER.defaultBlockState();
            case DARK -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            case CRACKED -> Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
            case MOSS -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case GRATE -> Blocks.IRON_BARS.defaultBlockState();
            case WARM -> Blocks.SHROOMLIGHT.defaultBlockState();
            case RED -> Blocks.REDSTONE_BLOCK.defaultBlockState();
            case CHAIN -> Blocks.CHAIN.defaultBlockState();
            case CHAIR -> Blocks.DARK_OAK_STAIRS.defaultBlockState();
            case WATER -> Blocks.WATER.defaultBlockState();
            case EARTH -> Blocks.COARSE_DIRT.defaultBlockState();
            case WOOD -> Blocks.DARK_OAK_LOG.defaultBlockState();
            case LEAF -> Blocks.DARK_OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true);
            case RUBBLE -> Blocks.COBBLESTONE_SLAB.defaultBlockState();
            case TRAM -> Blocks.YELLOW_TERRACOTTA.defaultBlockState();
            case WHEEL -> Blocks.COAL_BLOCK.defaultBlockState();
            case CLOCK -> Blocks.SMOOTH_QUARTZ.defaultBlockState();
        };
    }
    @SubscribeEvent public void tick(ServerTickEvent.Post event) {
        if(!StoryStorage.available(event.getServer()))return;
        var level=event.getServer().getLevel(KEY);
        if(level!=null){var data=get(level);if(data.started&&!data.ready())data.advance(level,256);}
    }
}
