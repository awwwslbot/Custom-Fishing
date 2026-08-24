package net.momirealms.customfishing.api.mechanic.game;

public record GameResult(
        GameResultType resultType
) {
    public boolean isSuccess() {
        return resultType == GameResultType.SUCCESS || resultType == GameResultType.PERFECT_SUCCESS;
    }

    public boolean isPerfectSuccess() {
        return resultType == GameResultType.PERFECT_SUCCESS;
    }
}

