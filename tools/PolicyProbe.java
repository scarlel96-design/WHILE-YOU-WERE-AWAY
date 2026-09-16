import io.github.whileaway.core.ThreatPolicy;
public final class PolicyProbe {
    public static void main(String[] args) {
        boolean actual = ThreatPolicy.mayManifest(Integer.parseInt(args[0]), Boolean.parseBoolean(args[1]));
        System.out.println("mayManifest=" + actual);
        if (actual != Boolean.parseBoolean(args[2])) System.exit(1);
    }
}
