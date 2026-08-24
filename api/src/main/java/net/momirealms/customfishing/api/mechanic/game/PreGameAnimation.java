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

package net.momirealms.customfishing.api.mechanic.game;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.momirealms.customfishing.api.util.MiscUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A sprite-sheet animation played before a minigame starts. Playback blocks:
 * the minigame's clock only starts once the last frame has been shown, and any
 * input received while it plays is swallowed.
 * <p>
 * Two configuration shapes are accepted. The short one is a plain list of
 * frames:
 * <pre>
 * pre-minigame-title:
 *   - "&lt;image:stardew:fishing_bite_f1&gt;"
 *   - "&lt;image:stardew:fishing_bite_f2&gt;"
 *   - "&lt;image:stardew:fishing_bite_f3&gt;"
 * </pre>
 * and the long one spells out the timing:
 * <pre>
 * pre-minigame-title:
 *   frames: ["&lt;image:stardew:fishing_bite_f1&gt;", ...]
 *   frame-time: 3      # ticks per frame
 *   loops: 2
 *   subtitle: ""       # optional, static for the whole animation
 *   font: ""           # optional, wraps every frame in &lt;font:...&gt;
 * </pre>
 *
 * @param frames the frames, in display order
 * @param frameTime how many 50ms ticks each frame is shown for
 * @param loops how many times the whole sequence is played
 * @param subtitle a static subtitle shown underneath, or null
 * @param font a font to wrap every frame in, or null
 */
public record PreGameAnimation(List<String> frames, int frameTime, int loops, @Nullable String subtitle,
                               @Nullable String font) {

    private static final int DEFAULT_FRAME_TIME = 3;

    /**
     * Parses the animation configured under the given key.
     *
     * @param parent the minigame's section
     * @param key the key holding the animation, typically {@code pre-minigame-title}
     * @return the animation, or null when unconfigured or empty
     */
    @Nullable
    public static PreGameAnimation parse(Section parent, String key) {
        Object raw = parent.get(key);
        if (raw == null) return null;

        if (raw instanceof Section section) {
            Object rawFrames = section.get("frames");
            if (rawFrames == null) return null;
            List<String> frames = MiscUtils.getAsStringList(rawFrames);
            if (frames.isEmpty()) return null;
            int frameTime = Math.max(1, section.getInt("frame-time", DEFAULT_FRAME_TIME));
            int loops = Math.max(1, section.getInt("loops", 1));
            return new PreGameAnimation(frames, frameTime, loops,
                    section.getString("subtitle"), section.getString("font"));
        }

        List<String> frames = MiscUtils.getAsStringList(raw);
        if (frames.isEmpty()) return null;
        return new PreGameAnimation(frames, DEFAULT_FRAME_TIME, 1, null, null);
    }

    /**
     * The total number of 50ms ticks the animation occupies.
     *
     * @return the duration in ticks
     */
    public int totalTicks() {
        return frames.size() * frameTime * loops;
    }
}
