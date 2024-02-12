package ru.stileyn;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.Paths;

public class UtilityForFilteringFileContent {
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("o", "output", true, "Путь для результатов");
        options.addOption("p", "prefix", true, "Префикс имен выходных файлов");
        options.addOption("a", "append", false, "Режим добавления в существующие файлы");
        options.addOption("s", "short", false, "Краткая статистика");
        options.addOption("f", "full", false, "Полная статистика");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            String outputPath = cmd.getOptionValue("o", "./src/main/result/");
            String prefix = cmd.getOptionValue("p", "");
            boolean append = cmd.hasOption("a");
            boolean shortStats = cmd.hasOption("s");
            boolean fullStats = cmd.hasOption("f");
            String[] inputFiles = cmd.getArgs();

            filterAndWriteFiles(inputFiles, outputPath, prefix, append, shortStats, fullStats);

        } catch (org.apache.commons.cli.ParseException e) {
            System.err.println("Ошибка при разборе аргументов командной строки: " + e.getMessage());
        }
    }

    private static void filterAndWriteFiles(String[] inputFiles, String outputPath, String prefix,
                                            boolean append, boolean shortStats, boolean fullStats) {
        Statistics stats = new Statistics();

        for (String inputFile : inputFiles) {
            try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    processDataLine(line, outputPath, prefix, append, stats);
                }
            } catch (IOException e) {
                System.err.println("Ошибка при чтении файла: " + inputFile + ". " + e.getMessage());
            }
        }

        if (shortStats) {
            System.out.println(stats.getShortStatistics());
        }
        if (fullStats) {
            System.out.println(stats.getFullStatistics());
        }
    }

    private static void processDataLine(String line, String outputPath, String prefix, boolean append,
                                        Statistics stats) {
        try {
            double number = Double.parseDouble(line);
            if (Math.floor(number) == number) {
                writeToFile(outputPath, prefix + "integers.txt", line, append);
                stats.incrementIntegerCount();
            } else {
                writeToFile(outputPath, prefix + "floats.txt", line, append);
                stats.incrementFloatCount();
                stats.updateFloatStatistics(number);
            }
        } catch (NumberFormatException e) {
            writeToFile(outputPath, prefix + "strings.txt", line, append);
            stats.incrementStringCount();
            stats.updateStringStatistics(line);
        }
    }

    private static void writeToFile(String outputPath, String fileName, String data, boolean append) {
        try {
            Path directoryPath = Paths.get(outputPath);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            Path filePath = directoryPath.resolve(fileName);
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }

            StandardOpenOption[] options = append ? new StandardOpenOption[]{StandardOpenOption.APPEND} :
                    new StandardOpenOption[]{};

            try (BufferedWriter writer = Files.newBufferedWriter(filePath, options)) {
                writer.write(data);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Ошибка при записи в файл: " + fileName + ". " + e.getMessage());
        }
    }

    private static class Statistics {
        private int integerCount = 0;
        private int floatCount = 0;
        private int stringCount = 0;
        private double sum = 0;
        private double min = Double.MAX_VALUE;
        private double max = Double.MIN_VALUE;
        private int shortestStringLength = Integer.MAX_VALUE;
        private int longestStringLength = Integer.MIN_VALUE;

        public void incrementIntegerCount() {
            integerCount++;
        }

        public void incrementFloatCount() {
            floatCount++;
        }

        public void incrementStringCount() {
            stringCount++;
        }

        public void updateFloatStatistics(double number) {
            sum += number;
            if (number < min) min = number;
            if (number > max) max = number;
        }

        public void updateStringStatistics(String line) {
            int length = line.length();
            if (length < shortestStringLength) shortestStringLength = length;
            if (length > longestStringLength) longestStringLength = length;
        }

        public String getShortStatistics() {
            return "Количество целых чисел: " + integerCount + "\n" +
                    "Количество вещественных чисел: " + floatCount + "\n" +
                    "Количество строк: " + stringCount;
        }

        public String getFullStatistics() {
            return "Количество целых чисел: " + integerCount + "\n" +
                    "Количество вещественных чисел: " + floatCount + "\n" +
                    "Минимальное вещественное число: " + min + "\n" +
                    "Максимальное вещественное число: " + max + "\n" +
                    "Сумма вещественных чисел: " + sum + "\n" +
                    "Среднее вещественное число: " + (sum / floatCount) + "\n" +
                    "Самая короткая строка: " + shortestStringLength + "\n" +
                    "Самая длинная строка: " + longestStringLength;
        }
    }
}
