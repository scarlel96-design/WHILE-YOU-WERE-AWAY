package io.github.whileaway;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static io.github.whileaway.ReturnNetworkState.*;

/** Disk-level contract tests for the Return Network persistence primitive. */
@GameTestHolder(WhileAway.ID)
@PrefixGameTestTemplate(false)
public final class ReturnNetworkPersistenceGameTests {
    private static ReturnNetworkState discovered() {
        return notStarted().discover(
            new Prerequisite(4, true, true, true),
            UUID.fromString("12345678-1234-1234-1234-123456789012"),
            new Location("rail_relay", "whileaway:quiet_city", 42)
        );
    }

    private static Path tempFile(String prefix) throws Exception {
        var root = Path.of(System.getProperty("whileaway.testEvidence", "../evidence/lifecycle"), "return-network-persistence");
        Files.createDirectories(root);
        var dir = Files.createTempDirectory(root, prefix);
        return dir.resolve("return-network.dat");
    }

    @GameTest(template = "empty")
    public static void durableCommitSurvivesFreshReload(GameTestHelper h) throws Exception {
        var file = tempFile("reload-");
        var store = ReturnNetworkPersistence.open(file);
        var start = store.current();
        var committed = store.commit(start, discovered());

        h.assertTrue(Files.exists(file), "commit created durable file");
        var fresh = ReturnNetworkPersistence.open(file);
        h.assertTrue(
            fresh.current().save().equals(committed.save()),
            "fresh persistence object reloads exact committed state"
        );
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void staleDraftCannotOverwriteNewerCommit(GameTestHelper h) throws Exception {
        var file = tempFile("stale-");
        var store = ReturnNetworkPersistence.open(file);
        var staleExpected = store.current();
        var committed = store.commit(staleExpected, discovered());

        boolean rejected = false;
        try {
            store.commit(staleExpected, discovered());
        } catch (IllegalStateException expected) {
            rejected = expected.getMessage().contains("STALE_DRAFT");
        }

        var fresh = ReturnNetworkPersistence.open(file);
        h.assertTrue(rejected, "stale expected state is rejected");
        h.assertTrue(fresh.current().save().equals(committed.save()), "stale submit cannot alter durable state");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void externalFileChangePoisonsWriter(GameTestHelper h) throws Exception {
        var file = tempFile("external-");
        var first = ReturnNetworkPersistence.open(file);
        var other = ReturnNetworkPersistence.open(file);

        first.commit(first.current(), discovered());

        boolean rejected = false;
        try {
            other.commit(other.current(), discovered());
        } catch (UncheckedIOException expected) {
            rejected = expected.getMessage().contains("WRITE_BLOCKED");
        }

        h.assertTrue(rejected, "external write after open is rejected");
        h.assertTrue(!other.writable(), "failed source guard poisons further writes");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void malformedRecordFailsClosed(GameTestHelper h) throws Exception {
        var file = tempFile("malformed-");
        var root = new CompoundTag();
        root.putInt("schema", ReturnNetworkPersistence.SCHEMA);
        root.putString("state", "not-a-compound");
        NbtIo.writeCompressed(root, file);

        boolean rejected = false;
        try {
            ReturnNetworkPersistence.open(file);
        } catch (IllegalStateException expected) {
            rejected = expected.getMessage().contains("wrong_type");
        }

        h.assertTrue(rejected, "malformed persisted state is rejected instead of defaulted");
        h.succeed();
    }
}
