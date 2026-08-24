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

import java.util.List;
import java.util.Locale;

/**
 * An easing curve mapping a normalized progress {@code [0,1]} onto an eased
 * progress {@code [0,1]}, used to shape the interpolation between two
 * {@link Keyframe keyframes}.
 * <p>
 * The configuration accepts either one of the CSS timing-function names
 * ({@code linear}, {@code ease}, {@code ease-in}, {@code ease-out},
 * {@code ease-in-out}) or a four element list describing a
 * {@code cubic-bezier(x1, y1, x2, y2)} curve, which behaves exactly like its
 * CSS counterpart.
 */
@FunctionalInterface
public interface Easing {

    Easing LINEAR = t -> t;
    Easing EASE = cubicBezier(0.25, 0.1, 0.25, 1);
    Easing EASE_IN = cubicBezier(0.42, 0, 1, 1);
    Easing EASE_OUT = cubicBezier(0, 0, 0.58, 1);
    Easing EASE_IN_OUT = cubicBezier(0.42, 0, 0.58, 1);

    /**
     * Applies the curve.
     *
     * @param t the normalized progress, within {@code [0,1]}
     * @return the eased progress
     */
    double apply(double t);

    /**
     * Creates a CSS style {@code cubic-bezier(x1, y1, x2, y2)} easing.
     *
     * @param x1 the first control point's abscissa, within {@code [0,1]}
     * @param y1 the first control point's ordinate
     * @param x2 the second control point's abscissa, within {@code [0,1]}
     * @param y2 the second control point's ordinate
     * @return the easing
     */
    static Easing cubicBezier(double x1, double y1, double x2, double y2) {
        if (x1 < 0 || x1 > 1 || x2 < 0 || x2 > 1) {
            throw new IllegalArgumentException("cubic-bezier x coordinates must be within [0,1], got " + x1 + " and " + x2);
        }
        return new CubicBezierEasing(x1, y1, x2, y2);
    }

    /**
     * Parses an easing from its configuration representation.
     *
     * @param o a preset name, or a four element list of numbers
     * @return the easing, never null
     */
    static Easing parse(Object o) {
        if (o == null) return LINEAR;
        if (o instanceof List<?> list) {
            if (list.size() != 4) {
                throw new IllegalArgumentException("cubic-bezier easing requires exactly 4 numbers, got " + list.size());
            }
            double[] p = new double[4];
            for (int i = 0; i < 4; i++) {
                if (!(list.get(i) instanceof Number n)) {
                    throw new IllegalArgumentException("cubic-bezier easing expects numbers, got " + list.get(i));
                }
                p[i] = n.doubleValue();
            }
            return cubicBezier(p[0], p[1], p[2], p[3]);
        }
        String name = o.toString().trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return switch (name) {
            case "linear" -> LINEAR;
            case "ease" -> EASE;
            case "ease-in", "in" -> EASE_IN;
            case "ease-out", "out" -> EASE_OUT;
            case "ease-in-out", "in-out" -> EASE_IN_OUT;
            default -> throw new IllegalArgumentException(
                    "Unknown easing '" + o + "'. Use linear/ease/ease-in/ease-out/ease-in-out or [x1,y1,x2,y2]");
        };
    }

    /**
     * The {@code cubic-bezier(x1, y1, x2, y2)} curve.
     * <p>
     * Both axes are cubic beziers anchored at {@code (0,0)} and {@code (1,1)}, so
     * evaluating the curve means solving {@code x(u) = t} for the bezier parameter
     * {@code u} first, then reading {@code y(u)}. Newton-Raphson converges within a
     * handful of iterations for well-formed curves; a bisection fallback keeps
     * near-vertical ones (a derivative close to zero) from diverging.
     */
    record CubicBezierEasing(double x1, double y1, double x2, double y2) implements Easing {

        private static final int NEWTON_ITERATIONS = 8;
        private static final double EPSILON = 1e-7;

        @Override
        public double apply(double t) {
            if (t <= 0) return 0;
            if (t >= 1) return 1;
            return bezier(solveForX(t), y1, y2);
        }

        private double solveForX(double t) {
            double u = t;
            for (int i = 0; i < NEWTON_ITERATIONS; i++) {
                double error = bezier(u, x1, x2) - t;
                if (Math.abs(error) < EPSILON) return u;
                double slope = bezierSlope(u, x1, x2);
                if (Math.abs(slope) < EPSILON) break;
                u -= error / slope;
            }
            // Newton stalled on a flat spot, fall back to bisection.
            double low = 0, high = 1;
            u = t;
            for (int i = 0; i < 32 && high - low > EPSILON; i++) {
                if (bezier(u, x1, x2) < t) low = u; else high = u;
                u = (low + high) / 2;
            }
            return u;
        }

        /** The polynomial form of a cubic bezier anchored at 0 and 1. */
        private static double bezier(double u, double p1, double p2) {
            double a = 1 - 3 * p2 + 3 * p1;
            double b = 3 * p2 - 6 * p1;
            double c = 3 * p1;
            return ((a * u + b) * u + c) * u;
        }

        private static double bezierSlope(double u, double p1, double p2) {
            double a = 1 - 3 * p2 + 3 * p1;
            double b = 3 * p2 - 6 * p1;
            double c = 3 * p1;
            return (3 * a * u + 2 * b) * u + c;
        }
    }
}
