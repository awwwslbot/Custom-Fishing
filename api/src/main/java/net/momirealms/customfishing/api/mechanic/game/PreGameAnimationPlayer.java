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

import net.momirealms.customfishing.api.mechanic.fishing.CustomFishingHook;
import net.momirealms.customfishing.common.helper.AdventureHelper;
import net.momirealms.sparrow.heart.SparrowHeart;

import java.util.function.Supplier;

/**
 * Plays a {@link PreGameAnimation} and only then hands over to the real
 * minigame. It is a {@link GamingPlayer} in its own right so that the hook
 * counts as "playing" for the whole duration: reeling in or clicking cannot
 * cut the animation short, and the minigame's own countdown starts from
 * scratch once the last frame has been shown.
 */
public class PreGameAnimationPlayer extends AbstractGamingPlayer {

    private final PreGameAnimation animation;
    private final Supplier<AbstractGamingPlayer> nextGame;
    private final int totalTicks;
    private int tick;

    /**
     * @param hook the fishing hook this animation belongs to
     * @param settings the settings the upcoming minigame will run with
     * @param animation the frames to play
     * @param nextGame creates the real minigame, invoked once the animation ends
     */
    public PreGameAnimationPlayer(CustomFishingHook hook, GameSetting settings,
                                  PreGameAnimation animation, Supplier<AbstractGamingPlayer> nextGame) {
        super(hook, settings);
        this.animation = animation;
        this.nextGame = nextGame;
        this.totalTicks = animation.totalTicks();
    }

    /**
     * Skips {@link AbstractGamingPlayer#timeOutCheck()}: the animation runs
     * outside of the minigame's time budget.
     */
    @Override
    public void run() {
        tick();
    }

    @Override
    protected void tick() {
        if (!isValid()) return;
        if (tick >= totalTicks) {
            handOver();
            return;
        }
        showFrame(animation.frames().get((tick / animation.frameTime()) % animation.frames().size()));
        tick++;
    }

    private void showFrame(String frame) {
        String title = animation.font() == null
                ? frame
                : AdventureHelper.surroundWithMiniMessageFont(frame, animation.font());
        String subtitle = animation.subtitle();
        SparrowHeart.getInstance().sendTitle(
                getPlayer(),
                AdventureHelper.miniMessageToJson(title),
                subtitle == null ? null : AdventureHelper.miniMessageToJson(subtitle),
                0, 20, 0
        );
    }

    private void handOver() {
        // destroy() first: it flips this player invalid and cancels our ticker, so a
        // concurrent tick cannot spawn the real game twice.
        destroy();
        hook.setGamingPlayer(nextGame.get());
    }

    /**
     * Swallowed: input during the animation must not end the cast. The animation
     * always finishes on its own timer.
     */
    @Override
    public void endGame() {
    }

    @Override
    public void handleRightClick() {
    }

    @Override
    public boolean handleLeftClick() {
        return true;
    }

    @Override
    public boolean handleJump() {
        return true;
    }

    @Override
    public boolean handleSneak() {
        return true;
    }

    @Override
    public void handleSwapHand() {
    }
}
