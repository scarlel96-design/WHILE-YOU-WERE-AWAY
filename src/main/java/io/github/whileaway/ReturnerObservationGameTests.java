package io.github.whileaway;

import io.github.whileaway.support.ReturnerObservationPolicy;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

/** Policy tests. These tests do not simulate a player's view in a client. */
@GameTestHolder(WhileAway.ID)
@PrefixGameTestTemplate(false)
public final class ReturnerObservationGameTests {
    private static int step(int previous, double distanceSquared, double dot, boolean los) {
        return ReturnerObservationPolicy.nextStreak(previous,
            ReturnerObservationPolicy.withinRange(distanceSquared) && ReturnerObservationPolicy.facing(dot) && los);
    }

    @GameTest(template="empty") public static void withinRangeFacingVisibleAccumulates(GameTestHelper h) {
        int streak=0;
        for(int tick=1;tick<=8;tick++) {
            streak=step(streak,100,0.8,true);
            h.assertTrue(streak==tick,"eligible exposure advances once per tick");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void exactlyEightTicksConfirms(GameTestHelper h) {
        h.assertTrue(!ReturnerObservationPolicy.confirmed(7),"seven does not confirm");
        h.assertTrue(ReturnerObservationPolicy.confirmed(8),"eight confirms");h.succeed();
    }
    @GameTest(template="empty") public static void distanceOutside32Resets(GameTestHelper h) {
        h.assertTrue(step(7,1024,1,true)==8,"exact 32-block boundary accepted");
        h.assertTrue(step(7,Math.nextUp(1024d),1,true)==0,"outside boundary resets");
        h.assertTrue(step(7,0.01,1,true)==0&&step(7,0,1,true)==0,"existing near-zero normalization guard retained");
        h.assertTrue(step(7,Double.NaN,1,true)==0,"unknown distance never confirms");h.succeed();
    }
    @GameTest(template="empty") public static void lookDotBelowThresholdResets(GameTestHelper h) {
        h.assertTrue(step(7,100,Math.nextDown(0.35),true)==0,"below threshold resets");
        h.assertTrue(step(7,100,-1,true)==0&&step(7,100,Double.NaN,true)==0,"back-facing or undefined view resets");h.succeed();
    }
    @GameTest(template="empty") public static void lookDotBoundaryAccepted(GameTestHelper h) {
        h.assertTrue(step(7,100,0.35,true)==8,"exact dot threshold accepted");h.succeed();
    }
    @GameTest(template="empty") public static void lineOfSightFalseResets(GameTestHelper h) {
        h.assertTrue(step(7,100,1,false)==0,"occlusion clears the entire streak");h.succeed();
    }
    @GameTest(template="empty") public static void singleInterruptedTickResetsEntireStreak(GameTestHelper h) {
        int streak=7;streak=step(streak,100,1,false);
        for(int tick=1;tick<=7;tick++) {
            streak=step(streak,100,1,true);
            h.assertTrue(!ReturnerObservationPolicy.confirmed(streak),"fresh streak still requires eight ticks");
        }
        h.assertTrue(ReturnerObservationPolicy.confirmed(step(streak,100,1,true)),"eighth fresh tick confirms");h.succeed();
    }
    @GameTest(template="empty") public static void intermittentVisibilityNeverAccumulates(GameTestHelper h) {
        int streak=0;
        for(int cycle=0;cycle<4;cycle++) {
            for(int tick=0;tick<7;tick++)streak=step(streak,100,1,true);
            streak=step(streak,100,1,false);
            h.assertTrue(streak==0&&!ReturnerObservationPolicy.confirmed(streak),"separate exposures never combine");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void extractionMatchesOriginalPredicate(GameTestHelper h) {
        for(double distance:new double[]{0,0.01,Math.nextUp(0.01),100,1024,Math.nextUp(1024d),Double.NaN,Double.POSITIVE_INFINITY})
            for(double dot:new double[]{-1,Math.nextDown(0.35),0.35,1,Double.NaN})for(boolean los:new boolean[]{false,true}) {
                boolean original=distance<=32*32&&distance>0.01&&dot>=0.35&&los;
                h.assertTrue((step(0,distance,dot,los)==1)==original,"original boundary semantics preserved");
            }
        h.succeed();
    }
}
