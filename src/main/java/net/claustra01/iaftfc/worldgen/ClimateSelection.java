package net.claustra01.iaftfc.worldgen;

import java.util.function.IntFunction;

public final class ClimateSelection {
    public enum Element { FIRE, ICE, LIGHTNING }

    public record ClimateValues(double temperature, double rainfall, double variance) {}

    public record Result(double[] scores, double[] probabilities, double choice, Element element) {}

    private final double[] mean = new double[3];
    private final double[] deviation = new double[3];
    private final double[] correction = new double[3];

    public ClimateSelection(IntFunction<ClimateValues> sample) {
        double[][] values = new double[4096][3];
        for (int i = 0; i < values.length; i++) {
            ClimateValues point = sample.apply(i);
            values[i][0] = point.temperature();
            values[i][1] = point.rainfall();
            values[i][2] = point.variance();
            for (int j = 0; j < 3; j++) mean[j] += values[i][j];
        }
        for (int j = 0; j < 3; j++) mean[j] /= values.length;
        for (double[] point : values) {
            for (int j = 0; j < 3; j++) deviation[j] += (point[j] - mean[j]) * (point[j] - mean[j]);
        }
        for (int j = 0; j < 3; j++) deviation[j] = Math.max(Math.sqrt(deviation[j] / values.length), 1e-6);

        for (int iteration = 0; iteration < 64; iteration++) {
            double[] average = new double[3];
            for (double[] point : values) {
                double[] probabilities = probabilities(scores(new ClimateValues(point[0], point[1], point[2])));
                for (int j = 0; j < 3; j++) average[j] += probabilities[j] / values.length;
            }
            for (int j = 0; j < 3; j++) correction[j] += Math.log((1.0 / 3.0) / Math.max(average[j], 1e-9));
            double center = (correction[0] + correction[1] + correction[2]) / 3;
            for (int j = 0; j < 3; j++) correction[j] -= center;
        }
    }

    public double[] scores(ClimateValues point) {
        double temperature = (point.temperature() - mean[0]) / deviation[0];
        double rainfall = (point.rainfall() - mean[1]) / deviation[1];
        double variance = (point.variance() - mean[2]) / deviation[2];
        return new double[] {temperature, -temperature, rainfall + 0.5 * variance - 0.5 * Math.abs(temperature)};
    }

    public double[] probabilities(double[] scores) {
        double[] weights = new double[3];
        double total = 0;
        for (int j = 0; j < 3; j++) {
            weights[j] = Math.exp(Math.clamp(scores[j] + correction[j], -15, 15));
            total += weights[j];
        }
        for (int j = 0; j < 3; j++) weights[j] /= total;
        return weights;
    }

    public Result select(ClimateValues point, long seed, int chunkX, int chunkZ, int salt) {
        double[] scores = scores(point);
        double[] probabilities = probabilities(scores);
        long random = splitMix64(seed ^ (long) chunkX * 0x9E3779B97F4A7C15L
            ^ (long) chunkZ * 0xBF58476D1CE4E5B9L ^ salt);
        double choice = (random >>> 11) * 0x1.0p-53;
        Element element = choice < probabilities[0] ? Element.FIRE
            : choice < probabilities[0] + probabilities[1] ? Element.ICE : Element.LIGHTNING;
        return new Result(scores, probabilities, choice, element);
    }

    public static long splitMix64(long value) {
        long result = value + 0x9E3779B97F4A7C15L;
        result = (result ^ (result >>> 30)) * 0xBF58476D1CE4E5B9L;
        result = (result ^ (result >>> 27)) * 0x94D049BB133111EBL;
        return result ^ (result >>> 31);
    }
}
