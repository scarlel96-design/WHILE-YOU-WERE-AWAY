package io.github.whileaway;

import java.io.IOException;
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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(WhileAway.ID)
@PrefixGameTestTemplate(false)
public final class ReturnNetworkTransactionGameTests {
    private static ReturnNetworkState discovered() {
        return ReturnNetworkState.notStarted().discover(
            new ReturnNetworkState.Prerequisite(4,true,true,true),
            UUID.fromString("12345678-1234-1234-1234-123456789012"),
            new ReturnNetworkState.Location("rail_relay","minecraft:overworld",new BlockPos(12,64,-8).asLong()));
    }
    private static Path file(String prefix) throws IOException {
        Path root=Path.of(System.getProperty("whileaway.testEvidence","../evidence/lifecycle"),"return-network-schema5");
        Files.createDirectories(root); return Files.createTempDirectory(root,prefix).resolve("whileaway_story.dat").toAbsolutePath().normalize();
    }
    private static byte[] bytes(Path p){try{return Files.readAllBytes(p);}catch(IOException e){throw new UncheckedIOException(e);}}
    private static CompoundTag disk(Path p){try{return NbtIo.readCompressed(p,NbtAccounter.unlimitedHeap()).getCompound("data");}catch(IOException e){throw new UncheckedIOException(e);}}
    private static void canonical(NarrativeData d,Path p,GameTestHelper h){
        d.setDirty(); d.save(p.toFile(),h.getLevel().registryAccess()); d.bindStorage(p,StoryStorage.digest(bytes(p)));
    }
    @GameTest(template="empty")
    public static void staleReturnNetworkDraftRejected(GameTestHelper h) throws Exception {
        Path p=file("stale-"); NarrativeData d=new NarrativeData(); canonical(d,p,h);
        ReturnNetworkState old=d.returnNetwork(); d.commitReturnNetwork(old,discovered(),h.getLevel().registryAccess());
        CompoundTag durable=disk(p).getCompound("returnNetwork").copy();
        boolean rejected=false; try{d.commitReturnNetwork(old,discovered(),h.getLevel().registryAccess());}catch(IllegalStateException e){rejected=e.getMessage().contains("STALE_DRAFT");}
        h.assertTrue(rejected,"stale expected rejected"); h.assertTrue(disk(p).getCompound("returnNetwork").equals(durable),"stale submit leaves disk unchanged"); h.succeed();
    }
    @GameTest(template="empty")
    public static void returnNetworkCommitSurvivesFreshStoryReload(GameTestHelper h) throws Exception {
        Path p=file("reload-"); NarrativeData d=new NarrativeData(); canonical(d,p,h);
        ReturnNetworkState c=d.commitReturnNetwork(d.returnNetwork(),discovered(),h.getLevel().registryAccess()).state();
        NarrativeData fresh=NarrativeData.load(disk(p),h.getLevel().registryAccess());
        h.assertTrue(fresh.returnNetwork().save().equals(c.save()),"fresh disk load sees committed state"); h.succeed();
    }
    @GameTest(template="empty")
    public static void externalStoryFileChangeBlocksReturnNetworkCommit(GameTestHelper h) throws Exception {
        Path p=file("external-"); NarrativeData d=new NarrativeData(); canonical(d,p,h); ReturnNetworkState old=d.returnNetwork();
        Files.write(p,new byte[]{1,2,3,4}); boolean rejected=false;
        try{d.commitReturnNetwork(old,discovered(),h.getLevel().registryAccess());}catch(UncheckedIOException e){rejected=true;}
        h.assertTrue(rejected&&!d.storySaveWritable(),"source mismatch blocks writer");
        h.assertTrue(d.returnNetwork().save().equals(old.save()),"source mismatch never publishes draft"); h.succeed();
    }
    @GameTest(template="empty")
    public static void failedCommitDoesNotPublishDraft(GameTestHelper h) throws Exception {
        Path p=file("iofail-"); NarrativeData d=new NarrativeData(); canonical(d,p,h); ReturnNetworkState old=d.returnNetwork(); byte[] before=bytes(p);
        d.setStoryWriterForTest((root,target)->{throw new IOException("injected");}); boolean failed=false;
        try{d.commitReturnNetwork(old,discovered(),h.getLevel().registryAccess());}catch(UncheckedIOException e){failed=true;}
        h.assertTrue(failed,"injected write failure visible");
        h.assertTrue(d.returnNetwork().save().equals(old.save()),"failed write never publishes draft");
        h.assertTrue(java.util.Arrays.equals(before,bytes(p)),"failed writer leaves canonical file unchanged"); h.succeed();
    }
}
