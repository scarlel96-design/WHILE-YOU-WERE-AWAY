package io.github.whileaway;

import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.*;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/** Actual storage-library characterization, not a client or recovery PASS.
 * Copies a recorded client checkpoint into a dedicated probe folder; never edits a world. */
public final class SavedDataLoadProbe {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Path input=Path.of(args[0]).toAbsolutePath().normalize();
        Path out=Path.of(args[1]).toAbsolutePath().normalize();
        if(Files.exists(out))throw new IllegalArgumentException("Preserve previous probe folder");
        Files.createDirectories(out);
        byte[] original=Files.readAllBytes(input);
        CompoundTag valid=NbtIo.readCompressed(input,NbtAccounter.unlimitedHeap());
        int expected=valid.getCompound("data").getList("actors",Tag.TAG_COMPOUND).size();
        if(expected!=1)throw new IllegalArgumentException("Expected one recorded actor in control checkpoint");
        var factory=new SavedData.Factory<>(NarrativeData::new,NarrativeData::load,null);
        for(boolean corrupt:new boolean[]{false,true}) {
            Path folder=out.resolve(corrupt?"missing-uuid":"control");Files.createDirectories(folder);
            CompoundTag root=valid.copy();
            if(corrupt)root.getCompound("data").getList("actors",Tag.TAG_COMPOUND).getCompound(0).remove("entityUUID");
            Path file=folder.resolve("whileaway_story.dat");NbtIo.writeCompressed(root,file);
            byte[] saved=Files.readAllBytes(file);
            var storage=new DimensionDataStorage(folder.toFile(),null,null);
            NarrativeData loaded=storage.computeIfAbsent(factory,"whileaway_story");
            boolean unchanged=java.util.Arrays.equals(saved,Files.readAllBytes(file));
            System.out.println("STORAGE_PROBE case="+(corrupt?"missing-uuid":"control")+" expectedActors="+expected+" loadedActors="+loaded.actors.size()+" fileUnchanged="+unchanged);
            if(!unchanged)throw new AssertionError("Read-only probe unexpectedly wrote saved data");
            if(!corrupt&&loaded.actors.size()!=expected)throw new AssertionError("Normal control failed; corruption result invalid");
            if(corrupt&&loaded.actors.isEmpty())System.out.println("FAIL FAIL_CLOSED: rejected actor data became an empty NarrativeData through computeIfAbsent; no save invoked by this probe");
        }
        if(!java.util.Arrays.equals(original,Files.readAllBytes(input)))throw new AssertionError("Source checkpoint changed");
        System.out.println("SOURCE_PRESERVED sha256="+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(original)));
        System.out.println("CHARACTERIZATION_COMPLETE; library probe only; recovery and actual client remain NOT TESTED");
        // Minecraft utility initialization creates background pools in this standalone process.
        // No server/window is running, and every probe file write above has completed.
        System.exit(0);
    }
}
