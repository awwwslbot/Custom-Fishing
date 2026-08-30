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

import java.util.Map;

/**
 * A single point on a {@link MovementTrack}.
 *
 * @param time the normalized time within the track, in {@code [0,1]}
 * @param value the normalized position, where {@code 0} is the top of the bar and {@code 1} the bottom
 * @param easing the curve used to reach this keyframe from the previous one
 */
public record Keyframe(double time, double value, Easing easing) {

    /**
     * Parses a keyframe from its configuration representation, e.g.
     * {@code { t: 0.35, y: 0.15, ease: ease-in-out }}.
     *
     * @param map the mapping holding {@code t}, {@code y} and an optional {@code ease}
     * @return the keyframe
     */
    public static Keyframe parse(Map<?, ?> map) {
        double time = number(map, "t", "time");
        double value = number(map, "y", "value");
        if (time < 0 || time > 1) {
            throw new IllegalArgumentException("Keyframe time must be within [0,1], got " + time);
        }
        Object ease = map.containsKey("ease") ? map.get("ease") : map.get("easing");
        return new Keyframe(time, value, Easing.parse(ease));
    }

    private static double number(Map<?, ?> map, String key, String alias) {
        Object o = map.containsKey(key) ? map.get(key) : map.get(alias);
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
                // fall through to the shared error below
            }
        }
        throw new IllegalArgumentException("Keyframe requires a numeric '" + key + "' (or '" + alias + "'), got " + o);
    }
}
