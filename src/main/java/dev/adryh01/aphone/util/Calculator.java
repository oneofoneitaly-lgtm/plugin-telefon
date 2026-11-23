package dev.adryh01.aphone.util;

/** Tiny recursive-descent math parser: + - * / % ^ ( ) and decimals. */
public final class Calculator {

    private final String s;
    private int pos = -1;
    private int ch;

    private Calculator(String s) {
        this.s = s.replace(" ", "").replace(",", ".").replace("x", "*");
    }

    public static double eval(String expr) {
        Calculator c = new Calculator(expr);
        c.next();
        double v = c.expression();
        if (c.pos < c.s.length()) throw new IllegalArgumentException("Unexpected: " + (char) c.ch);
        if (Double.isNaN(v) || Double.isInfinite(v)) throw new ArithmeticException("Not a number");
        return v;
    }

    public static String format(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e15) return String.valueOf((long) v);
        return String.format(java.util.Locale.ROOT, "%.6f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private void next() {
        ch = (++pos < s.length()) ? s.charAt(pos) : -1;
    }

    private boolean eat(int c) {
        if (ch == c) {
            next();
            return true;
        }
        return false;
    }

    private double expression() {
        double x = term();
        while (true) {
            if (eat('+')) x += term();
            else if (eat('-')) x -= term();
            else return x;
        }
    }

    private double term() {
        double x = factor();
        while (true) {
            if (eat('*')) x *= factor();
            else if (eat('/')) x /= factor();
            else if (eat('%')) x %= factor();
            else return x;
        }
    }

    private double factor() {
        if (eat('+')) return factor();
        if (eat('-')) return -factor();
        double x;
        int start = pos;
        if (eat('(')) {
            x = expression();
            if (!eat(')')) throw new IllegalArgumentException("Missing )");
        } else if ((ch >= '0' && ch <= '9') || ch == '.') {
            while ((ch >= '0' && ch <= '9') || ch == '.') next();
            x = Double.parseDouble(s.substring(start, pos));
        } else {
            throw new IllegalArgumentException("Unexpected character");
        }
        if (eat('^')) x = Math.pow(x, factor());
        return x;
    }
}
