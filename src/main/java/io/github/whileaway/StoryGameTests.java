package io.github.whileaway;

import io.github.whileaway.core.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.neoforged.neoforge.gametest.*;

/** Headless integration tests; these use real NeoForge registries and Minecraft NBT. */
@GameTestHolder(WhileAway.ID)
@PrefixGameTestTemplate(false)
public final class StoryGameTests {
    @GameTest(template="empty")
    public static void stateRoundTrip(GameTestHelper h) {
        var d=new NarrativeData();var id=UUID.randomUUID();var e=d.entry(id);
        d.discover(id,Clue.STATION);d.discover(id,Clue.WARNING_READ);d.discover(id,Clue.SIGNAL_RESTORED);
        e.station=new BlockPos(-123,64,987);e.encounter=UUID.randomUUID();e.readingUntil=9876;
        e.progress=e.progress.eventPlayed(100,900);
        var loaded=NarrativeData.load(d.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        var actual=loaded.entry(id);
        h.assertTrue(actual.progress.equals(e.progress),"progress round-trip");
        h.assertTrue(actual.station.equals(e.station),"negative station coordinate survives");
        h.assertTrue(actual.encounter.equals(e.encounter),"entity ticket survives");
        h.assertTrue(actual.readingUntil==9876,"reading grace survives");h.succeed();
    }
    @GameTest(template="empty")
    public static void playersStayIsolated(GameTestHelper h) {
        var d=new NarrativeData();var a=UUID.randomUUID();var b=UUID.randomUUID();
        d.discover(a,Clue.STATION);d.discover(a,Clue.WARNING_READ);
        h.assertTrue(d.entry(a).progress.stage()==2,"first player progresses");
        h.assertTrue(d.entry(b).progress.stage()==0,"second player stays quiet");h.succeed();
    }
    @GameTest(template="empty")
    public static void futureSchemaRejected(GameTestHelper h) {
        var t=new CompoundTag();t.putInt("schema",NarrativeData.SCHEMA+1);boolean rejected=false;
        try {NarrativeData.load(t,h.getLevel().registryAccess());}catch(IllegalStateException expected){rejected=true;}
        h.assertTrue(rejected,"future data must not be overwritten");h.succeed();
    }
    @GameTest(template="empty")
    public static void stationTemplatePlaces(GameTestHelper h) {
        var optional=h.getLevel().getStructureManager().get(WhileAway.id("abandoned_station"));
        h.assertTrue(optional.isPresent(),"station NBT resolves");var template=optional.orElseThrow();
        h.assertTrue(template.getSize().equals(new BlockPos(19,10,19)),"station dimensions");
        var origin=h.absolutePos(new BlockPos(0,1,0));
        boolean placed=template.placeInWorld(h.getLevel(),origin,origin,new StructurePlaceSettings(),h.getLevel().getRandom(),Block.UPDATE_ALL);
        h.assertTrue(placed,"station placed");
        h.assertTrue(h.getLevel().getBlockState(origin.offset(6,1,9)).is(WhileAway.STATION.get()),"anchor placed");
        h.assertTrue(h.getLevel().getBlockEntity(origin.offset(6,1,9)) instanceof io.github.whileaway.content.StationEntity,"anchor ticker entity");
        h.assertTrue(h.getLevel().getBlockState(origin.offset(8,1,6)).is(WhileAway.RELAY.get()),"relay placed");h.succeed();
    }
    @GameTest(template="empty")
    public static void worldgenRegistriesResolve(GameTestHelper h) {
        var access=h.getLevel().registryAccess();
        h.assertTrue(access.registryOrThrow(Registries.STRUCTURE).containsKey(WhileAway.id("abandoned_station")),"structure registered");
        h.assertTrue(access.registryOrThrow(Registries.STRUCTURE_SET).containsKey(WhileAway.id("stations")),"placement registered");
        h.assertTrue(access.registryOrThrow(Registries.TEMPLATE_POOL).containsKey(WhileAway.id("station")),"pool registered");h.succeed();
    }
    @GameTest(template="empty")
    public static void worldgenBuildsPieces(GameTestHelper h) {
        var level=h.getLevel();var generator=level.getChunkSource().getGenerator();
        var structure=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(WhileAway.id("abandoned_station"));
        var context=new net.minecraft.world.level.levelgen.structure.Structure.GenerationContext(
            level.registryAccess(),generator,generator.getBiomeSource(),level.getChunkSource().randomState(),
            level.getStructureManager(),74192361L,new net.minecraft.world.level.ChunkPos(0,0),level,b->true);
        var point=((net.minecraft.world.level.levelgen.structure.structures.JigsawStructure)structure).findGenerationPoint(context);
        h.assertTrue(point.isPresent(),"station generation point");
        h.assertTrue(!point.orElseThrow().getPiecesBuilder().build().pieces().isEmpty(),"jigsaw must emit a piece; depth zero emits none in 1.21.1");h.succeed();
    }
    @GameTest(template="empty")
    public static void scenePacketRoundTrip(GameTestHelper h) {
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            var input=new ScenePayload(1,new BlockPos(-123,75,987));
            ScenePayload.CODEC.encode(buffer,input);
            h.assertTrue(ScenePayload.CODEC.decode(buffer).equals(input),"scene packet coordinates and kind survive");
        } finally {buffer.release();}
        h.succeed();
    }
    @GameTest(template="empty")
    public static void entitySerializes(GameTestHelper h) {
        var mob=WhileAway.WAYFARER.get().create(h.getLevel());var owner=UUID.randomUUID();
        h.assertTrue(mob!=null,"entity factory");mob.bind(owner,new BlockPos(1,5,7));
        var t=new CompoundTag();mob.saveWithoutId(t);
        var copy=WhileAway.WAYFARER.get().create(h.getLevel());copy.load(t);
        h.assertTrue(owner.equals(copy.witness()),"encounter ownership saved");
        h.assertTrue(copy.getMaxHealth()==28f,"attributes registered");h.succeed();
    }
    @GameTest(template="empty")
    public static void bookComponentsResolve(GameTestHelper h) {
        var book=WhileAway.WARNING_BOOK.get().getDefaultInstance();
        var pages=book.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
        h.assertTrue(pages!=null&&pages.pages().size()==2,"localized book pages");h.succeed();
    }
    @GameTest(template="empty")
    public static void schemaOneMigrates(GameTestHelper h) {
        var id=UUID.randomUUID();var d=new NarrativeData();d.discover(id,Clue.STATION);
        var t=d.save(new CompoundTag(),h.getLevel().registryAccess());t.putInt("schema",1);
        var player=t.getList("players",10).getCompound(0);
        for(var key:java.util.List.of("cityVisited","cityClues","returnPosition","returnYaw","returnPitch","transitAfter"))player.remove(key);
        var e=NarrativeData.load(t,h.getLevel().registryAccess()).entry(id);
        h.assertTrue(e.progress.has(Clue.STATION)&&!e.cityVisited&&e.cityClues==0&&e.returnPosition==null,"schema 1 progress retained, city defaults empty");h.succeed();
    }
    @GameTest(template="empty")
    public static void cityReturnPointRoundTrip(GameTestHelper h) {
        var d=new NarrativeData();var id=UUID.randomUUID();var e=d.entry(id);
        e.returnPosition=new BlockPos(-333,68,-275);e.returnYaw=123.5f;e.returnPitch=-17.5f;
        e.cityVisited=true;e.cityClues=5;e.transitAfter=321;
        var a=NarrativeData.load(d.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess()).entry(id);
        h.assertTrue(a.returnPosition.equals(e.returnPosition)&&a.returnYaw==123.5f&&a.returnPitch==-17.5f,"return position/orientation survive reload");
        h.assertTrue(a.cityVisited&&a.cityClues==5&&a.transitAfter==321,"city state persists");h.succeed();
    }
    @GameTest(template="empty")
    public static void cityPlacementRejectsOverworld(GameTestHelper h) {
        // Vanilla GameTestServer bakes an empty dimension registry into the FLAT preset.
        // Actual custom-dimension generation/resume is checked by CitySmoke instead.
        h.assertTrue(h.getLevel().registryAccess().registryOrThrow(Registries.DIMENSION_TYPE).containsKey(WhileAway.id("quiet_city")),"city dimension type decodes");
        h.assertTrue(h.getLevel().registryAccess().registryOrThrow(Registries.BIOME).containsKey(WhileAway.id("quiet_city")),"city biome decodes");
        var d=new CityDistrict();d.start();boolean rejected=false;
        try {d.advance(h.getLevel(),1);}catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected&&d.cursor()==0,"district never writes to overworld");h.succeed();
    }
    @GameTest(template="empty")
    public static void invalidCityCursorRejected(GameTestHelper h) {
        var t=new CompoundTag();t.putInt("version",1);t.putInt("cursor",-1);boolean fail=false;
        try{CityDistrict.load(t,h.getLevel().registryAccess());}catch(IllegalStateException expected){fail=true;}
        h.assertTrue(fail,"invalid cursor rejected rather than overwrite district");h.succeed();
    }
    @GameTest(template="empty")
    public static void relayLightAndCityBooks(GameTestHelper h) {
        var relay=WhileAway.RELAY.get().defaultBlockState();
        h.assertTrue(!relay.getValue(io.github.whileaway.content.RelayBlock.LIT),"relay starts unlit");
        var p=h.absolutePos(new BlockPos(3,2,3));h.getLevel().setBlock(p,relay.setValue(io.github.whileaway.content.RelayBlock.LIT,true),3);
        h.assertTrue(h.getLevel().getBlockState(p).getLightEmission(h.getLevel(),p)==12,"lit relay emits light");
        for(var item:java.util.List.of(WhileAway.CITY_INTAKE_BOOK.get(),WhileAway.CITY_HOME_BOOK.get(),WhileAway.CITY_SIREN_BOOK.get()))
            h.assertTrue(item.getDefaultInstance().get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT).pages().size()==2,"city book two pages");h.succeed();
    }
    @GameTest(template="empty")
    public static void blockedLandingRejected(GameTestHelper h) {
        var p=h.absolutePos(new BlockPos(10,4,10));
        for(int y=-4;y<=5;y++)h.getLevel().setBlock(p.offset(0,y,0),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
        h.assertTrue(CityTransit.findLanding(h.getLevel(),p,0)==null,"solid shaft never accepted");
        h.getLevel().setBlock(p,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        h.getLevel().setBlock(p.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        h.assertTrue(p.equals(CityTransit.findLanding(h.getLevel(),p,0)),"clear two-block landing accepted");h.succeed();
    }
    @GameTest(template="empty")
    public static void legacyCityPlanNeverReordered(GameTestHelper h) {
        var t=new CompoundTag();t.putInt("version",1);t.putBoolean("started",true);t.putInt("cursor",20988);
        var old=CityDistrict.load(t,h.getLevel().registryAccess());
        h.assertTrue(old.ready()&&old.layoutVersion()==1&&old.plan()==CityLayout.CELLS,"completed old plan remains untouched");
        t.putInt("cursor",100);old=CityDistrict.load(t,h.getLevel().registryAccess());
        h.assertTrue(old.plan().get(100).equals(CityLayout.CELLS.get(100)),"partial old cursor points at same block");
        h.assertTrue(new CityDistrict().layoutVersion()==2,"new worlds get art revision two");h.succeed();
    }
    @GameTest(template="empty")
    public static void cityHorrorFlagPersists(GameTestHelper h) {
        var d=new NarrativeData();var id=UUID.randomUUID();d.entry(id).cityShockTriggered=true;
        var copy=NarrativeData.load(d.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(copy.entry(id).cityShockTriggered&&!copy.entry(UUID.randomUUID()).cityShockTriggered,"scene flag persisted and player-isolated");h.succeed();
    }

    @GameTest(template="empty")
    public static void allSceneBoundariesRoundTripTwice(GameTestHelper h) {
        for(int repeat=0;repeat<2;repeat++)for(int kind=1;kind<=3;kind++)for(int cp=0;cp<=4;cp++) {
            var d=new NarrativeData();var id=UUID.randomUUID();var e=d.entry(id);
            var r=new SceneRecord(kind,new BlockPos(-18,65,11));e.scenes.put(kind,r);
            for(int n=1;n<=cp;n++)h.assertTrue(r.progress.advance(n),"ordered checkpoint commits");
            r.progress.shown=127;e.cityClues=7;e.returnPosition=new BlockPos(8,64,-9);
            d.discover(id,Clue.SIGNAL_RESTORED);
            if(kind==3&&cp==4)e.cityShockTriggered=true;
            var loaded=NarrativeData.load(d.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess()).entry(id);
            var a=loaded.scenes.get(kind);
            h.assertTrue(a.id.equals(r.id)&&a.progress.checkpoint==cp&&a.progress.shown==127,"checkpoint identity and committed cues preserved");
            h.assertTrue(a.progress.terminal()==(cp==4),"only final boundary completes");
            h.assertTrue(loaded.cityClues==7&&loaded.progress.has(Clue.SIGNAL_RESTORED)&&loaded.returnPosition.equals(e.returnPosition),"permanent facts never roll back");
            h.assertTrue(!a.progress.advance(cp)&&!a.progress.advance(cp+2),"duplicates and skipped steps rejected");
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void invalidSceneStateRecovers(GameTestHelper h) {
        var r=new SceneRecord(3,CityHorror.STAGE);var t=r.save();t.putString("state","BROKEN_ENUM");t.putInt("checkpoint",2);
        var a=SceneRecord.load(t);
        h.assertTrue(a.progress.state==EventCheckpoint.State.FAILED_RECOVERABLE&&a.progress.checkpoint==2,"unknown enum returns to safe checkpoint");
        t.putInt("checkpoint",999);a=SceneRecord.load(t);
        h.assertTrue(!a.progress.terminal()&&a.progress.checkpoint==0,"invalid checkpoint does not award completion");
        t.remove("checkpoint");t.remove("shown");a=SceneRecord.load(t);
        h.assertTrue(a.progress.checkpoint==0&&a.progress.shown==0,"missing optional fields default safely");h.succeed();
    }
    @GameTest(template="empty")
    public static void schemaTwoAndThreeMigration(GameTestHelper h) {
        for(int version:new int[]{2,3})for(boolean oldFlag:new boolean[]{false,true}) {
            var d=new NarrativeData();var id=UUID.randomUUID();var e=d.entry(id);e.station=new BlockPos(-7,70,4);e.cityClues=5;
            var t=d.save(new CompoundTag(),h.getLevel().registryAccess());t.putInt("schema",version);
            var player=t.getList("players",10).getCompound(0);player.remove("scenes");player.putString("unknown_future_field","ignored");
            if(version==2)player.remove("cityShockTriggered");else player.putBoolean("cityShockTriggered",oldFlag);
            var loaded=NarrativeData.load(t,h.getLevel().registryAccess());var a=loaded.entry(id);
            h.assertTrue(a.cityShockTriggered==(version==3&&oldFlag),"legacy flag preserved/defaulted");
            h.assertTrue(a.cityClues==5&&a.station.equals(e.station),"old story facts retained");
            h.assertTrue(loaded.save(new CompoundTag(),h.getLevel().registryAccess()).getInt("schema")==4,"explicit schema 4 save");
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void missingRequiredSceneDataRejected(GameTestHelper h) {
        var t=new SceneRecord(1,BlockPos.ZERO).save();t.remove("origin");boolean rejected=false;
        try{SceneRecord.load(t);}catch(IllegalStateException ex){rejected=ex.getMessage().contains("origin");}
        h.assertTrue(rejected,"diagnosable missing origin, no silent world overwrite");h.succeed();
    }
    @GameTest(template="empty")
    public static void sceneIntegrityDetectsContradiction(GameTestHelper h) {
        var e=new NarrativeData.Entry();var r=new SceneRecord(3,CityHorror.STAGE);e.scenes.put(3,r);e.cityShockTriggered=true;
        h.assertTrue(SceneRecovery.integrity(e).contains("seven_completion_fact_mismatch"),"contradiction diagnosed");
        e.cityShockTriggered=false;h.assertTrue(SceneRecovery.integrity(e).isEmpty(),"valid pending scene accepted");h.succeed();
    }
    @GameTest(template="empty")
    public static void ackLeasePacketRoundTrip(GameTestHelper h) {
        var b=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try {var a=new SceneAck(UUID.randomUUID(),UUID.randomUUID(),2,63,true);SceneAck.CODEC.encode(b,a);
            h.assertTrue(a.equals(SceneAck.CODEC.decode(b)),"lease and checkpoint payload preserved");}finally{b.release();}h.succeed();
    }
    @GameTest(template="empty")
    public static void offlineWayfarerDoesNotAge(GameTestHelper h) {
        var m=WhileAway.WAYFARER.get().create(h.getLevel());var id=UUID.randomUUID();m.bind(id,BlockPos.ZERO);
        NarrativeData.get(h.getLevel().getServer()).entry(id).encounter=m.getUUID();
        for(int i=0;i<80;i++)m.tick();var t=new CompoundTag();m.addAdditionalSaveData(t);
        h.assertTrue(t.getInt("StoryAge")==0,"offline time does not consume encounter lifetime");h.succeed();
    }
    @GameTest(template="empty")
    public static void duplicateWayfarerRejected(GameTestHelper h) {
        var m=WhileAway.WAYFARER.get().create(h.getLevel());var id=UUID.randomUUID();m.bind(id,BlockPos.ZERO);
        NarrativeData.get(h.getLevel().getServer()).entry(id).encounter=UUID.randomUUID();m.tick();
        h.assertTrue(m.isRemoved(),"noncanonical owned actor cleaned");h.succeed();
    }

    @GameTest(template="empty")
    public static void migrationIsDeterministic(GameTestHelper h) {
        var id=UUID.fromString("12345678-1234-1234-1234-123456789012");
        var d=new NarrativeData();var e=d.entry(id);e.station=new BlockPos(-7,70,4);
        e.progress=e.progress.discover(Clue.SIGNAL_RESTORED);e.cityVisited=true;e.cityShockTriggered=true;
        for(int version=1;version<=3;version++) {
            var input=d.save(new CompoundTag(),h.getLevel().registryAccess());input.putInt("schema",version);
            input.getList("players",10).getCompound(0).remove("scenes");
            CompoundTag first=null;
            for(int copy=0;copy<3;copy++) {
                var output=NarrativeData.load(input.copy(),h.getLevel().registryAccess()).save(new CompoundTag(),h.getLevel().registryAccess());
                if(first==null)first=output;else h.assertTrue(first.equals(output),"Migration Failure: identical schema "+version+" input produced different scene identities");
            }
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void missingIdentityRepairIsDeterministic(GameTestHelper h) {
        var t=new SceneRecord(1,new BlockPos(-10,64,30)).save();t.remove("instanceId");
        h.assertTrue(SceneRecord.load(t.copy()).id.equals(SceneRecord.load(t.copy()).id),"Migration Failure: missing ID repair uses randomness");h.succeed();
    }
    @GameTest(template="empty")
    public static void contradictoryCheckpointNeverCompletes(GameTestHelper h) {
        var p=new EventCheckpoint();p.restore("ABORTED",4,127,0);
        h.assertTrue(p.state==EventCheckpoint.State.ABORTED,"Framework Failure: aborted checkpoint promoted to completion");
        p.restore("COMPLETED",2,127,0);
        h.assertTrue(p.state==EventCheckpoint.State.FAILED_RECOVERABLE&&p.checkpoint==2,"Framework Failure: completion contradicts safe checkpoint");
        p.restore("ACTIVE",4,127,0);
        h.assertTrue(p.state==EventCheckpoint.State.FAILED_RECOVERABLE&&p.checkpoint==3,"Framework Failure: active state promoted to completion");h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=400)
    public static void schemaFourDiskStress(GameTestHelper h) throws Exception {
        var dir=java.nio.file.Path.of(System.getProperty("whileaway.testEvidence","../evidence/lifecycle"),"nbt-stress");
        java.nio.file.Files.createDirectories(dir);
        var id=UUID.fromString("34567890-1234-1234-1234-123456789012");
        int cycles=0;
        for(int kind=1;kind<=3;kind++)for(int cp=0;cp<=4;cp++) {
            var d=new NarrativeData();var e=d.entry(id);var r=new SceneRecord(kind,new BlockPos(-19,65,31));e.scenes.put(kind,r);
            for(int n=1;n<=cp;n++)r.progress.advance(n);
            r.progress.shown=127;e.cityClues=5;e.cityShockTriggered=kind==3&&cp==4;
            var input=d.save(new CompoundTag(),h.getLevel().registryAccess());
            var player=input.getList("players",10).getCompound(0);
            player.remove("readingUntil");player.remove("transitAfter");player.putString("unknown_extension","ignored");
            CompoundTag normalized=null;
            var path=dir.resolve("kind-"+kind+"-cp-"+cp+".dat");
            for(int cycle=0;cycle<30;cycle++) {
                net.minecraft.nbt.NbtIo.writeCompressed(input,path);
                var disk=net.minecraft.nbt.NbtIo.readCompressed(path,net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                var loaded=NarrativeData.load(disk,h.getLevel().registryAccess());var a=loaded.entry(id).scenes.get(kind);
                h.assertTrue(a.id.equals(r.id)&&a.progress.checkpoint==cp&&a.progress.shown==127,"disk stress lost committed checkpoint/cues");
                h.assertTrue(loaded.entry(id).cityClues==5,"disk stress lost permanent evidence");
                input=loaded.save(new CompoundTag(),h.getLevel().registryAccess());
                if(normalized==null)normalized=input;else h.assertTrue(normalized.equals(input),"4 -> 4 drift");cycles++;
            }
        }
        java.nio.file.Files.writeString(dir.resolve("RESULT.txt"),"PASS schema4 disk cycles="+cycles+"; scenes=3; boundaries=5; repeats=30\n");h.succeed();
    }
    @GameTest(template="empty")
    public static void migratedPlayerIdentitiesRemainIsolated(GameTestHelper h) {
        var root=new CompoundTag();root.putInt("schema",3);var list=new net.minecraft.nbt.ListTag();
        for(int i=1;i<=2;i++) {
            var t=new CompoundTag();t.putUUID("id",new UUID(0,i));t.putBoolean("cityVisited",true);t.putBoolean("cityShockTriggered",true);list.add(t);
        }
        root.put("players",list);var d=NarrativeData.load(root,h.getLevel().registryAccess());
        h.assertTrue(!d.entry(new UUID(0,1)).scenes.get(2).id.equals(d.entry(new UUID(0,2)).scenes.get(2).id),"same origin does not collapse players");
        var copy=NarrativeData.load(d.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess());
        copy.entry(new UUID(0,1)).cityClues=7;
        h.assertTrue(d.entry(new UUID(0,1)).cityClues==0,"copied ledger is independent, same UUID does not imply shared memory");h.succeed();
    }

    @GameTest(template="empty")
    public static void investigationOrdersAndBoundaries(GameTestHelper h) {
        int[][] orders={{1,2,4},{1,4,2},{2,1,4},{2,4,1},{4,1,2},{4,2,1}};
        for(var order:orders) {
            var r=new CityInvestigation();
            for(int i=0;i<3;i++) {
                h.assertTrue(r.accept(order[i]),"new evidence accepted");
                h.assertTrue(!r.accept(order[i]),"duplicate evidence suppressed");
                r=CityInvestigation.load(r.save(),r.evidence(),true);
                h.assertTrue(r.progress.checkpoint==i+1,"safe checkpoint follows facts, not arrival order");
                if(i<2)h.assertTrue(!r.confirm(),"incomplete investigation never resolves");
            }
            h.assertTrue(r.progress.state==EventCheckpoint.State.RESOLVING,"D boundary is durable before confirmation");
            h.assertTrue(r.confirm()&&!r.confirm(),"one completion claim");
            for(int reload=0;reload<30;reload++) {
                r=CityInvestigation.load(r.save(),r.evidence(),true);
                h.assertTrue(r.progress.state==EventCheckpoint.State.COMPLETED&&r.noticeClaimed()&&!r.confirm(),"E boundary does not repeat summary");
            }
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void investigationLegacyAndDamage(GameTestHelper h) {
        var legacy=CityInvestigation.load(new CompoundTag(),7,false);
        h.assertTrue(legacy.progress.checkpoint==4&&legacy.noticeClaimed(),"old all-records fact does not replay notice");
        var broken=legacy.save();broken.putInt("evidence",1);
        var partial=CityInvestigation.load(broken,1,true);
        h.assertTrue(partial.progress.checkpoint==1&&!partial.confirm(),"corrupt completion cannot invent missing evidence");
        var pending=new CityInvestigation();pending.accept(1);pending.accept(4);pending.accept(2);
        var missing=pending.save();missing.remove("state");missing.remove("checkpoint");
        var recovered=CityInvestigation.load(missing,7,true);
        h.assertTrue(recovered.progress.checkpoint==3&&recovered.confirm(),"facts rebuild safe finalization checkpoint");h.succeed();
    }

    @GameTest(template="empty")
    public static void investigationPreparedReadSurvives(GameTestHelper h) {
        var r=new CityInvestigation();h.assertTrue(r.reserve(4)&&!r.reserve(4),"one durable read reservation");
        r=CityInvestigation.load(r.save(),0,true);
        h.assertTrue(r.pendingEvidence()==4&&r.evidence()==0&&r.progress.checkpoint==0,"A restores intent without inventing completion");
        h.assertTrue(r.accept(4),"reserved read can commit");r=CityInvestigation.load(r.save(),4,true);
        h.assertTrue(r.pendingEvidence()==0&&r.evidence()==4&&r.progress.checkpoint==1&&!r.reserve(4),"B commits once without leaving an orphan reservation");h.succeed();
    }
    @GameTest(template="empty")
    public static void actualLegacyMigrationTriplicates(GameTestHelper h) throws Exception {
        var dir=java.nio.file.Path.of(System.getProperty("whileaway.testEvidence","../evidence/lifecycle"));int copies=0;
        java.nio.file.Files.createDirectories(dir.resolve("nbt-stress")); // Independent of another test running first.
        for(int schema:new int[]{2,3}) {
            var input=net.minecraft.nbt.NbtIo.readCompressed(java.nio.file.Path.of(System.getProperty("whileaway.testFixtures","../evidence/reuse/baseline"),"legacy-v"+schema+".dat"),net.minecraft.nbt.NbtAccounter.unlimitedHeap()).getCompound("data");
            h.assertTrue(input.getInt("schema")==schema,"actual legacy fixture schema");
            CompoundTag first=null;
            for(int copy=0;copy<3;copy++) {
                var output=NarrativeData.load(input.copy(),h.getLevel().registryAccess()).save(new CompoundTag(),h.getLevel().registryAccess());
                if(first==null)first=output;else h.assertTrue(first.equals(output),"actual same save migrated differently");
                var oldPlayers=input.getList("players",10);var newPlayers=output.getList("players",10);
                h.assertTrue(oldPlayers.size()==newPlayers.size(),"legacy player count preserved");
                for(int n=0;n<oldPlayers.size();n++)for(var key:java.util.List.of("station","encounter","clues","complete","cityVisited","cityClues","returnPosition")) {
                    if(oldPlayers.getCompound(n).contains(key))h.assertTrue(oldPlayers.getCompound(n).get(key).equals(newPlayers.getCompound(n).get(key)),"legacy fact changed: "+key);
                }
                net.minecraft.nbt.NbtIo.writeCompressed(output,dir.resolve("nbt-stress/legacy-v"+schema+"-copy-"+copy+".dat"));copies++;
            }
        }
        java.nio.file.Files.writeString(dir.resolve("nbt-stress/LEGACY.txt"),"PASS actual legacy schemas=2,3; identical migrations="+copies+"; original files read only via copies\n");h.succeed();
    }

    @GameTest(template="empty")
    public static void checkpointSaveWaitsForDurability(GameTestHelper h) throws Exception {
        var dir=java.nio.file.Path.of(System.getProperty("whileaway.testEvidence","../evidence/lifecycle"),"io-probe");java.nio.file.Files.createDirectories(dir);
        var file=java.nio.file.Files.createTempFile(dir,"durability-",".dat");java.nio.file.Files.delete(file);
        var blocked=new java.util.concurrent.CountDownLatch(1);
        net.neoforged.neoforge.common.IOUtilities.withIOWorker(()->{try {blocked.await();}catch(InterruptedException ex){Thread.currentThread().interrupt();}});
        boolean durable=false;
        try {
            var d=new NarrativeData();d.discover(new UUID(0,91),Clue.STATION);d.save(file.toFile(),h.getLevel().registryAccess());
            durable=java.nio.file.Files.exists(file)&&net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.unlimitedHeap()).getCompound("data").getInt("schema")==4;
        } finally {blocked.countDown();net.neoforged.neoforge.common.IOUtilities.waitUntilIOWorkerComplete();}
        h.assertTrue(durable,"Framework Failure: checkpoint save returned before queued disk write");h.succeed();
    }

    @GameTest(template="empty")
    public static void failedCheckpointSaveRemainsDirty(GameTestHelper h) throws Exception {
        var root=java.nio.file.Path.of(System.getProperty("whileaway.testEvidence","../evidence/lifecycle"),"io-probe");java.nio.file.Files.createDirectories(root);
        var directory=java.nio.file.Files.createTempDirectory(root,"not-a-file-");
        var d=new NarrativeData();d.discover(new UUID(0,92),Clue.STATION);boolean failed=false;
        try {d.save(directory.toFile(),h.getLevel().registryAccess());}catch(java.io.UncheckedIOException expected){failed=true;}
        h.assertTrue(failed&&d.isDirty()&&java.nio.file.Files.isDirectory(directory),"write failure is visible and cannot clear pending facts");h.succeed();
    }
}
