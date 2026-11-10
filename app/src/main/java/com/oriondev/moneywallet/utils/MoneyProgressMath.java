package com.oriondev.moneywallet.utils;

/**
 * Overflow-safe arithmetic for balances and progress stored as minor currency units.
 */
public final class MoneyProgressMath {

    private MoneyProgressMath() {
    }

    public static long saturatedAdd(long first, long second) {
        if (second > 0L && first > Long.MAX_VALUE - second) return Long.MAX_VALUE;
        if (second < 0L && first < Long.MIN_VALUE - second) return Long.MIN_VALUE;
        return first + second;
    }

    public static long saturatedSubtract(long first, long second) {
        if (second < 0L && first > Long.MAX_VALUE + second) return Long.MAX_VALUE;
        if (second > 0L && first < Long.MIN_VALUE + second) return Long.MIN_VALUE;
        return first - second;
    }

    public static long safeAbs(long value) {
        return value == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(value);
    }

    public static long remaining(long target, long progress) {
        return Math.max(0L, saturatedSubtract(safeAbs(target), safeAbs(progress)));
    }

    public static long amountNeeded(long target, long current) {
        return Math.max(0L, saturatedSubtract(target, current));
    }
}
