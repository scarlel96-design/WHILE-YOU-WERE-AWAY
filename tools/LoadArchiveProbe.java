import java.util.jar.JarFile;
import java.nio.charset.StandardCharsets;
public final class LoadArchiveProbe {
    public static void main(String[] args)throws Exception {
        PresenceArchiveProbe.main(new String[]{args[0],"present"});
        boolean expected=args[1].equals("modified");
        try(var jar=new JarFile(args[0])) {
            boolean loader=jar.getEntry("io/github/whileaway/StoryStorage.class")!=null;
            String data=new String(jar.getInputStream(jar.getEntry("io/github/whileaway/NarrativeData.class")).readAllBytes(),StandardCharsets.ISO_8859_1);
            boolean guard=data.contains("STORY_WRITE_BLOCKED");
            if(loader!=expected||guard!=expected)throw new AssertionError("Loader revision mismatch");
            System.out.println("PASS archive="+args[1]+" explicitLoader="+loader+" writeGuard="+guard);
        }
    }
}
