package io.github.whileaway;
import java.nio.file.*;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.*;
/** Mutates only the explicit test-copy file supplied by the runner. */
public final class NpcCorruptionFixture {
    public static void main(String[] args)throws Exception {
        SharedConstants.tryDetectVersion();var file=Path.of(args[0]);String mode=args[1];
        var root=NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap());var list=root.getCompound("data").getList("actors",10);
        if(list.size()!=1)throw new IllegalStateException("fixture requires one saved NPC");var a=list.getCompound(0);
        if(!a.getString("storyId").equals(NpcEvents.STORY)||a.getInt("checkpoint")!=4)throw new IllegalStateException("fixture requires completed campaign NPC");
        var facts=a.getList("facts",8);
        switch(mode){
            case "MissingHome" -> facts.removeIf(t->t.getAsString().startsWith("npc.home="));
            case "MissingExperience" -> facts.removeIf(t->t.getAsString().equals(NpcState.OBSERVED));
            case "WrongWork" -> {facts.removeIf(t->t.getAsString().startsWith("npc.work="));facts.add(StringTag.valueOf("npc.work=check_return_light"));}
            case "Recover" -> a.getCompound("snapshot").remove("Motion");
            case "Control" -> {}
            default -> throw new IllegalArgumentException("unknown fixture mode");
        }
        NbtIo.writeCompressed(root,file);System.out.println("PASS explicit NPC fixture "+mode+" actorCount=1 identity/checkpoint unchanged");
    }
}
