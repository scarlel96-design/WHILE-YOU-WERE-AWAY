package io.github.whileaway.core;

import java.util.*;
import static io.github.whileaway.core.CityLayout.Kind.*;
import io.github.whileaway.core.CityLayout.*;

/** New-district living spaces. Revision-1 and revision-2 construction orders stay unchanged. */
public final class CityLivingLayout {
    public static final int VERSION=3;
    public static final List<Cell> ADDITIONS=build();
    public static final List<Cell> CELLS;
    private static final Map<String,Kind> UNDERLAY=new HashMap<>();
    static {
        for(var c:CityArtLayout.CELLS)UNDERLAY.put(key(c),c.kind());
        var all=new ArrayList<>(CityArtLayout.CELLS);all.addAll(ADDITIONS);CELLS=List.copyOf(all);
    }
    private CityLivingLayout() {}
    private static String key(Cell c){return c.x()+","+c.y()+","+c.z();}
    public static Kind underlay(Cell c){return UNDERLAY.getOrDefault(key(c),AIR);}
    private static List<Cell> build() {
        var cells=new LinkedHashMap<String,Cell>();
        class Draw {
            void at(int x,int y,int z,Kind kind){var c=new Cell(x,y,z,kind);cells.put(key(c),c);}
            void row(int x,int y,int z,int length,Kind kind){for(int i=0;i<length;i++)at(x+i,y,z,kind);}
        }
        var d=new Draw();
        // Yeoul's small caretaker home opens north and east. Keep the light and arrival approach clear.
        for(int x=35;x<=39;x++)for(int z=70;z<=75;z++) {
            d.at(x,0,z,FLOOR);d.at(x,5,z,COPPER);
            for(int y=1;y<=4;y++) {
                boolean wall=x==35||x==39||z==70||z==75;
                boolean door=(z==70&&x==38)||(x==39&&z==72)||(z==75&&x==38);
                if(wall&&!(door&&y<=3))d.at(x,y,z,y==3?GLASS:BRICK);
            }
        }
        d.at(36,1,71,MAT);d.at(36,1,72,MAT);
        d.at(36,1,74,WORKBENCH);d.at(37,1,74,STORAGE);d.at(37,4,73,WARM);
        // A rest seat faces the road. The standing space beside it stays empty.
        d.at(35,1,73,CHAIR);
        // Family room: usable storage, crafting, and ordinary furniture without a clue at every object.
        d.at(54,1,64,WORKBENCH);d.at(55,1,64,STORAGE);d.at(66,1,65,STORAGE);
        d.row(63,1,68,3,BENCH);d.at(63,1,67,CHAIR);d.at(65,1,67,CHAIR);
        d.at(55,1,66,MAT);d.at(55,1,67,MAT);
        d.at(60,0,73,ROUTE_TRACE);d.at(60,0,74,EARTH);d.at(60,0,75,EARTH);
        // The boarding-house common room remains an ordinary player-usable place.
        d.at(12,1,64,WORKBENCH);d.at(13,1,64,STORAGE);d.row(14,1,69,4,BENCH);
        d.at(14,1,68,CHAIR);d.at(17,1,68,CHAIR);
        // Workshop: a real tool bench, furnace, storage, and a player-operated service circuit.
        d.at(64,1,38,TOOL_BENCH);d.at(65,1,38,WORKBENCH);d.at(66,1,38,FURNACE);
        d.at(54,1,39,STORAGE);d.at(54,1,40,STORAGE);d.row(57,1,39,3,BENCH);
        d.at(65,0,46,SERVICE_LAMP);d.at(65,1,46,SERVICE_LEVER);d.at(66,1,46,MAINTENANCE_TAG);
        return List.copyOf(cells.values());
    }
}
