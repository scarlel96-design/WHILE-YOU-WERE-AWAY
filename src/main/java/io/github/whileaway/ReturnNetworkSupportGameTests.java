package io.github.whileaway;

import java.util.*;
import io.github.whileaway.content.RelayBlock;
import io.github.whileaway.support.ReturnNetworkLoadAssessment;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.*;

import static io.github.whileaway.support.ReturnNetworkLoadAssessment.*;

/**
 * Support-level coverage only. Model observations do not prove actual chunk unload, entity
 * storage reload, updater execution, or a real client. No test spawns/teleports a player or NPC,
 * force-loads a chunk, or advances a durable campaign transaction.
 */
@GameTestHolder(WhileAway.ID)
@PrefixGameTestTemplate(false)
public final class ReturnNetworkSupportGameTests {
    private static final UUID EVENT = UUID.fromString("81000000-0000-0000-0000-000000000001");

    private static Snapshot snapshot(
        boolean player, boolean dimension, boolean npcChunk, boolean npcStorage,
        boolean canonical, boolean relay, boolean environment, Path path
    ) {
        return new Snapshot(player, dimension, npcChunk, npcStorage, canonical, relay, environment, path);
    }

    private static ReturnNetworkState response(StoryActorRecord resident) {
        return ReturnNetworkState.notStarted()
            .discover(new ReturnNetworkState.Prerequisite(4, true, true, true), EVENT,
                new ReturnNetworkState.Location(ReturnNetworkEvents.LOCATION, "minecraft:overworld", ReturnNetworkEvents.RELAY.asLong()))
            .join(new ReturnNetworkState.Participant(
                resident.storyId, resident.instanceId, resident.entityId, resident.generation))
            .observePattern().intervene().beginPresentation().observeResponse();
    }

