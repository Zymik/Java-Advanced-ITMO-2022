package info.kgeorgiy.ja.kosolapov.i18n;

import java.text.Collator;
import java.util.*;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

public record TextStatisticsReport(Locale locale, String fileName, List<String> words, List<String> sentences, List<Date> dates,
                                   List<Double> numbers, List<Double> currencies) {

    /**
     * {@code words().size()}
     */
    public int wordCount() {
        return words.size();
    }

    /**
     * {@code sentences().size()}
     */
    public int sentenceCount() {
        return sentences.size();
    }

    /**
     *  {@code currencies().size()}
     */
    public int dateCount() {
        return dates.size();
    }

    /**
     *  {@code numbers().size()}
     */
    public int numberCount() {
        return numbers.size();
    }

    /**
     * {@code currencies().size()}
     */
    public int currencyCount() {
        return currencies.size();
    }

    /**
     * Find count of uniq elements in list
     * @return count of uniq element in list
     */
    public static <T> int uniq(List<T> list) {
        return (int) list.stream().distinct().count();
    }

    /**
     * Find average value in list
     * @return {@code list.stream.mapToDouble(x -> x).average()}
     */
    public static double average(List<Double> list) {
        return average(list.stream());
    }

    private static double average(Stream<Double> stream) {
        return average(stream.mapToDouble(x -> x));
    }

    private static double average(DoubleStream stream) {
        return stream.average().orElse(0);
    }

    /**
     *
     * @return average length of word in {@code list}
     */
    public static double averageLength(List<String> list) {
        return average(list.stream().mapToDouble(String::length));
    }

    /**
     *
     * @return {@code average(currencies())}
     */
    public double averageCurrency() {
        return average(currencies);
    }

    /**
     * @return {@code average(numbers)}
     */
    public double averageNumber() {
        return average(numbers);
    }

    /**
     * @return date with average time in millis
     * @throws ArithmeticException if {@code dates.isEmpty()}
     */
    public Date averageDate() {
        return new Date(dates.stream().mapToLong(Date::getTime).sum() / dates.size());
    }

    /**
     * Find min in list by comparator
     */
    public static <T> T min(List<T> list, Comparator<? super T> comparator) {
        return list.stream().min(comparator).orElse(null);
    }

    /**
     * Find max in list by comparator
     */
    public static <T> T max(List<T> list, Comparator<? super T> comparator) {
        return min(list, comparator.reversed());
    }

    /**
     * {@code max(list, Collator.getInstance(locale()))}
     */
    public String maxString(List<String> list) {
        return max(list, Collator.getInstance(locale));
    }

    /**
     * {@code min(list, Collator.getInstance(locale()))}
     */
    public String minString(List<String> list) {
        return min(list, Collator.getInstance(locale));
    }

    /**
     * Find first {@link String} in list with minimal length
     */
    public static String minByLength(List<String> list) {
        return min(list, Comparator.comparingInt(String::length));
    }

    /**
     * Find first {@link String} in list with maximal length
     */
    public static String maxByLength(List<String> list) {
        return max(list, Comparator.comparingInt(String::length));
    }

    /**
     * {@code min(list, Comparator.naturalOrder())}
     * @see Comparator#naturalOrder
     */
    public static <T extends Comparable<? super T>> T min(List<T > list) {
       return min(list, Comparator.naturalOrder());
    }

    /**
     * {@code max(list, Comparator.naturalOrder())}
     * @see Comparator#naturalOrder
     */
    public static  <T extends Comparable<? super T>> T max(List<T > list) {
        return max(list, Comparator.naturalOrder());
    }

}
