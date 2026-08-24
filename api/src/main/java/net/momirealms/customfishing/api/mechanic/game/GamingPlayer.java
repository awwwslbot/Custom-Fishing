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

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a gaming player.
 */
public interface GamingPlayer {

    /**
     * Checks if the gaming player is valid.
     *
     * @return {@code true} if the gaming player is valid, {@code false} otherwise
     */
    boolean isValid();

    /**
     * Forces the game result
     *
     * @param result result, true for success, false for failure
     * @deprecated Use {@link #forceGameResult(GameResult)} instead.
     */
    @Deprecated
    void forceGameResult(boolean result);

    /**
     * Forces the game result
     *
     * @param result the game result, or null to clear the forced game result.
     */
    void forceGameResult(@Nullable GameResult result);

    /**
     * Gets the current game settings
     *
     * @return game settings
     */
    GameSetting settings();

    /**
     * Destroys the gaming player, performing any necessary cleanup
     */
    void destroy();

    /**
     * Cancels the game
     */
    void cancel();

    /**
     * Gets the game result
     *
     * @return the game result. or null indicating the game is not finished yet.
     */
    @Nullable GameResult result();

    /**
     * Handles left-click actions.
     *
     * @return true if cancel the event, false otherwise.
     */
    boolean handleLeftClick();

    /**
     * Handles right-click actions.
     */
    void handleRightClick();

    /**
     * Handles the swap hand action during the game.
     */
    void handleSwapHand();

    /**
     * Handles chat input during the game.
     *
     * @param message the chat message.
     * @return true if cancel the event, false otherwise.
     */
    boolean handleChat(String message);

    /**
     * Handles the jump action during the game.
     *
     * @return true if cancel the event, false otherwise.
     */
    boolean handleJump();

    /**
     * Handles the sneak action during the game.
     *
     * @return true if cancel the event, false otherwise.
     */
    boolean handleSneak();

    /**
     * Gets the player associated with the gaming player.
     *
     * @return the player.
     */
    Player getPlayer();

    /**
     * Ends the game
     */
    void endGame();
}
