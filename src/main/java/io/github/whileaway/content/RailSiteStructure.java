package io.github.whileaway.content;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import io.github.whileaway.WhileAway;
import java.util.Optional;
import java.util.function.IntBinaryOperator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

/** Skip wet or steep rail-site candidates before any block placement. */
public final class RailSiteStructure extends Structure {
    public static final MapCodec<RailSiteStructure> CODEC=JigsawStructure.CODEC.codec()
        .fieldOf("layout").xmap(RailSiteStructure::new,site->site.layout);
    private final JigsawStructure layout;

    public RailSiteStructure(JigsawStructure layout){
        super(layout.getModifiedStructureSettings());
        this.layout=layout;
    }

    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context){
        var candidate=layout.findGenerationPoint(context);
        if(candidate.isEmpty())return Optional.empty();
        var builder=candidate.orElseThrow().getPiecesBuilder();
        var pieces=builder.build().pieces();
        // These two sites use one bounded template, not an expanding rail network.
        if(pieces.size()!=1)return Optional.empty();
        var box=pieces.getFirst().getBoundingBox();
        boolean fits=terrainFits(box,
            (x,z)->context.chunkGenerator().getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,context.heightAccessor(),context.randomState()),
            (x,z)->context.chunkGenerator().getBaseHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,context.heightAccessor(),context.randomState()));
        if(!fits)return Optional.empty();
        // Reuse the chosen rotation and pieces. Do not consume the layout RNG twice.
        return Optional.of(new GenerationStub(candidate.orElseThrow().position(),Either.right(builder)));
    }

    public static boolean terrainFits(BoundingBox box,IntBinaryOperator surface,IntBinaryOperator solidFloor){
        if(box.getXSpan()>32||box.getZSpan()>32)return false;
        int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE;
        for(int ix=0;ix<5;ix++)for(int iz=0;iz<5;iz++){
            int x=box.minX()+ix*(box.getXSpan()-1)/4;
            int z=box.minZ()+iz*(box.getZSpan()-1)/4;
            int y=surface.applyAsInt(x,z);
            if(y!=solidFloor.applyAsInt(x,z))return false;
            low=Math.min(low,y);high=Math.max(high,y);
            if(high-low>2)return false;
        }
        return true;
    }

    @Override public StructureType<?> type(){return WhileAway.RAIL_SITE.get();}
}
