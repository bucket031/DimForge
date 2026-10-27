package com.example.dimensionforge;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/** Generates completely empty chunks (a void world). */
public final class VoidGenerator extends ChunkGenerator {

    @Override
    public @Nullable Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        return new Location(world, 0.5, 64, 0.5);
    }
}
