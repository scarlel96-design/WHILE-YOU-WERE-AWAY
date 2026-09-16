import java.util.jar.JarFile;
import java.nio.charset.StandardCharsets;
public final class NpcArchiveProbe {
 public static void main(String[] args)throws Exception {
  PresenceArchiveProbe.main(new String[]{args[0],"present"});
  boolean expected=args[1].equals("modified");
  try(var jar=new JarFile(args[0])) {
   boolean npc=jar.getEntry("io/github/whileaway/entity/StoryNpc.class")!=null;
   boolean canonical=jar.getEntry("io/github/whileaway/CanonicalRecovery.class")!=null;
   String data=new String(jar.getInputStream(jar.getEntry("io/github/whileaway/NarrativeData.class")).readAllBytes(),StandardCharsets.ISO_8859_1);
   if(npc!=expected||!canonical||!data.contains("STORY_WRITE_BLOCKED"))throw new AssertionError("artifact contract");
   System.out.println("PASS archive="+args[1]+" canonicalRecovery=true writeGuard=true campaignNpc="+npc);
  }
 }
}
