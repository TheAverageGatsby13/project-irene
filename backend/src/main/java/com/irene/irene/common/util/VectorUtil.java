package com.irene.irene.common.util;

import java.util.List;

public final class VectorUtil {

    private VectorUtil() {
    }

    public static String serialize(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(vector[i]);
        }
        return builder.toString();
    }

    public static float[] deserialize(String value) {
        List<String> parts = CsvUtil.split(value);
        float[] vector = new float[parts.size()];
        for (int i = 0; i < parts.size(); i++) {
            vector[i] = Float.parseFloat(parts.get(i));
        }
        return vector;
    }

    public static double cosineSimilarity(float[] a, float[] b) {
        if (a.length == 0 || b.length == 0 || a.length != b.length) {
            return 0d;
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0d;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static float[] deterministicEmbedding(String text, int dimensions) {
        float[] vector = new float[dimensions];
        if (text == null || text.isBlank()) {
            return vector;
        }
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            int idx = i % dimensions;
            vector[idx] += (chars[i] % 23) / 23.0f;
        }
        float max = 1f;
        for (float v : vector) {
            max = Math.max(max, Math.abs(v));
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / max;
        }
        return vector;
    }

    public static String pretty(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(String.format("%.4f", vector[i]));
        }
        return builder.toString();
    }
}
