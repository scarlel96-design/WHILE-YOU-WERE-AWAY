import java.util.jar.JarFile;
import java.nio.charset.StandardCharsets;
public final class NpcWorldArchiveProbe {
 public static void main(String[] args)throws Exception {
  PresenceArchiveProbe.main(new String[]{args[0],"present"});
  boolean expected=args[1].equals("modified");
  try(var jar=new JarFile(args[0])) {
   String npc=new String(jar.getInputStream(jar.getEntry("io/github/whileaway/NpcEvents.class")).readAllBytes(),StandardCharsets.ISO_8859_1);
   String data=new String(jar.getInputStream(jar.getEntry("io/github/whileaway/NarrativeData.class")).readAllBytes(),StandardCharsets.ISO_8859_1);
   if(!npc.contains("hasWitness")||(jar.getEntry("io/github/whileaway/client/NpcWorldSmoke.class")!=null)!=expected||!data.contains("STORY_WRITE_BLOCKED")||jar.getEntry("io/github/whileaway/CanonicalRecovery.class")==null)throw new AssertionError("artifact contract");
   if((jar.getEntry("io/github/whileaway/client/NpcCopySmoke.class")!=null)!=expected)throw new AssertionError("copy harness contract");
   System.out.println("PASS archive="+args[1]+" canonicalRecovery=true writeGuard=true campaignNpc=true witnessGate=true worldHarness="+expected+" copyHarness="+expected);
  }
 }
}
