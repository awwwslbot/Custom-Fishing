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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * A deterministic keyframed animation of a normalized position over time, used
 * to drive how a fish swims during a minigame.
 * <p>
 * Playback is a pure function of the elapsed time, so the same track always
 * produces the same motion. Positions are normalized: {@code 0} is the top of
 * the bar and {@code 1} the bottom, which lets one track be reused across bars
 * of different heights.
 */
public class MovementTrack {

    private final Keyframe[] keyframes;
    private final double duration;
    private final boolean loop;
    private final double weight;

    private MovementTrack(Keyframe[] keyframes, double duration, boolean loop, double weight) {
        this.keyframes = keyframes;
        this.duration = duration;
        this.loop = loop;
        this.weight = weight;
    }

    /**
     * Samples the track.
     *
     * @param elapsedSeconds the time elapsed since the track started playing
     * @return the normalized position, within the range spanned by the keyframes
     */
    public double valueAt(double elapsedSeconds) {
        if (keyframes.length == 1 || duration <= 0) return keyframes[0].value();

        double t = elapsedSeconds / duration;
        if (loop) {
            t -= Math.floor(t);
        } else if (t <= 0) {
            return keyframes[0].value();
        } else if (t >= 1) {
            return keyframes[keyframes.length - 1].value();
        }

        if (t <= keyframes[0].time()) return keyframes[0].value();
        Keyframe last = keyframes[keyframes.length - 1];
        if (t >= last.time()) return last.value();

        // The keyframe whose time is the first one greater than t closes the segment.
        int high = keyframes.length - 1;
        int low = 0;
        while (low + 1 < high) {
            int mid = (low + high) >>> 1;
            if (keyframes[mid].time() <= t) low = mid; else high = mid;
        }
        Keyframe from = keyframes[low];
        Keyframe to = keyframes[high];
        double span = to.time() - from.time();
        double progress = span <= 0 ? 1 : to.easing().apply((t - from.time()) / span);
        return from.value() + (to.value() - from.value()) * progress;
    }

    /**
     * The relative weight used when a {@link MovementTrackSet} picks a track.
     *
     * @return the weight, always positive
     */
    public double weight() {
        return weight;
    }

    /**
     * Parses a track from its configuration representation.
     *
     * @param map the mapping holding {@code keyframes} and optional {@code duration}/{@code loop}/{@code weight}
     * @param defaultDuration the duration inherited from the enclosing section
     * @param defaultLoop whether the track loops, inherited from the enclosing section
     * @return the track
     */
    public static MovementTrack parse(Map<?, ?> map, double defaultDuration, boolean defaultLoop) {
        Object raw = map.get("keyframes");
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException("A movement track requires a non-empty 'keyframes' list");
        }
        List<Keyframe> parsed = new ArrayList<>(list.size());
        for (Object element : list) {
            if (!(element instanceof Map<?, ?> frame)) {
                throw new IllegalArgumentException("Each keyframe must be a mapping like { t: 0.5, y: 0.2 }, got " + element);
            }
            parsed.add(Keyframe.parse(frame));
        }
        parsed.sort(Comparator.comparingDouble(Keyframe::time));

        double duration = map.get("duration") instanceof Number n ? n.doubleValue() : defaultDuration;
        boolean loop = map.get("loop") instanceof Boolean b ? b : defaultLoop;
        double weight = map.get("weight") instanceof Number n ? n.doubleValue() : 1;
        if (weight <= 0) {
            throw new IllegalArgumentException("A movement track's weight must be positive, got " + weight);
        }
        return new MovementTrack(parsed.toArray(new Keyframe[0]), duration, loop, weight);
    }
}
