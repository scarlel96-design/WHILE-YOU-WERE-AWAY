package io.github.whileaway.core;

import java.util.*;

/** Stable v1 district plan. Never reorder this plan without migrating the saved construction cursor. */
public final class CityLayout {
    public enum Kind { ROAD, LINE, PAVING, WALL, TRIM, GLASS, FLOOR, LAMP, POST, BENCH, GATE, INTAKE, HOME, SIREN,
        AIR, BRICK, PLASTER, COPPER, DARK, CRACKED, MOSS, GRATE, WARM, RED, CHAIN, CHAIR, WATER, EARTH, WOOD, LEAF, RUBBLE, TRAM, WHEEL, CLOCK }
    public record Cell(int x,int y,int z,Kind kind) {}
    public record Building(int x,int z,int width,int depth,int floors) {}
    public static final List<Building> BUILDINGS=List.of(
        new Building(8,8,22,18,3),new Building(50,8,22,18,4),
        new Building(8,34,22,18,2),new Building(50,34,22,18,3),
        new Building(8,60,22,14,2),new Building(50,60,22,14,2));
    public static final int SIZE=80, BASE_Y=64, VERSION=1;
    public static final List<Cell> CELLS=build();
    private CityLayout() {}
    private static List<Cell> build() {
        var cells=new LinkedHashMap<String,Cell>();
        class Draw {
            void at(int x,int y,int z,Kind k){cells.put(x+","+y+","+z,new Cell(x,y,z,k));}
        }
        var d=new Draw();
        for(int x=0;x<SIZE;x++)for(int z=0;z<SIZE;z++) {
            boolean road=x>=34&&x<=45 || z>=28&&z<=31 || z>=54&&z<=57;
            d.at(x,0,z,road ? (x==40&&z%6<3?Kind.LINE:Kind.ROAD):Kind.PAVING);
            if(x==0||z==0||x==79||z==79)for(int y=1;y<=5;y++)d.at(x,y,z,Kind.WALL);
        }
        for(var b:BUILDINGS) {
            int height=b.floors*5;
            for(int x=b.x;x<b.x+b.width;x++)for(int z=b.z;z<b.z+b.depth;z++) {
                boolean wall=x==b.x||x==b.x+b.width-1||z==b.z||z==b.z+b.depth-1;
                boolean doorway=z==b.z+b.depth-1 && x>=b.x+9&&x<=b.x+11;
                for(int y=1;y<=height+1;y++) {
                    if(doorway&&y<=3)continue;
                    if(y%5==0||y==height+1) d.at(x,y,z,y==height+1?Kind.TRIM:Kind.FLOOR);
                    else if(wall) {
                        boolean window=y%5>=2&&y%5<=3&&(x+z)%5>=1&&(x+z)%5<=3;
                        d.at(x,y,z,window?Kind.GLASS:Kind.WALL);
                    }
                }
            }
            // Ground-floor waiting benches and a dim fixture; upper floors are visual shells for now.
            for(int x=b.x+3;x<b.x+8;x++)d.at(x,1,b.z+4,Kind.BENCH);
            d.at(b.x+10,4,b.z+6,Kind.LAMP);
        }
        for(int z=8;z<75;z+=12)for(int x:new int[]{32,47}) {
            for(int y=1;y<=5;y++)d.at(x,y,z,Kind.POST);
            d.at(x,6,z,Kind.LAMP);
        }
        // Evacuation checkpoint and the return light are permanent route anchors.
        for(int x=31;x<=48;x++)d.at(x,7,4,Kind.TRIM);
        for(int y=1;y<=6;y++)for(int x:new int[]{31,48})d.at(x,y,4,Kind.POST);
        d.at(40,1,72,Kind.GATE);
        d.at(18,1,47,Kind.INTAKE);
        d.at(60,1,69,Kind.HOME);
        d.at(40,1,7,Kind.SIREN);
        return List.copyOf(cells.values());
    }
}
