package io.github.whileaway;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

/**
 * Durable world-local persistence primitive for ReturnNetworkState.
 *
 * This class deliberately does not trigger presentation/world/NPC effects. A caller may only
 * publish effects after commit(...) returns successfully.
 *
 * The current milestone keeps this record isolated from NarrativeData until the repository's
 * save-format migration gate is explicitly advanced.
 */
public final class ReturnNetworkPersistence {
    public static final int SCHEMA = 1;
    private static final long MAX_BYTES = 1024L * 1024L;

    private final Path file;
    private ReturnNetworkState state;
    private String expectedHash;
    private boolean writable = true;

    private ReturnNetworkPersistence(Path file, ReturnNetworkState state, String expectedHash) {
        this.file = file.toAbsolutePath().normalize();
        this.state = Objects.requireNonNull(state);
        this.expectedHash = expectedHash;
    }

    public static ReturnNetworkPersistence open(Path file) {
        var normalized = file.toAbsolutePath().normalize();
        try {
            if (Files.notExists(normalized)) {
                return new ReturnNetworkPersistence(normalized, ReturnNetworkState.notStarted(), null);
            }
            if (!Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("return_network_not_regular_file");
            }
            if (Files.size(normalized) > MAX_BYTES) {
                throw new IOException("return_network_exceeds_bounded_input");
            }
            var bytes = Files.readAllBytes(normalized);
            var root = NbtIo.readCompressed(normalized, NbtAccounter.create(MAX_BYTES * 8));
            if (root == null || !root.contains("schema", Tag.TAG_INT)) {
                throw new IllegalStateException("return_network_missing_schema");
            }
            if (root.getInt("schema") != SCHEMA) {
                throw new IllegalStateException("return_network_unsupported_schema=" + root.getInt("schema"));
            }
            if (!root.contains("state", Tag.TAG_COMPOUND)) {
                throw new IllegalStateException("return_network_state_wrong_type");
            }
            var state = ReturnNetworkState.load(root.getCompound("state"));
            return new ReturnNetworkPersistence(normalized, state, StoryStorage.digest(bytes));
        } catch (IOException ex) {
            throw new UncheckedIOException("RETURN_NETWORK_LOAD_BLOCKED " + normalized, ex);
        }
    }

    public synchronized ReturnNetworkState current() {
        return state;
    }

    public synchronized boolean writable() {
        return writable;
    }

    /**
     * Compare-and-swap durable commit.
     *
     * The expected state must match the currently committed state, the candidate must round-trip
     * through the canonical codec, and the backing file must still have the exact bytes observed
     * by this instance. No presentation/world/NPC effect belongs in this method.
     */
    public synchronized ReturnNetworkState commit(ReturnNetworkState expected, ReturnNetworkState draft) {
        if (!writable) {
            throw new IllegalStateException("RETURN_NETWORK_WRITE_BLOCKED previous_guard_failure");
        }
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(draft, "draft");
        if (!state.save().equals(expected.save())) {
            throw new IllegalStateException("RETURN_NETWORK_STALE_DRAFT");
        }

        // Canonical semantic validation without trusting caller-owned object identity.
        var validated = ReturnNetworkState.load(draft.save());

        try {
            verifySourceUnchanged();
            var parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);

            var root = new CompoundTag();
            root.putInt("schema", SCHEMA);
            root.put("state", validated.save());

            net.neoforged.neoforge.common.IOUtilities.writeNbtCompressed(root, file);
            var bytes = Files.readAllBytes(file);
            expectedHash = StoryStorage.digest(bytes);
            state = validated;
            return state;
        } catch (IOException ex) {
            writable = false;
            throw new UncheckedIOException("RETURN_NETWORK_WRITE_BLOCKED original_preserved", ex);
        }
    }

    private void verifySourceUnchanged() throws IOException {
        if (expectedHash == null) {
            if (!Files.notExists(file)) throw new IOException("return_network_source_changed_since_load");
            return;
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("return_network_source_changed_since_load");
        }
        var actual = StoryStorage.digest(Files.readAllBytes(file));
        if (!expectedHash.equals(actual)) {
            throw new IOException("return_network_source_changed_since_load");
        }
    }
}
