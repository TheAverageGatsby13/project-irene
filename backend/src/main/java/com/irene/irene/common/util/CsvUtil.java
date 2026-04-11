package com.irene.irene.common.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class CsvUtil {

    private CsvUtil() {
    }

    public static String join(List<String> values) {
        return values == null ? "" : values.stream()
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .collect(Collectors.joining(","));
    }

    public static List<String> split(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .toList();
    }
}
