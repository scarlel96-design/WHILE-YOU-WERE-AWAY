import java.net.URLClassLoader;
import java.nio.file.Path;

/** Tests the packaged pure policy without loading a Minecraft runtime. */
public final class PresenceArchiveProbe {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: PresenceArchiveProbe <jar> <present|absent>");
        try (var loader = new URLClassLoader(new java.net.URL[]{Path.of(args[0]).toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Class<?> policy;
            try { policy = loader.loadClass("io.github.whileaway.core.ActorPresencePolicy"); }
            catch (ClassNotFoundException ex) {
                if (!args[1].equals("absent")) throw ex;
                System.out.println("PASS archive policy=ABSENT (baseline/rollback)");
                return;
            }
            if (!args[1].equals("present")) throw new AssertionError("Unexpected modified policy in baseline archive");
            var method = policy.getMethod("canRun", boolean.class, boolean.class, boolean.class, boolean.class,
                boolean.class, double.class, double.class);
            Object[][] cases = {
                {false,false,false,false,true,Double.NaN,42d,true},
                {true,true,true,true,true,1764d,42d,true},
                {true,true,true,true,true,Math.nextUp(1764d),42d,false},
                {true,true,false,true,true,0d,42d,false},
                {true,true,true,false,true,0d,42d,false},
                {true,false,true,true,true,0d,42d,false},
                {true,true,true,true,true,Double.POSITIVE_INFINITY,Double.MAX_VALUE,false},
                {true,true,true,true,false,Double.NaN,42d,true}
            };
            for (Object[] row : cases) {
                Object actual = method.invoke(null, java.util.Arrays.copyOf(row, 7));
                if (!actual.equals(row[7])) throw new AssertionError("Packaged policy mismatch");
            }
            System.out.println("PASS archive policy=PRESENT checks=8");
        }
    }
}
