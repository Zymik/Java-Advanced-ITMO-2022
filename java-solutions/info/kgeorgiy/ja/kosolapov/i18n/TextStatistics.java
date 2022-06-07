package info.kgeorgiy.ja.kosolapov.i18n;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public class TextStatistics {


    /***
     * Runs {@link TextStatistics#analyzeStringReport(ReportFormatter, Path, Path, Locale)} on this args.
     * if args size is 4, expected format: in locale, out locale(RU or UK), in file, out file.
     * if args size is 5, expected format: in locale, country, out locale(RU or UK), in file, out file
     * @param args args to run on
     */
    public static void main(String[] args)  {
        if (args.length != 4 && args.length != 5) {
            System.err.println("Args length should be 4 or 5");
            return;
        }
        int i = 0;
        Locale locale;
        if (args.length == 5) {
            i++;
            locale = new Locale(args[0], args[1]);
        } else {
            locale = new Locale(args[0]);
        }

        ReportFormatter reportFormatter = switch (args[i + 1].toUpperCase()) {
            case "UK" -> ReportFormatter.UK;
            case "RU" -> ReportFormatter.RU;
            default -> null;
        };

        if (reportFormatter == null) {
            System.err.println("Output Locale argument should be RU or UK");
            return;
        }
        Path input = Path.of(args[i + 2]);
        Path output = Path.of(args[i + 3]);
        try {
            analyzeStringReport(reportFormatter, input, output, locale);
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }
    }

    /**
     * Analyzes from {@code input} in {@code locale} and format by {@code reportFormatter} and write it to {@code output}
     * @param reportFormatter formatter of report
     * @param input file to analyze
     * @param output file to write report
     * @param locale locale of text
     */
    public static void analyzeStringReport(ReportFormatter reportFormatter, Path input, Path output, Locale locale) throws IOException {
        String report = analyze(reportFormatter, input, locale);
        Files.writeString(output, report);
    }

    /**
     * Analyzes from {@code input} in {@code locale} and format by {@code reportFormatter}
     * @param reportFormatter formatter of report
     * @param input file to analyze
     * @param locale locale of text
     * @return report of analyzing
     */
    public static String analyze(ReportFormatter reportFormatter, Path input, Locale locale) throws IOException {
        return reportFormatter.format(analyze(input, locale));
    }

    /**
     * Analyzes from {@code input} in {@code locale}
     * @param input file to analyze
     * @param locale locale of text
     * @return report of analyzing
     */
    public static TextStatisticsReport analyze(Path input, Locale locale) throws IOException {
        return analyze(input.getFileName().toString(),Files.readString(input), locale);
    }

    /**
     * Analyzes {@code text} from file with name {@code fileName} in {@code locale}
     * @param fileName name of input file
     * @param text text to analyze
     * @param locale locale of text
     * @return report of analyzing
     */
    public static TextStatisticsReport analyze(String fileName, String text, Locale locale) {
        List<String> sentences = getSentences(text, locale);
        List<String> words = countWords(text, locale);
        List<Date> dates = countDates(text, locale);
        List<Double> numbers = countNumber(text, locale);
        List<Double> currencies = countCurrency(text, locale);
        return new TextStatisticsReport(locale, fileName, words, sentences, dates, numbers, currencies);
    }

    private static List<Double> countNumberDouble(String text, Locale locale, NumberFormat numberFormat) {
        return formatAnalyze(text, locale, numberFormat::parse).stream().map(Number::doubleValue).toList();
    }

    private static List<Double> countNumber(String text, Locale locale) {
        return countNumberDouble(text, locale, NumberFormat.getNumberInstance(locale));
    }

    private static List<Double> countCurrency(String text, Locale locale) {
        return countNumberDouble(text, locale, NumberFormat.getCurrencyInstance(locale));
    }

    private static List<Date> countDates(String text, Locale locale) {
        return formatAnalyze(text, locale, DateFormat.getDateInstance(DateFormat.DEFAULT, locale)::parse);
    }

    private static <T> List<T> formatAnalyze(String text, Locale locale, TypedParser<T> parser) {
        var iterator = BreakIterator.getCharacterInstance(locale);
        iterator.setText(text);
        int bound = 0;
        int left = iterator.first();
        int right = iterator.next();
        List<T> answer = new ArrayList<>();
        while (notDone(right)) {
            var parsePosition = new ParsePosition(left);
            if (left >= bound) {
                parse(text, answer, parser, left, parsePosition);
            }
            bound = Math.max(bound, parsePosition.getIndex());
            left = right;
            right = iterator.next();
        }
        return answer;
    }

    private static <T> void parse(String text, List<? super T> data, TypedParser<? extends T> format,
                                  int left, ParsePosition parsePosition) {
        T answer = format.parse(text, parsePosition);
        if (parsePosition.getIndex() != left) {
            data.add(answer);
        }
    }

    private static List<String> countWords(String text, Locale locale) {
        return stringCounterWithPredicate(text, BreakIterator.getWordInstance(locale), Character::isLetter);
    }

    private static List<String> getSentences(String text, Locale locale) {
        return stringCounterWithPredicate(text, BreakIterator.getSentenceInstance(locale), a -> true);
    }

    private static List<String> stringCounterWithPredicate(String text, BreakIterator iterator,
                                                           Predicate<? super Character> predicate) {
        List<String> data = new ArrayList<>();
        iterator.setText(text);
        int left = iterator.first();
        int right = iterator.next();
        while (notDone(right)) {
            if (predicate.test(text.charAt(left))) {
                data.add(text.substring(left, right).trim().replaceAll("\n|\r\n", " "));
            }
            left = right;
            right = iterator.next();
        }
        return data;
    }

    private static boolean notDone(int right) {
        return right != BreakIterator.DONE;
    }

    @FunctionalInterface
    private interface TypedParser<T> {
        T parse(String text, ParsePosition parsePosition);
    }
}
