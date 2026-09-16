import java.util.jar.JarFile;
import java.nio.charset.StandardCharsets;
public final class CanonicalArchiveProbe {
    public static void main(String[] args)throws Exception {
        PresenceArchiveProbe.main(new String[]{args[0],"present"});
        boolean expected=args[1].equals("modified");
        try(var jar=new JarFile(args[0])) {
            boolean loader=jar.getEntry("io/github/whileaway/StoryStorage.class")!=null;
            String data=new String(jar.getInputStream(jar.getEntry("io/github/whileaway/NarrativeData.class")).readAllBytes(),StandardCharsets.ISO_8859_1);
            boolean guard=data.contains("STORY_WRITE_BLOCKED");
            boolean canonical=jar.getEntry("io/github/whileaway/CanonicalRecovery.class")!=null;
            String actors=new String(jar.getInputStream(jar.getEntry("io/github/whileaway/StoryActors.class")).readAllBytes(),StandardCharsets.ISO_8859_1);
            boolean boundaries=actors.contains("GENERATION_6_RUNTIME_RESTORED");
            if(!loader||!guard||canonical!=expected||boundaries!=expected)throw new AssertionError("Artifact revision mismatch");
            System.out.println("PASS archive="+args[1]+" explicitLoader=true writeGuard=true canonicalRecovery="+canonical+" generationBoundaries="+boundaries);
        }
    }
}
