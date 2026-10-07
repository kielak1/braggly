package com.example.backend.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Flat formula sums only: no groups, charges, isotope labels or hydrate separators. */
final class ChemicalComposition {
    private static final Pattern TOKEN = Pattern.compile("([A-Z][a-z]?)\\s*([0-9]+(?:\\.[0-9]+)?)?");
    private static final Set<String> ELEMENTS = Set.of((
            "H He Li Be B C N O F Ne Na Mg Al Si P S Cl Ar K Ca Sc Ti V Cr Mn Fe Co Ni Cu Zn "
            + "Ga Ge As Se Br Kr Rb Sr Y Zr Nb Mo Tc Ru Rh Pd Ag Cd In Sn Sb Te I Xe Cs Ba La Ce "
            + "Pr Nd Pm Sm Eu Gd Tb Dy Ho Er Tm Yb Lu Hf Ta W Re Os Ir Pt Au Hg Tl Pb Bi Po At Rn "
            + "Fr Ra Ac Th Pa U Np Pu Am Cm Bk Cf Es Fm Md No Lr Rf Db Sg Bh Hs Mt Ds Rg Cn Nh Fl Mc Lv Ts Og"
    ).split(" "));

    private ChemicalComposition() {}

    static boolean isElementSymbol(String symbol) {
        return ELEMENTS.contains(symbol);
    }

    static Optional<Map<String, BigDecimal>> parse(String formula) {
        if (formula == null || formula.length() > 257) return Optional.empty();
        String text = formula.strip();
        if (text.startsWith("-") && text.endsWith("-") && text.length() >= 2) {
            text = text.substring(1, text.length() - 1).strip();
        }
        Map<String, BigDecimal> composition = new TreeMap<>();
        Matcher matcher = TOKEN.matcher(text);
        int offset = 0;
        while (offset < text.length()) {
            if (Character.isWhitespace(text.charAt(offset))) { offset++; continue; }
            matcher.region(offset, text.length());
            if (!matcher.lookingAt() || !ELEMENTS.contains(matcher.group(1))) return Optional.empty();
            BigDecimal count = matcher.group(2) == null ? BigDecimal.ONE : new BigDecimal(matcher.group(2));
            if (count.signum() <= 0) return Optional.empty();
            composition.merge(matcher.group(1), count, BigDecimal::add);
            offset = matcher.end();
        }
        if (composition.isEmpty()) return Optional.empty();
        composition.replaceAll((element, count) -> count.stripTrailingZeros());
        return Optional.of(Collections.unmodifiableMap(composition));
    }

    /** SQL prefilter only; parsed element counts decide equality, never this regex. */
    static String candidatePattern(Set<String> elements) {
        String symbols = String.join("|", elements);
        return "^[[:space:]]*(-[[:space:]]*)?((" + symbols
                + ")[[:space:]]*([0-9]+([.][0-9]+)?)?[[:space:]]*)+(-[[:space:]]*)?$";
    }
}
