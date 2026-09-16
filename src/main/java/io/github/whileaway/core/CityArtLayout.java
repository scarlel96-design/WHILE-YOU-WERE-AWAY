package io.github.whileaway.core;

import java.util.*;
import static io.github.whileaway.core.CityLayout.Kind.*;
import io.github.whileaway.core.CityLayout.*;

/** Art revision 2: a separate immutable plan. Old revision-1 saves keep their exact original cell order. */
public final class CityArtLayout {
    public static final int VERSION=2;
    public static final List<Cell> CELLS=build();
    private CityArtLayout() {}
    private static List<Cell> build() {
        var map=new LinkedHashMap<String,Cell>();
        class Draw {
            void at(int x,int y,int z,Kind kind){map.put(x+","+y+","+z,new Cell(x,y,z,kind));}
            void box(int x,int y,int z,int w,int h,int d,Kind kind){for(int a=x;a<x+w;a++)for(int b=y;b<y+h;b++)for(int c=z;c<z+d;c++)at(a,b,c,kind);}
        }
        var d=new Draw();
        for(var c:CityLayout.CELLS) {
            Kind k=c.kind();int noise=Math.floorMod(c.x()*73+c.y()*17+c.z()*131,29);
            if(k==WALL&&noise<4)k=CRACKED;
            if(k==PAVING&&noise<3)k=MOSS;
            if(k==ROAD&&noise==1)k=DARK;
            if(k==LAMP)k=(c.z()%24==8)?WARM:DARK;
            d.at(c.x(),c.y(),c.z(),k);
        }
        // Individual building identities: apartment, broadcast house, intake clinic, workshops, boarding house, family home.
        int index=0;
        for(var b:CityLayout.BUILDINGS) {
            Kind facade=switch(index){case 0,4->BRICK;case 2,5->PLASTER;case 3->COPPER;default->TRIM;};
            int top=b.floors()*5+1;
            for(int x=b.x();x<b.x()+b.width();x++)for(int z=b.z();z<b.z()+b.depth();z++) {
                boolean edge=x==b.x()||x==b.x()+b.width()-1||z==b.z()||z==b.z()+b.depth()-1;
                if(edge)for(int y=1;y<top;y++) {
                    var old=map.get(x+","+y+","+z);if(old==null)continue;
                    if(old.kind()==WALL||old.kind()==CRACKED)d.at(x,y,z,(x+z+y)%19==0?CRACKED:facade);
                    if(old.kind()==GLASS&&(x+z)%7==0)d.at(x,y,z,GRATE);
                }
                if(edge)d.at(x,top+1,z,index%2==0?BRICK:COPPER);
            }
            // Recessed roof plant, chimneys, visible pipes and canopy instead of a single flat slab.
            if(index==3) {
                for(int z=b.z()+3;z<b.z()+13;z+=4) {
                    d.box(b.x()+3,top+1,z,12,1,2,COPPER);
                    d.box(b.x()+4,top+2,z,10,1,1,GLASS);
                }
                d.box(b.x()+17,top+1,b.z()+3,3,8,3,BRICK);
            } else {
                d.box(b.x()+3,top+1,b.z()+3,4+index%3,2+index%2,4,index==2?COPPER:TRIM);
                if(index!=2)d.box(b.x()+5,top+3,b.z()+4,2,3+index%3,2,DARK);
            }
            for(int y=1;y<=top+2;y++)d.at(b.x()+b.width()-2,y,b.z()-1,CHAIN);
            d.box(b.x()+8,4,b.z()+b.depth(),6,1,3,index==2?COPPER:DARK);
            for(int x:new int[]{b.x()+8,b.x()+13})for(int y=1;y<4;y++)d.at(x,y,b.z()+b.depth()+2,POST);
            // Balconies face the central road, breaking up the silhouette at multiple floors.
            if(index==0||index==4||index==5)for(int y=5;y<top;y+=5) {
                int face=b.x()<40?b.x()+b.width():b.x()-2;
                d.box(face,y,b.z()+4,2,1,7,TRIM);
                for(int z=b.z()+4;z<b.z()+11;z++)d.at(face+(b.x()<40?1:0),y+1,z,GRATE);
            }
            // Interior evidence: counters, alternating waiting chairs, low warm pools of light.
            d.box(b.x()+3,1,b.z()+8,5,1,1,index==2?PLASTER:FLOOR);
            for(int x=b.x()+3;x<b.x()+12;x+=3)d.at(x,1,b.z()+3,CHAIR);
            d.at(b.x()+4,2,b.z()+8,WARM);
            if(index==2) {
                int x=b.x()+b.width();int z=b.z()+9;
                for(int y=7;y<=11;y++)d.at(x,y,z,RED);
                for(int dz=-2;dz<=2;dz++)d.at(x,9,z+dz,RED);
            }
            index++;
        }
        // Missing façade and spill of rubble at the workshop. Keep the original front-door route open.
        d.box(50,2,37,1,7,4,AIR);
        for(int z=37;z<=42;z++)for(int x=48;x<=50;x++)if((x+z)%3!=0)d.at(x,1,z,RUBBLE);
        // Derelict tram across one traffic lane, with broken glazing and wheels, not an impassable wall.
        d.box(37,1,37,6,1,10,DARK);d.box(37,2,37,6,2,10,TRAM);
        d.box(38,2,38,4,2,8,AIR);
        for(int z=38;z<46;z+=2)for(int x:new int[]{37,42})d.at(x,3,z,GLASS);
        for(int x:new int[]{36,43})for(int z:new int[]{39,44})d.at(x,1,z,WHEEL);
        d.box(37,4,37,6,1,10,COPPER);d.box(39,2,46,2,2,1,AIR);
        d.at(38,2,37,WARM);
        // One clock/broadcast landmark at the vanishing point of the boulevard.
        d.box(36,8,1,9,13,4,BRICK);d.box(35,21,0,11,1,6,COPPER);
        d.box(36,1,1,2,7,4,BRICK);d.box(43,1,1,2,7,4,BRICK);
        d.box(37,15,5,7,5,1,DARK);
        for(int y=15;y<=19;y++)d.at(40,y,6,CLOCK);
        for(int x=40;x<=43;x++)d.at(x,17,6,CLOCK);
        d.at(40,20,5,WARM);
        // Seven empty chairs hang over the closed departure lane. The intake ledger accounts for exactly seven missing people.
        d.box(31,13,11,19,1,1,TRIM);
        for(int x:new int[]{31,49})d.box(x,1,11,1,12,1,POST);
        for(int i=0;i<7;i++) {
            int x=34+i*2;
            for(int y=7;y<=12;y++)d.at(x,y,11,CHAIN);
            d.at(x,6,11,CHAIR);d.at(x,0,11,RED);
        }
        // Surroundings: uneven embankment, canal, dead trees and distant silhouettes, not an empty flat plane.
        for(int x=-28;x<=107;x++)for(int z=-28;z<=107;z++) {
            if(x>=0&&x<80&&z>=0&&z<80)continue;
            int n=Math.floorMod(x*113+z*53,23);
            int h=Math.max(0,(int)(2+2*Math.sin(x*.17)*Math.cos(z*.13)));
            for(int y=0;y<=h;y++)d.at(x,y,z,n<4?MOSS:EARTH);
            if(x>=85&&x<=90){d.box(x,0,z,1,5,1,AIR);d.at(x,0,z,WATER);}
        }
        for(int z=-12;z<105;z+=17)for(int x:new int[]{-9,99}) {
            int h=9+Math.floorMod(z,4);
            d.box(x,1,z,1,h,1,WOOD);
            for(int a=1;a<=4;a++){d.at(x+a,h-2+a/2,z,WOOD);d.at(x-a,h-4+a/2,z+1,WOOD);}
            d.at(x,h+1,z,LEAF);
        }
        for(int[] b:new int[][]{{-25,9,12,18},{-24,48,11,27},{17,-24,17,24},{52,-24,15,33},{94,5,11,23},{95,53,11,31},{15,95,16,20},{53,94,16,28}}) {
            for(int x=b[0];x<b[0]+b[2];x++)for(int z=b[1];z<b[1]+12;z++)for(int y=1;y<=b[3];y++) {
                if(x==b[0]||z==b[1]||x==b[0]+b[2]-1||z==b[1]+11||y==b[3])
                    d.at(x,y,z,y%5==2&&(x+z)%4==1?GLASS:DARK);
            }
            d.at(b[0]+3,b[3]+1,b[1]+3,RED);
            // Broken upper corners and stepped service towers vary the distant roofline.
            d.box(b[0],b[3]-4,b[1],4,5,4,AIR);
            d.box(b[0]+b[2]-5,b[3]+1,b[1]+5,4,3,4,DARK);
        }
        // Preserve navigation-critical cells, including a clear arrival/return approach.
        for(var c:CityLayout.CELLS)if(c.kind()==GATE||c.kind()==INTAKE||c.kind()==HOME||c.kind()==SIREN)d.at(c.x(),c.y(),c.z(),c.kind());
        return List.copyOf(map.values());
    }
}
