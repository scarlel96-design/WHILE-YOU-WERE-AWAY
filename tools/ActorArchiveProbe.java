import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.jar.JarFile;
/** Archive identity + unchanged pure checkpoint behavior. Not a substitute for Minecraft lifecycle QA. */
public final class ActorArchiveProbe {
    public static void main(String[] args) throws Exception {
        boolean expected=args[1].equals("modified");
        try(var jar=new JarFile(args[0]);var loader=new URLClassLoader(new java.net.URL[]{Path.of(args[0]).toUri().toURL()},ClassLoader.getPlatformClassLoader())) {
            boolean registry=jar.getEntry("io/github/whileaway/StoryActors.class")!=null;
            boolean adapter=new String(jar.getInputStream(jar.getEntry("io/github/whileaway/entity/Wayfarer.class")).readAllBytes(),java.nio.charset.StandardCharsets.ISO_8859_1).contains("io/github/whileaway/StoryActors");
            if(registry!=expected||adapter!=expected)throw new AssertionError("actor registry/adapter mismatch");
            var c=loader.loadClass("io.github.whileaway.core.EventCheckpoint");var r=c.getConstructor().newInstance();
            var restore=c.getMethod("restore",String.class,int.class,int.class,int.class);
            restore.invoke(r,"ABORTED",4,127,0);String a=c.getField("state").get(r)+"/"+c.getField("checkpoint").get(r);
            restore.invoke(r,"COMPLETED",2,127,0);String b=c.getField("state").get(r)+"/"+c.getField("checkpoint").get(r);
            if(!a.equals("ABORTED/4")||!b.equals("FAILED_RECOVERABLE/2"))throw new AssertionError("checkpoint regression");
            System.out.println("actorRegistry="+registry+" chaseAdapter="+adapter);
            System.out.println("ABORTED/4 -> "+a+"; COMPLETED/2 -> "+b);
            System.out.println("PASS archive="+args[1]+"; runtime lifecycle proof is separate");
        }
    }
}
