package io.github.whileaway;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(WhileAway.ID)
@PrefixGameTestTemplate(false)
public final class ReturnNetworkSchema5GameTests {
    private static ReturnNetworkState discovered() {
        return ReturnNetworkState.notStarted().discover(
            new ReturnNetworkState.Prerequisite(4,true,true,true),
            UUID.fromString("12345678-1234-1234-1234-123456789012"),
            new ReturnNetworkState.Location("rail_relay","minecraft:overworld",new BlockPos(12,64,-8).asLong()));
    }
    private static CompoundTag legacy4(GameTestHelper h, UUID id) {
        NarrativeData d=new NarrativeData();
        d.discover(id,io.github.whileaway.core.Clue.STATION);
        d.entry(id).cityVisited=true; d.entry(id).cityClues=5;
        CompoundTag root=d.save(new CompoundTag(),h.getLevel().registryAccess());
        root.putInt("schema",4); root.remove("returnNetwork");
        return root;
    }
    @GameTest(template="empty")
    public static void schema4LoadsWithReturnNetworkNotStarted(GameTestHelper h) {
        UUID id=UUID.fromString("10000000-0000-0000-0000-000000000001");
        CompoundTag root=legacy4(h,id);
        h.assertTrue(!root.contains("returnNetwork"),"real schema4 shape has no returnNetwork");
        NarrativeData d=NarrativeData.load(root,h.getLevel().registryAccess());
        h.assertTrue(d.returnNetwork().stage==ReturnNetworkState.Stage.NOT_STARTED,"legacy migration stays NOT_STARTED");
        h.assertTrue(d.entry(id).progress.has(io.github.whileaway.core.Clue.STATION)&&d.entry(id).cityVisited&&d.entry(id).cityClues==5,"legacy player facts preserved");
        h.assertTrue(d.returnNetwork().facts.isEmpty(),"migration invents no Return Network facts");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void schema5AbsentReturnNetworkRoundTripsAsNotStarted(GameTestHelper h) {
        CompoundTag root=new NarrativeData().save(new CompoundTag(),h.getLevel().registryAccess());
        h.assertTrue(root.getInt("schema")==5,"writer emits schema5");
        h.assertTrue(!root.contains("returnNetwork"),"NOT_STARTED remains absent on disk");
        NarrativeData d=NarrativeData.load(root,h.getLevel().registryAccess());
        h.assertTrue(d.returnNetwork().stage==ReturnNetworkState.Stage.NOT_STARTED,"absent record reads as NOT_STARTED");
        CompoundTag round=d.save(new CompoundTag(),h.getLevel().registryAccess());
        h.assertTrue(!round.contains("returnNetwork"),"absent record remains absent after round-trip");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void schema5RoundTripsReturnNetwork(GameTestHelper h) {
        CompoundTag root=new NarrativeData().save(new CompoundTag(),h.getLevel().registryAccess());
        root.put("returnNetwork",discovered().save());
        NarrativeData d=NarrativeData.load(root,h.getLevel().registryAccess());
        h.assertTrue(d.returnNetwork().save().equals(discovered().save()),"schema5 Return Network round-trip");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void schema5MaterializedNotStartedRejected(GameTestHelper h) {
        CompoundTag root=new NarrativeData().save(new CompoundTag(),h.getLevel().registryAccess());
        root.put("returnNetwork",ReturnNetworkState.notStarted().save());
        boolean rejected=false; try{NarrativeData.load(root,h.getLevel().registryAccess());}catch(RuntimeException e){rejected=true;}
        h.assertTrue(rejected,"materialized NOT_STARTED is rejected");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void schema5MalformedReturnNetworkFailsClosed(GameTestHelper h) {
        CompoundTag root=new NarrativeData().save(new CompoundTag(),h.getLevel().registryAccess());
        root.put("returnNetwork",StringTag.valueOf("bad"));
        boolean rejected=false; try{NarrativeData.load(root,h.getLevel().registryAccess());}catch(RuntimeException e){rejected=true;}
        h.assertTrue(rejected,"schema5 wrong returnNetwork type fails closed"); h.succeed();
    }
    @GameTest(template="empty")
    public static void schema5UnsupportedNestedReturnNetworkSchemaFailsClosed(GameTestHelper h) {
        CompoundTag root=new NarrativeData().save(new CompoundTag(),h.getLevel().registryAccess());
        CompoundTag nested=discovered().save(); nested.putInt("schema",2); root.put("returnNetwork",nested);
        boolean rejected=false; try{NarrativeData.load(root,h.getLevel().registryAccess());}catch(RuntimeException e){rejected=true;}
        h.assertTrue(rejected,"nested Return Network schema > 1 fails closed"); h.succeed();
    }
    @GameTest(template="empty")
    public static void notStartedMigrationDoesNotActivateEvent(GameTestHelper h) {
        NarrativeData d=NarrativeData.load(legacy4(h,UUID.fromString("50000000-0000-0000-0000-000000000005")),h.getLevel().registryAccess());
        CompoundTag saved=d.save(new CompoundTag(),h.getLevel().registryAccess());
        h.assertTrue(saved.getInt("schema")==5,"legacy save migrates to schema5");
        h.assertTrue(!saved.contains("returnNetwork"),"schema4->5 invents no durable Return Network record");
        NarrativeData r=NarrativeData.load(saved,h.getLevel().registryAccess());
        ReturnNetworkState s=r.returnNetwork();
        h.assertTrue(s.stage==ReturnNetworkState.Stage.NOT_STARTED&&s.instance==null&&s.location==null&&s.participant==null&&s.facts.isEmpty(),"schema4->5 creates no event identity/location/participant/facts");
        h.succeed();
    }
}
