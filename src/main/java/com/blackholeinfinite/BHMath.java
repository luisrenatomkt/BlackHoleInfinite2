package com.blackholeinfinite;

/** Matematica numericamente estavel (sem NaN/Infinity). */
public class BHMath {
    /** Limite do tipo double, nao um limite de design (~1.8e298). */
    public static final double MAX_VALUE = Double.MAX_VALUE / 1.0e10;

    public static double finite(double v, double fallback) {
        return (Double.isNaN(v) || Double.isInfinite(v)) ? fallback : v;
    }

    public static double safePow(double base, double exp) {
        if (!(base > 0)) return 0;
        double r = Math.exp(exp * Math.log(base));
        if (Double.isNaN(r)) return 0;
        if (Double.isInfinite(r) || r > MAX_VALUE) return MAX_VALUE;
        return r;
    }

    public static double clampMass(double m) {
        m = finite(m, 1.0);
        if (m < 1e-6) m = 1e-6;
        if (m > MAX_VALUE) m = MAX_VALUE;
        return m;
    }

    public static double gravRadius(double mass) { return Math.min(MAX_VALUE, BHConfig.gravBase * safePow(mass, BHConfig.gravExponent)); }
    public static double destroyRadius(double mass) { return Math.min(MAX_VALUE, BHConfig.destroyBase * safePow(mass, BHConfig.destroyExponent)); }
    public static double visualRadius(double mass) { return Math.min(MAX_VALUE, BHConfig.visualRatio * destroyRadius(mass)); }

    /** Intensidade da atracao: cresce logaritmicamente com a massa (estavel para qualquer valor). */
    public static double intensity(double mass) {
        return BHConfig.gravIntensity * (1.0 + Math.log10(1.0 + mass) * BHConfig.accelerationFactor);
    }

    public static String fmt(double v) {
        if (v >= 1e6 || (v > 0 && v < 1e-3)) return String.format(java.util.Locale.ROOT, "%.3e", v);
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }
}
