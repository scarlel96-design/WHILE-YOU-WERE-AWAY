package io.github.whileaway.core;

/** Runs with plain Java 21; no Minecraft process, network or external test library. */
public final class CoreTests {
    private static int assertions;
    private static void check(boolean ok,String message) { assertions++; if(!ok)throw new AssertionError(message); }
    private static Director.Context ctx(long tick) { return new Director.Context(tick,true,true,true,false,false,false); }
    public static void main(String[] args) {
        var p=Progress.empty();
        check(p.stage()==0,"new world quiet");
        check(Director.choose(p,ctx(Long.MAX_VALUE/2))==Director.Cue.NONE,"time never unlocks horror");
        p=p.discover(Clue.STATION);
        check(p.stage()==1,"station warning");
        check(Director.choose(p,ctx(0))==Director.Cue.DISTANT_KNOCK,"first warning");
        check(!Director.canRestore(p),"cannot skip reading");
        p=p.discover(Clue.WARNING_READ);
        check(p.stage()==2,"reading gives contact clue");
        check(Director.canRestore(p),"restore prerequisites");
        check(Director.choose(p,ctx(0))==Director.Cue.RETURNING_STEPS,"second warning");
        p=p.discover(Clue.SIGNAL_RESTORED);
        check(p.stage()==3,"explicit repair unlocks manifestation");
        check(Director.choose(p,ctx(0))==Director.Cue.MANIFEST,"manifest unlocked");
        for(int mask=0;mask<32;mask++) {
            var q=new Progress(mask,100,0,false);
            for(Clue clue:Clue.values()) {
                var added=q.discover(clue);
                check(added.discover(clue).equals(added),"clue idempotence");
                check(added.stage()>=q.stage(),"monotonic discovery");
                check(added.distinctClues()>=q.distinctClues(),"unique evidence count");
            }
            check(Director.choose(q,ctx(99))==Director.Cue.NONE,"cooldown before boundary");
            check(Director.choose(q.complete(),ctx(1000))==Director.Cue.NONE,"completed story not repeated");
            for(int gate=0;gate<7;gate++) {
                var blocked=new Director.Context(1000,gate!=0,gate!=1,gate!=2,gate==3,gate==4,gate==5);
                if(gate<6)check(Director.choose(q,blocked)==Director.Cue.NONE,"context gate "+gate);
            }
        }
        check(p.eventPlayed(100,200).nextEventTick()==300,"cooldown arithmetic");
        check(Director.choose(p.eventPlayed(100,200),ctx(299))==Director.Cue.NONE,"repetition blocked");
        check(Director.choose(p.eventPlayed(100,200),ctx(300))==Director.Cue.MANIFEST,"boundary exact");
        check(p.defer(800).defer(100).nextEventTick()==800,"grace never shortened");
        check(new Progress(-1,-3,-2,false).distinctClues()==5,"unknown bits stripped");
        check(new Progress(0,-3,-2,false).eventsPlayed()==0,"corrupt counter normalized");
        check(Progress.empty().discover(Clue.PERSONAL_READ).stage()==0,"book alone no station");
        check(!ThreatPolicy.mayManifest(2,true),"two clues insufficient");
        check(!ThreatPolicy.mayManifest(5,false),"repair required");
        check(!ThreatPolicy.mayManifest(-1,true),"negative count denied");
        check(ThreatPolicy.mayManifest(3,true),"policy modified branch");
        check(Progress.empty().stage()==0,"other player untouched");
        for(int t=-10;t<260;t++) {
            var frame=SceneTimeline.at(t);
            check(frame.pressure()>=0&&frame.pressure()<=.6f,"bounded scene pressure");
            check(frame.bars()>=0&&frame.bars()<=.035f,"bounded overlay");
            check(frame.finished()==(t<0||t>=240),"scene termination");
        }
        check(SceneTimeline.at(0).pressure()==0,"no sudden fade onset");
        check(SceneTimeline.at(239).pressure()<.001,"smooth release");
        check(SceneTimeline.at(240).caption()==0,"no stuck caption");
        check(!CityAccess.canEnter(Progress.empty()),"city locked initially");
        var cityReady=p.discover(Clue.PERSONAL_READ).complete();
        check(CityAccess.canEnter(cityReady),"encounter and address unlock city");
        check(!CityAccess.canEnter(new Progress(cityReady.clues(),0,0,false)),"encounter required");
        check(!CityAccess.canEnter(new Progress(cityReady.clues()&~Clue.PERSONAL_READ.bit(),0,0,true)),"address required");
        for(int bits=0;bits<8;bits++)for(int b:new int[]{1,2,4}) {
            check(CityAccess.read(CityAccess.read(bits,b),b)==CityAccess.read(bits,b),"city records idempotent");
            check((CityAccess.read(bits,b)&bits)==bits,"city records monotonic");
        }
        var positions=new java.util.HashSet<String>();
        var ground=new java.util.HashMap<String,CityLayout.Kind>();
        for(var c:CityLayout.CELLS) {
            check(positions.add(c.x()+","+c.y()+","+c.z()),"city cell unique");
            check(c.x()>=0&&c.x()<80&&c.z()>=0&&c.z()<80&&c.y()>=0&&c.y()<=21,"city bounds");
            if(c.y()==1||c.y()==2)ground.put(c.x()+","+c.z(),c.kind());
        }
        // Walkable ground connectivity from arrival to the three reading positions and the return light.
        var seen=new java.util.HashSet<String>();var queue=new java.util.ArrayDeque<int[]>();queue.add(new int[]{40,68});
        while(!queue.isEmpty()) {
            var at=queue.remove();String key=at[0]+","+at[1];
            if(at[0]<1||at[0]>78||at[1]<1||at[1]>78||ground.containsKey(key)||!seen.add(key))continue;
            for(var offset:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})queue.add(new int[]{at[0]+offset[0],at[1]+offset[1]});
        }
        for(var target:new int[][]{{18,46},{60,68},{40,8},{40,71}})check(seen.contains(target[0]+","+target[1]),"city record route connected");
        check(CityLayout.BUILDINGS.size()==6,"six district buildings");
        check(CityLayout.CELLS.size()==20988,"legacy plan remains exactly sized");
        var artPositions=new java.util.HashSet<String>();var artGround=new java.util.HashMap<String,CityLayout.Kind>();
        for(var c:CityArtLayout.CELLS) {
            check(artPositions.add(c.x()+","+c.y()+","+c.z()),"art plan unique cells");
            check(c.x()>=-28&&c.x()<=107&&c.z()>=-28&&c.z()<=107&&c.y()>=0&&c.y()<=36,"art extent bounded");
            if(c.y()==1||c.y()==2) {
                if(c.kind()!=CityLayout.Kind.AIR)artGround.put(c.x()+","+c.z(),c.kind());
            }
        }
        seen.clear();queue.clear();queue.add(new int[]{40,68});
        while(!queue.isEmpty()) {
            var at=queue.remove();String key=at[0]+","+at[1];
            if(at[0]<1||at[0]>78||at[1]<1||at[1]>78||artGround.containsKey(key)||!seen.add(key))continue;
            for(var offset:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})queue.add(new int[]{at[0]+offset[0],at[1]+offset[1]});
        }
        for(var target:new int[][]{{18,46},{60,68},{40,8},{40,71}})check(seen.contains(target[0]+","+target[1]),"art route still connected");
        check(CityArtLayout.CELLS.stream().filter(c->c.kind()==CityLayout.Kind.CHAIR&&c.y()==6&&c.z()==11).count()==7,"seven suspended chairs match ledger");
        check(CityArtLayout.CELLS.stream().anyMatch(c->c.x()<0&&c.y()>20),"surrounding skyline exists");
        System.out.println("PASS core assertions="+assertions);
    }
}
