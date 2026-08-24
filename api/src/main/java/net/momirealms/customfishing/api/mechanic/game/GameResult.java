package net.momirealms.customfishing.api.mechanic.game;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public record GameResult(
        GameResultType resultType,
        @Range(from = 0, to = 100)
        @Nullable
        Integer score
) {
    public static GameResult gameFailure() {
        return new GameResult(GameResultType.GAME_FAILED, null);
    }

    public static GameResult timeout() {
        return new GameResult(GameResultType.TIMEOUT_FAILURE, null);
    }

    public static GameResult forceFailure() {
        return new GameResult(GameResultType.FORCE_FAILURE, null);
    }

    public boolean isSuccess() {
        return resultType == GameResultType.SUCCESS;
    }

    public static GameResult success() {
        return new GameResult(GameResultType.SUCCESS, null);
    }

    public static GameResult success(int score) {
        return new GameResult(GameResultType.SUCCESS, score);
    }

    public static GameResult perfect() {
        return new GameResult(GameResultType.SUCCESS, 100);
    }

    public boolean isPerfect() {
        return score != null && score == 100;
    }
}

