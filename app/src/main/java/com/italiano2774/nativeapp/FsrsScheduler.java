package com.italiano2774.nativeapp;

/**
 * Lightweight FSRS-inspired scheduler.
 * It keeps a per-card stability (days) and difficulty (1-10), estimates
 * retrievability, and schedules the next review for the desired retention.
 * The formulas are intentionally compact so the app stays fully offline.
 */
public final class FsrsScheduler {
    public static final double DEFAULT_RETENTION = 0.90;
    private FsrsScheduler() {}

    public static final class Result {
        public double stability;
        public double difficulty;
        public double retrievability;
        public int intervalDays;
    }

    public static double retrievability(double stability, int elapsedDays) {
        if (stability <= 0) return 0.0;
        int days = Math.max(0, elapsedDays);
        return clamp(Math.pow(0.90, days / stability), 0.0, 1.0);
    }

    public static Result schedule(double oldStability, double oldDifficulty, int rating,
                                  int elapsedDays, double desiredRetention) {
        int q = Math.max(1, Math.min(4, rating)); // 1 again, 2 hard, 3 good, 4 easy
        double target = clamp(desiredRetention, 0.80, 0.97);
        double d = oldDifficulty > 0 ? oldDifficulty : initialDifficulty(q);
        double s = oldStability > 0 ? oldStability : initialStability(q);
        double r = retrievability(s, elapsedDays);

        // Difficulty moves slowly and regresses toward the middle instead of drifting forever.
        d = d - 0.45 * (q - 3);
        d = 0.92 * d + 0.08 * 5.5;
        d = clamp(d, 1.0, 10.0);

        double nextS;
        if (q == 1) {
            // Lapse: keep part of the old memory, but bring the card back quickly.
            nextS = Math.max(0.45, s * (0.18 + 0.045 * (11.0 - d)) * (0.75 + 0.25 * r));
        } else {
            double hardFactor = q == 2 ? 0.62 : 1.0;
            double easyBonus = q == 4 ? 1.55 : 1.0;
            double growth = Math.exp((11.0 - d) / 5.2)
                    * Math.pow(Math.max(0.5, s), -0.18)
                    * (Math.exp((1.0 - r) * 2.35) - 1.0)
                    * hardFactor * easyBonus;
            nextS = s * (1.0 + Math.max(0.08, growth));
            if (q == 4) nextS += 1.0;
        }
        nextS = clamp(nextS, 0.45, 3650.0);

        // With R(t)=0.9^(t/S), solve t for the requested retention.
        double interval = nextS * Math.log(target) / Math.log(0.90);
        int days = Math.max(q == 1 ? 0 : 1, (int)Math.round(interval));
        days = Math.min(days, 3650);

        Result out = new Result();
        out.stability = nextS;
        out.difficulty = d;
        out.retrievability = r;
        out.intervalDays = days;
        return out;
    }

    private static double initialStability(int q) {
        switch (q) {
            case 1: return 0.45;
            case 2: return 1.0;
            case 4: return 5.5;
            default: return 2.6;
        }
    }

    private static double initialDifficulty(int q) {
        return clamp(6.2 - 0.7 * (q - 2), 1.0, 10.0);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
