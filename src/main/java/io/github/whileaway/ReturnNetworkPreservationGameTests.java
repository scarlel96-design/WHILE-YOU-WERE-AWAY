package io.github.whileaway;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(WhileAway.ID)
@PrefixGameTestTemplate(false)
public final class ReturnNetworkPreservationGameTests {
    private static ReturnNetworkState discovered() {
        return ReturnNetworkState.notStarted().discover(
            new ReturnNetworkState.Prerequisite(4,true,true,true),
            UUID.fromString("12345678-1234-1234-1234-123456789012"),
            new ReturnNetworkState.Location("rail_relay","minecraft:overworld",new BlockPos(12,64,-8).asLong()));
    }
    @GameTest(template="empty")
    public static void schema5PreservesExistingPlayersAndActors(GameTestHelper h) throws Exception {
        Path root=Path.of(System.getProperty("whileaway.testEvidence","../evidence/lifecycle"),"return-network-schema5");
        Files.createDirectories(root); Path p=Files.createTempDirectory(root,"preserve-").resolve("whileaway_story.dat").toAbsolutePath().normalize();
        NarrativeData d=new NarrativeData();
        UUID player=UUID.fromString("20000000-0000-0000-0000-000000000002");
        d.discover(player,io.github.whileaway.core.Clue.STATION); d.entry(player).cityVisited=true;
        StoryActorRecord a=new StoryActorRecord("npc:test","event:test","witness","minecraft:pig","minecraft:overworld",
            UUID.fromString("30000000-0000-0000-0000-000000000003"),null,
            UUID.fromString("40000000-0000-0000-0000-000000000004"),new BlockPos(3,64,3),false);
        a.generation=2; a.facts.add("kept"); d.actors.put(a.storyId,a);
        CompoundTag before=d.save(new CompoundTag(),h.getLevel().registryAccess());
        d.setDirty(); d.save(p.toFile(),h.getLevel().registryAccess());
        d.bindStorage(p,StoryStorage.digest(Files.readAllBytes(p)));
        d.commitReturnNetwork(d.returnNetwork(),discovered(),h.getLevel().registryAccess());
        CompoundTag after;
        try { after=NbtIo.readCompressed(p,NbtAccounter.unlimitedHeap()).getCompound("data"); }
        catch(java.io.IOException e){throw new UncheckedIOException(e);}
        h.assertTrue(after.getList("players",Tag.TAG_COMPOUND).equals(before.getList("players",Tag.TAG_COMPOUND)),"players preserved across Return Network commit");
        h.assertTrue(after.getList("actors",Tag.TAG_COMPOUND).equals(before.getList("actors",Tag.TAG_COMPOUND)),"actors preserved across Return Network commit");
        h.succeed();
    }
}
