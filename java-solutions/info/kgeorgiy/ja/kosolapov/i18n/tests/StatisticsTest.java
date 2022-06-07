package info.kgeorgiy.ja.kosolapov.i18n.tests;

import info.kgeorgiy.ja.kosolapov.i18n.ReportFormatter;
import info.kgeorgiy.ja.kosolapov.i18n.TextStatistics;
import info.kgeorgiy.ja.kosolapov.i18n.TextStatisticsReport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Locale.UK;

@DisplayName("Statistics test")
public class StatisticsTest {
    // :NOTE: хочется увидеть тесты на разные версии одного языка
    private static final Format emptyFormat = new Format() {
        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(obj);
        }

        @Override
        public Object parseObject(String source, ParsePosition pos) {
            throw new UnsupportedOperationException("Cannot throw in empty format");
        }
    };

    private enum Word {

        AR("كلمة", StatisticsTest.AR), UK("word", Locale.UK), RU("слово", StatisticsTest.RU);
        private final String word;
        private final Locale locale;

        Word(String word, Locale locale) {
            this.word = word;
            this.locale = locale;
        }

    }

    private static final int STRESS_COUNT = 1000;
    private static final Random random = new Random();
    private static final String SEPARATOR = System.getProperty("line.separator");
    private static final Locale RU = new Locale("ru", "RU");
    private static final Locale AR = new Locale("ar", "AE");
    private static final Locale DE = new Locale("de");

    private static String lineDiff(String actual, String expected) {
        String[] actualLines = splitByLines(actual);
        String[] expectedLines = splitByLines(expected);
        StringBuilder builder = new StringBuilder();
        if (actualLines.length != expectedLines.length) {
            builder.append("Different lines count actual ")
                    .append(actualLines.length)
                    .append(" and ")
                    .append(expectedLines.length);
            builder.append(SEPARATOR);
        }
        int n = Math.min(actualLines.length, expectedLines.length);
        for (int i = 0; i < n; i++) {
            if (!actualLines[i].equals(expectedLines[i])) {
                builder.append(format(i, actualLines[i].trim(), expectedLines[i].trim()));
            }
        }
        return builder.toString();
    }

    private static String format(int line, String actual, String expected) {
        return """
                line: %d
                actual: %s
                expected: %s
                                
                """.formatted(line, actual, expected);

    }

    private static String[] splitByLines(String actual) {
        return actual.split("\\r?\\n");
    }

    private static Path getResource(String resource) throws URISyntaxException {
        return Path.of(Objects.requireNonNull(StatisticsTest.class.getResource("data/" + resource)).toURI());
    }

    private static void runTest(String name, Locale locale) throws URISyntaxException, IOException {
        Path in = getResource(name + ".in");
        Path en = getResource(name + "_en" + ".out");
        Path ru = getResource(name + "_ru" + ".out");
        var report = TextStatistics.analyze(in, locale);
        String actualRu = ReportFormatter.RU.format(report);
        String expectedRu = Files.readString(ru);
        String diff = lineDiff(actualRu, expectedRu);
        Assertions.assertTrue(diff.isBlank(), diff);
        String actualUk = ReportFormatter.UK.format(report);
        String expectedUk = Files.readString(en);
        diff = lineDiff(actualUk, expectedUk);
        Assertions.assertTrue(diff.isBlank(), diff);
    }

    @DisplayName("Simple Russian")
    @Test
    public void simpleRussian() throws URISyntaxException, IOException {
        runTest("simple_russian", RU);
    }

    @DisplayName("Complex Russian")
    @Test
    public void complexRussian() throws URISyntaxException, IOException {
        runTest("complex_russian", RU);
    }

    @DisplayName("Simple English")
    @Test
    public void simpleEnglish() throws URISyntaxException, IOException {
        runTest("simple_english", UK);
    }

    @DisplayName("Complex English")
    @Test
    public void complexEnglish() throws URISyntaxException, IOException {
        runTest("complex_english", UK);
    }

    @DisplayName("Arabic")
    @Test
    public void arabic() throws URISyntaxException, IOException {
        runTest("arabic", AR);
    }

    @DisplayName("Deutsch")
    @Test
    public void deutsch() throws URISyntaxException, IOException {
        runTest("de", DE);
    }

    private static <T> void runStressByLocaleAndFormat(List<T> list,
                                                       Format format,
                                                       Locale locale,
                                                       Function<? super TextStatisticsReport, ? extends List<T>> getter,
                                                       String delimiter) {
        String s = list.stream().map(format::format).collect(Collectors.joining(delimiter));
        var report = TextStatistics.analyze("", s, locale);
        var result = getter.apply(report);
        Assertions.assertEquals(formatted(format, result), formatted(format, result), locale + " Expected: " + s + " Actual:" + result);
    }

    private static <T> List<String> formatted(Format format, List<T> result) {
        return result.stream().map(format::format).toList();
    }

    private static <T> void runStressByLocaleAndFormat(List<T> list,
                                                       Format format,
                                                       Locale locale,
                                                       Function<? super TextStatisticsReport, ? extends List<T>> getter) {
        runStressByLocaleAndFormat(list, format, locale, getter, " ");
    }

    private static void runStressCurrency(int count, Locale locale) {
        runStressByLocaleAndFormat(stressNumber(count), NumberFormat.getCurrencyInstance(locale),
                locale, TextStatisticsReport::currencies);
    }

    private static void runStressNumber(int count, Locale locale) {
        runStressByLocaleAndFormat(stressNumber(count), NumberFormat.getNumberInstance(locale),
                locale, TextStatisticsReport::numbers);
    }

    private static void runWordLike(int count, Word word, String delimiter) {
        Locale locale = word.locale;
        runStressByLocaleAndFormat(stressWord(count, word), emptyFormat, locale, TextStatisticsReport::words, delimiter);
    }

    private static void runStressWords(int count, Word word) {
        runWordLike(count, word, " ");
    }

    private static void runStressSentences(int count, Word word) {
        runWordLike(count, word, ". ");
    }

    private static void test(BiConsumer<Integer, Word> runner) {
        var set = EnumSet.allOf(Word.class);
        for (var i : set) {
            runner.accept(STRESS_COUNT, i);
        }
    }

    @DisplayName("Test number parsing")
    @Test
    public void numberTest() {
        test((x, y) -> runStressNumber(x, y.locale));
    }

    @DisplayName("Test currency parsing")
    @Test
    public void currencyTest() {
        test((x, y) -> runStressCurrency(x, y.locale));
    }

    @DisplayName("Test word parsing")
    @Test
    public void wordTest() {
        test(StatisticsTest::runStressWords);
    }

    @DisplayName("Test sentence parsing")
    @Test
    public void sentenceTest() {
        test(StatisticsTest::runStressSentences);
    }

    private static List<Double> stressNumber(int count) {
        return random.doubles(count).map(x -> 10000 * x).boxed().toList();
    }

    private static List<String> stressWord(int count, Word word) {
        return Stream.generate(() -> word.word).limit(count).toList();
    }
}
