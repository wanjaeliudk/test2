package com.dtbafrica.scf.ussd.support;

import java.util.Optional;

/** Maps a 1-based menu reply onto a 0-based list index. */
public final class Selection {

    private Selection() {}

    public static Optional<Integer> index(String input, int size) {
        if (input == null || !input.chars().allMatch(Character::isDigit) || input.isEmpty()) {
            return Optional.empty();
        }
        try {
            int chosen = Integer.parseInt(input);
            return chosen >= 1 && chosen <= size ? Optional.of(chosen - 1) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
