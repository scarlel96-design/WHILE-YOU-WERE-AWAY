import java.net.URLClassLoader;
import java.nio.file.Path;
/** Isolated archive behavior probe: no Minecraft process and no world files. */
public final class CheckpointProbe {
  public static void main(String[] args) throws Exception {
    try(var loader=new URLClassLoader(new java.net.URL[]{Path.of(args[0]).toUri().toURL()},ClassLoader.getPlatformClassLoader())) {
      var c=loader.loadClass("io.github.whileaway.core.EventCheckpoint");var r=c.getConstructor().newInstance();
      var restore=c.getMethod("restore",String.class,int.class,int.class,int.class);
      var state=c.getField("state");var cp=c.getField("checkpoint");
      restore.invoke(r,"ABORTED",4,127,0);
      var a="ABORTED/4 -> "+state.get(r)+"/"+cp.get(r);
      restore.invoke(r,"COMPLETED",2,127,0);
      var b="COMPLETED/2 -> "+state.get(r)+"/"+cp.get(r);
      String expected=args[1].equals("baseline")?"ABORTED/4 -> COMPLETED/4|COMPLETED/2 -> COMPLETED/4":"ABORTED/4 -> ABORTED/4|COMPLETED/2 -> FAILED_RECOVERABLE/2";
      if(!(a+"|"+b).equals(expected))throw new AssertionError(a+"|"+b);
      System.out.println(a);System.out.println(b);System.out.println("PASS archive behavior="+args[1]);
    }
  }
}