    @GameTest(template="empty")
    public static void loadMatrixIsDiagnosticAndDurableWaitIsImmutable(GameTestHelper h) {
        var state = ReturnNetworkState.notStarted();
        var before = state.save();
        var cases = List.of(
            Map.entry(Desired.PLAYER_ABSENT, snapshot(false, false, false, false, false, false, false, Path.UNREACHABLE)),
            Map.entry(Desired.WRONG_DIMENSION, snapshot(true, false, false, false, false, false, false, Path.UNREACHABLE)),
            Map.entry(Desired.NPC_CHUNK_UNAVAILABLE, snapshot(true, true, false, false, false, false, false, Path.NOT_ASSESSED)),
            Map.entry(Desired.NPC_ENTITY_STORAGE_UNAVAILABLE, snapshot(true, true, true, false, false, false, false, Path.NOT_ASSESSED)),
            Map.entry(Desired.NPC_IDENTITY_UNCONFIRMED, snapshot(true, true, true, true, false, false, false, Path.NOT_ASSESSED)),
            Map.entry(Desired.RELAY_UNAVAILABLE, snapshot(true, true, true, true, true, false, false, Path.NOT_ASSESSED)),
            Map.entry(Desired.ENVIRONMENT_UNAVAILABLE, snapshot(true, true, true, true, true, true, false, Path.UNREACHABLE)),
            Map.entry(Desired.READY, snapshot(true, true, true, true, true, true, true, Path.ARRIVED))
        );
        for (var entry : cases) {
            var result = ReturnNetworkLoadAssessment.assess(entry.getValue());
            h.assertTrue(result.desired() == entry.getKey(), "load diagnostic " + entry.getKey());
            h.assertTrue(!result.factMutationAuthorized(), "diagnosis never authorizes a fact mutation");
        }
        h.assertTrue(state.waiting(false, false, false, false) == ReturnNetworkState.Wait.PLAYER_ABSENT,
            "durable helper keeps player precedence");
        h.assertTrue(state.waiting(true, false, false, false) == ReturnNetworkState.Wait.WRONG_DIMENSION,
            "durable helper keeps dimension precedence");
        h.assertTrue(state.waiting(true, true, false, false) == ReturnNetworkState.Wait.WAITING_NPC,
            "durable helper represents unconfirmed NPC generically");
        h.assertTrue(state.waiting(true, true, true, false) == ReturnNetworkState.Wait.WAITING_ENVIRONMENT,
            "durable helper represents environment wait");
        h.assertTrue(state.waiting(true, true, true, true) == ReturnNetworkState.Wait.NONE,
            "durable helper reports no coarse wait when all supplied observations are ready");
        h.assertTrue(before.equals(state.save()), "diagnostics and wait helper leave durable state immutable");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void playerPrecedesNpcRelayAndUnconfirmedIdentity(GameTestHelper h) {
        var productionBefore = ReturnNetworkEvents.waitState(h.getLevel());
        var absent = ReturnNetworkLoadAssessment.assess(
            snapshot(false, true, false, false, false, false, false, Path.CHUNK_UNAVAILABLE));
        var npcChunk = ReturnNetworkLoadAssessment.assess(
            snapshot(true, true, false, false, false, false, false, Path.CHUNK_UNAVAILABLE));
        var identity = ReturnNetworkLoadAssessment.assess(
            snapshot(true, true, true, true, false, true, true, Path.ARRIVED));
        h.assertTrue(absent.desired() == Desired.PLAYER_ABSENT, "player absence wins before NPC/relay reload");
        h.assertTrue(npcChunk.desired() == Desired.NPC_CHUNK_UNAVAILABLE, "NPC chunk is evaluated after eligible player returns");
        h.assertTrue(identity.desired() == Desired.NPC_IDENTITY_UNCONFIRMED, "loaded entity without canonical proof still waits");
        h.assertTrue(ReturnNetworkEvents.waitState(h.getLevel()) == productionBefore,
            "model-only assessment does not claim that the live-NPC-owned updater refreshed production wait state");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void pathClassificationFollowsEnvironmentAndNeverAuthorizesMutation(GameTestHelper h) {
        var blockedEnvironment = ReturnNetworkLoadAssessment.assess(
            snapshot(true, true, true, true, true, true, false, Path.ARRIVED));
        h.assertTrue(blockedEnvironment.desired() == Desired.ENVIRONMENT_UNAVAILABLE,
            "environment takes precedence even when a previous path observation arrived");
        for (Path path : Path.values()) {
            var result = ReturnNetworkLoadAssessment.assess(
                snapshot(true, true, true, true, true, true, true, path));
            h.assertTrue(!result.factMutationAuthorized(), "path " + path + " remains observation only");
            h.assertTrue(result.ready() == (path == Path.ARRIVED), "only ARRIVED is diagnostically ready: " + path);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void waitAndResumeRoundTripPreservesIdentityAndFacts(GameTestHelper h) {
        var data = new NarrativeData();
        var resident = ReturnNetworkFixtures.resident(data);
        var state = response(resident).shareExperience().complete();
        var identity = state.participant;
        var facts = Set.copyOf(state.facts);
        var actorIdentity = resident.entityId;
        var actorInstance = resident.instanceId;
        int actorGeneration = resident.generation;
        var actorFacts = Set.copyOf(resident.facts);
        var saved = state.save();
        var savedActor = resident.save();
        for (var observation : List.of(
            snapshot(false, true, false, false, false, false, false, Path.CHUNK_UNAVAILABLE),
            snapshot(true, true, false, false, false, false, false, Path.CHUNK_UNAVAILABLE),
            snapshot(true, true, true, false, false, false, false, Path.NOT_ASSESSED),
            snapshot(true, true, true, true, false, true, true, Path.ARRIVED),
            snapshot(true, true, true, true, true, true, true, Path.ARRIVED)
        )) ReturnNetworkLoadAssessment.assess(observation);
        var restored = ReturnNetworkState.load(saved);
        h.assertTrue(restored.participant.equals(identity), "wait/reload observations preserve participant identity");
        h.assertTrue(restored.facts.equals(facts), "wait/reload observations preserve facts");
        h.assertTrue(restored.save().equals(saved), "wait/resume round trip is byte-semantic unchanged");
        var restoredActor = StoryActorRecord.load(savedActor);
        h.assertTrue(restoredActor.entityId.equals(actorIdentity)
            && restoredActor.instanceId.equals(actorInstance)
            && restoredActor.generation == actorGeneration, "wait/resume serialization preserves actor identity");
        h.assertTrue(restoredActor.facts.equals(actorFacts), "wait/resume serialization preserves actor facts");
        h.succeed();
    }

    @GameTest(template="empty", batch="return_network_support_fixed_world")
    public static void actualEnvironmentChangedRelayObstructionAndWaterThenRestore(GameTestHelper h) {
        var level = h.getLevel();
        var relay = ReturnNetworkEvents.RELAY;
        var stand = ReturnNetworkEvents.STAND;
        var positions = List.of(relay, stand, stand.above(), stand.below());
        for (var position : positions)
            h.assertTrue(level.hasChunkAt(position), "fixture must already have fixed environment chunk loaded: " + position);
        List<BlockState> original = positions.stream().map(level::getBlockState).toList();
        var data = new NarrativeData();
        var active = response(ReturnNetworkFixtures.resident(data));
        var durableBefore = active.save();
        var actorFactsBefore = Set.copyOf(data.actors.get(NpcEvents.STORY).facts);
        try {
            level.setBlock(stand, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(stand.above(), Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(stand.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(relay, WhileAway.RELAY.get().defaultBlockState().setValue(RelayBlock.LIT, true), 3);
            h.assertTrue(ReturnNetworkEvents.environmentReady(level, active), "lit relay and clear supported stand are ready");

            level.setBlock(relay, WhileAway.RELAY.get().defaultBlockState().setValue(RelayBlock.LIT, false), 3);
            h.assertTrue(!ReturnNetworkEvents.environmentReady(level, active), "unlit relay waits");
            level.setBlock(relay, Blocks.AIR.defaultBlockState(), 3);
            h.assertTrue(!ReturnNetworkEvents.environmentReady(level, active), "removed relay waits");
            level.setBlock(relay, Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
            h.assertTrue(!ReturnNetworkEvents.environmentReady(level, active), "changed lamp waits without replacement");
            h.assertTrue(level.getBlockState(relay).is(Blocks.REDSTONE_LAMP), "diagnostic leaves changed lamp untouched");

            level.setBlock(relay, WhileAway.RELAY.get().defaultBlockState().setValue(RelayBlock.LIT, true), 3);
            level.setBlock(stand, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
            h.assertTrue(!ReturnNetworkEvents.environmentReady(level, active), "player obstruction waits");
            h.assertTrue(level.getBlockState(stand).is(Blocks.DIAMOND_BLOCK), "player obstruction remains untouched");
            level.setBlock(stand, Blocks.WATER.defaultBlockState(), 3);
            h.assertTrue(!ReturnNetworkEvents.environmentReady(level, active), "water waits");
            level.setBlock(stand, Blocks.AIR.defaultBlockState(), 3);
            h.assertTrue(ReturnNetworkEvents.environmentReady(level, active), "restored relay and stand re-evaluate ready");
            h.assertTrue(active.save().equals(durableBefore), "all actual environment probes leave durable event state unchanged");
            h.assertTrue(data.actors.get(NpcEvents.STORY).facts.equals(actorFactsBefore),
                "all actual environment probes leave completed prerequisite facts unchanged");
        } finally {
            for (int i = 0; i < positions.size(); i++) level.setBlock(positions.get(i), original.get(i), 3);
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void integrityCrossRecordConsistencySurvivesDiagnosticWaits(GameTestHelper h) {
        var data = new NarrativeData();
        var resident = ReturnNetworkFixtures.resident(data);
        var state = response(resident);
        h.assertTrue(ReturnNetworkIntegrity.issues(state, data.actors).isEmpty(), "bound resident and event agree");
        ReturnNetworkLoadAssessment.assess(
            snapshot(true, true, false, false, false, false, false, Path.CHUNK_UNAVAILABLE));
        h.assertTrue(ReturnNetworkIntegrity.issues(state, data.actors).isEmpty(), "load diagnosis cannot alter cross-record consistency");

        var conflicting = StoryActorRecord.load(resident.save());
        conflicting.entityId = UUID.fromString("81000000-0000-0000-0000-000000000099");
        var actors = new TreeMap<String, StoryActorRecord>();
        actors.put(conflicting.storyId, conflicting);
        h.assertTrue(ReturnNetworkIntegrity.issues(state, actors).contains("RN_PARTICIPANT_IDENTITY_MISMATCH"),
            "unconfirmed/conflicting identity is detected rather than selected");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void completedPrerequisiteAndReturnLightRegressionRetained(GameTestHelper h) {
        var data = new NarrativeData();
        var resident = ReturnNetworkFixtures.resident(data);
        h.assertTrue(ReturnNetworkIntegrity.prerequisite(resident), "completed checkpoint-4 resident remains eligible prerequisite");
        h.assertTrue(NpcState.integrity(resident).isEmpty(), "completed NPC receipts remain internally consistent");

        h.assertTrue(NpcState.home(resident).equals(new BlockPos(12, 64, -8)),
            "fixture retains the completed resident home binding");
        var level = h.getLevel();
        BlockPos home = h.absolutePos(new BlockPos(2, 2, 2));
        var positions = List.of(home.below(), home, home.above(), home.offset(2, 0, 0));
        for (var position : positions)
            h.assertTrue(level.hasChunkAt(position), "fixture must already have return-light probe chunk loaded: " + position);
        List<BlockState> original = positions.stream().map(level::getBlockState).toList();
        try {
            level.setBlock(home.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(home, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(home.above(), Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(home.offset(2, 0, 0), WhileAway.RETURN_LIGHT.get().defaultBlockState(), 3);
            var assessment = NpcEnvironment.inspect(level, level.dimension().location().toString(), home, home);
            h.assertTrue(assessment.dependenciesReady(), "completed resident's return-light dependency remains recognized");
            level.setBlock(home.offset(2, 0, 0), Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
            h.assertTrue(NpcEnvironment.inspect(level, level.dimension().location().toString(), home, home).light()
                == NpcEnvironment.Light.WRONG_BLOCK, "ordinary lamp is not inferred as the authored return light");
        } finally {
            for (int i = 0; i < positions.size(); i++) level.setBlock(positions.get(i), original.get(i), 3);
        }
        h.succeed();
    }
}
