import java.util.jar.JarFile;
import java.nio.charset.StandardCharsets;

/** Packaging regression only; the 22 actual client processes remain separate evidence. */
public final class WayfarerArchiveProbe {
    public static void main(String[] args) throws Exception {
        boolean expected=args[1].equals("modified");
        PresenceArchiveProbe.main(new String[]{args[0],"present"});
        try(var jar=new JarFile(args[0])) {
            boolean processGate=jar.getEntry("io/github/whileaway/client/ClientTestEnvironment.class")!=null;
            String exceptions=new String(jar.getInputStream(jar.getEntry("io/github/whileaway/client/ActorExceptionsSmoke.class")).readAllBytes(),StandardCharsets.ISO_8859_1);
            String crash=new String(jar.getInputStream(jar.getEntry("io/github/whileaway/client/ActorSmoke.class")).readAllBytes(),StandardCharsets.ISO_8859_1);
            boolean chunks=exceptions.contains("snapshot Pos and registered position disagree");
            boolean combat=crash.contains("playerAttack");
            if(processGate!=expected||chunks!=expected||combat!=expected)throw new AssertionError("Fixture revision mismatch");
            System.out.println("PASS archive="+args[1]+" actorSnapshotChunk="+chunks+" combatD="+combat+" processGate="+processGate);
        }
    }
}
