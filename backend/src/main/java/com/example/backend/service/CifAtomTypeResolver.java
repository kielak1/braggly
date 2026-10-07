package com.example.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.regex.Pattern;

/** Conservative subset of IUCr atom-type codes and atom-site label components. */
final class CifAtomTypeResolver {
    private static final Pattern TYPE = Pattern.compile("([A-Z][a-z]?)(?:[0-9]+[+-]|[+-][0-9]+|[+-])?");
    // Accept numbered sites (C1, O1A), parenthesized sites (O(3A)), and underscore components.
    // Arbitrary words, mixed species, isotopes and ambiguous charge/site suffixes are not inferred.
    private static final Pattern LABEL = Pattern.compile(
            "([A-Z][a-z]?)(?:[0-9]+[A-Za-z0-9]*|\\([0-9]+[A-Za-z0-9]*\\))?(?:_[A-Za-z0-9_]+)?");

    private CifAtomTypeResolver() {}

    static String resolve(String explicitType, String component0, String label, Set<String> declaredTypes) {
        // Explicit per-site type always wins, even when the label describes another element.
        if (present(explicitType)) return element(explicitType);
        if (present(component0)) return declaredElement(component0, declaredTypes);
        if (!present(label)) throw unsupported();
        var match = LABEL.matcher(label);
        if (!match.matches()) throw unsupported();
        return declaredElement(match.group(1), declaredTypes);
    }

    private static String declaredElement(String code, Set<String> declaredTypes) {
        // A file-level atom_type table is a dictionary, never a row-wise mapping or a reason to guess.
        if (!declaredTypes.isEmpty() && !declaredTypes.contains(code)) throw unsupported();
        return element(code);
    }

    private static String element(String code) {
        var match = TYPE.matcher(code);
        if (!match.matches() || !ChemicalComposition.isElementSymbol(match.group(1))) throw unsupported();
        return match.group(1); // Viewer needs the element, without oxidation-state suffix.
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank() && !value.equals(".") && !value.equals("?");
    }

    static ResponseStatusException unsupported() {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "CIF atom data is not supported");
    }
}
