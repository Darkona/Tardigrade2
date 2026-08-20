package com.darkona.tardigrade.logging;

import java.util.List;

/** Cycles a string through the palette, one color per visible character. */
public final class Rainbow {

    private static final List<BoldAnsi> COLORS = List.of(
            BoldAnsi.RED, BoldAnsi.ORANGE, BoldAnsi.YELLOW, BoldAnsi.GREEN,
            BoldAnsi.AQUA, BoldAnsi.BLUE, BoldAnsi.PURPLE, BoldAnsi.PINK);

    private Rainbow() {
    }

    public static String rainbowify(String s) {
        StringBuilder result = new StringBuilder();
        int colorIndex = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\n') {
                result.append(COLORS.get(colorIndex));
                colorIndex = (colorIndex + 1) % COLORS.size();
            } else {
                if (c == '\n') {
                    colorIndex = 0;
                }
            }
            result.append(c);
        }
        result.append(RegularAnsi.RESET);
        return result.toString();
    }
}
