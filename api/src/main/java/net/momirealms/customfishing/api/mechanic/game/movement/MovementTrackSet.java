/*
 *  Copyright (C) <2024> <XiaoMoMi>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.customfishing.api.mechanic.game.movement;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The set of swim patterns a single minigame may use. One track is drawn at the
 * start of each attempt, so a minigame can offer several distinct behaviours
 * (a lazy cruise, a darting sprint, ...) while each individual attempt stays
 * fully deterministic.
 * <p>
 * Configuration, as read from a minigame entry:
 * <pre>
 * fish-movement:
 *   loop: true
 *   duration: 6.0
 *   tracks:
 *     - weight: 1
 *       keyframes:
 *         - { t: 0.0, y: 0.5 }
 *         - { t: 0.5, y: 0.1, ease: ease-in-out }
 *         - { t: 1.0, y: 0.5, ease: ease-in-out }
 * </pre>
 * A single unweighted track may also be written inline by putting
 * {@code keyframes} directly under {@code fish-movement}.
 */
public class MovementTrackSet {

    private static final double DEFAULT_DURATION = 6;

    private final MovementTrack[] tracks;
    private final double totalWeight;

    private MovementTrackSet(MovementTrack[] tracks) {
        this.tracks = tracks;
        double total = 0;
        for (MovementTrack track : tracks) total += track.weight();
        this.totalWeight = total;
    }

    /**
     * Draws one track at random, honouring the configured weights.
     *
     * @return the track to play for this attempt
     */
    public MovementTrack pick() {
        if (tracks.length == 1) return tracks[0];
        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        for (MovementTrack track : tracks) {
            roll -= track.weight();
            if (roll < 0) return track;
        }
        return tracks[tracks.length - 1];
    }

    /**
     * Parses a track set from a minigame's {@code fish-movement} section.
     *
     * @param section the section, may be null
     * @return the track set, or null when the section is absent or empty
     */
    @Nullable
    public static MovementTrackSet parse(@Nullable Section section) {
        if (section == null) return null;

        double duration = section.getDouble("duration", DEFAULT_DURATION);
        boolean loop = section.getBoolean("loop", true);

        List<MovementTrack> tracks = new ArrayList<>();
        Object raw = section.get("tracks");
        if (raw instanceof List<?> list) {
            for (Object element : list) {
                Map<?, ?> map = element instanceof Section inner
                        ? inner.getStringRouteMappedValues(false)
                        : element instanceof Map<?, ?> m ? m : null;
                if (map == null) {
                    throw new IllegalArgumentException("Each entry of 'tracks' must be a mapping, got " + element);
                }
                tracks.add(MovementTrack.parse(map, duration, loop));
            }
        } else if (section.contains("keyframes")) {
            tracks.add(MovementTrack.parse(section.getStringRouteMappedValues(false), duration, loop));
        }

        return tracks.isEmpty() ? null : new MovementTrackSet(tracks.toArray(new MovementTrack[0]));
    }
}
