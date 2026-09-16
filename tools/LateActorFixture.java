package io.github.whileaway;
import java.nio.file.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.storage.*;

/** Offline mutation of an explicit copied world: a stale actor in a previously untouched entity chunk. */
public final class LateActorFixture {
    public static void main(String[] args)throws Exception {
        Path world=Path.of(args[0]),source=Path.of(args[1]);
        if(!world.getFileName().toString().equals("canonical-Late"))throw new IllegalArgumentException("fixture target only");
        var original=NbtIo.readCompressed(source,NbtAccounter.create(64*1024*1024));
        var actor=original.getCompound("data").getList("actors",10).getCompound(0);
        var entity=actor.getCompound("snapshot").copy();entity.putString("id",actor.getString("entityType"));entity.putUUID("UUID",actor.getUUID("entityUUID"));
        var pos=new ListTag();pos.add(DoubleTag.valueOf(9600.5));pos.add(DoubleTag.valueOf(100));pos.add(DoubleTag.valueOf(9600.5));entity.put("Pos",pos);
        var chunk=new CompoundTag();chunk.putInt("DataVersion",original.getInt("DataVersion"));chunk.putIntArray("Position",new int[]{600,600});
        var entities=new ListTag();entities.add(entity);chunk.put("Entities",entities);
        Path dir=world.resolve("entities");Files.createDirectories(dir);Path region=dir.resolve("r.18.18.mca");
        if(Files.exists(region))throw new IllegalStateException("fixture region already exists");
        var cp=new ChunkPos(600,600);
        try(var file=new RegionFile(new RegionStorageInfo("copied-fixture",Level.OVERWORLD,"entities"),region,dir,true)) {
            try(var out=file.getChunkDataOutputStream(cp)){NbtIo.write(chunk,out);}file.flush();
            try(var in=file.getChunkDataInputStream(cp)){if(!NbtIo.read(in,NbtAccounter.create(64*1024*1024)).equals(chunk))throw new AssertionError("entity fixture readback");}
        }
        System.out.println("PASS offline stale entity chunk (600,600) written and reopened; generation="+actor.getInt("generation")+" uuid="+actor.getUUID("entityUUID"));System.exit(0);
    }
}
