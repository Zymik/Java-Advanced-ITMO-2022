package info.kgeorgiy.ja.kosolapov.i18n;

import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public enum ReportFormatter {

    RU(new Locale("ru", "RU")) {
        private final ResourceBundle resourceBundle = ResourceBundle.getBundle(PREFIX + "ru");

        @Override
        public String format(TextStatisticsReport textStatisticsReport) {
            return format(textStatisticsReport, resourceBundle);

        }

        @Override
        protected String differentWord(int value, RussianGender gender) {
            if (value % 10 == 1 && value % 100 != 11) {
                return switch (gender) {
                    case MALE -> "различный";
                    case FEMALE -> "различная";
                    case NEUTRAL -> "различное";
                };

            }
            return "различных";
        }
    },

    UK(Locale.UK) {
        private final ResourceBundle resourceBundle = ResourceBundle.getBundle(PREFIX + "en");

        @Override
        public String format(TextStatisticsReport textStatisticsReport) {
            return format(textStatisticsReport, resourceBundle);

        }

        @Override
        protected String differentWord(int value, RussianGender russianGender) {
            return "different";
        }
    };


    ReportFormatter(Locale locale) {
        this.locale = locale;
    }


    private enum RussianGender {
        MALE, FEMALE, NEUTRAL
    }

    private static final String PREAMBLE_FORMAT =
            """
                    {0} "{1}"
                    {2}
                       {3}: {4, number}
                       {5}: {6, number}
                       {7}: {8, number}
                       {9}: {10, number}
                       """;

    private static final String WORD_STYLE_FORMAT =
            """
                    {0}
                       {1}: {2, number, integer} ({3, number, integer} {4}).
                       {5}: "{6}".
                       {7}: "{8}".
                       {9}: {10, number, integer} ("{11}").
                       {12}: {13, number, integer} ("{14}").
                       {15}: {16, number}.
                    """;

    private static final String NUMBER_STYLE_PATTERN =
            """
                    {0}
                       {1}: {2, number, integer} ({3, number, integer} {4}).
                       {5}: {6, %1$s}.
                       {7}: {8, %1$s}.
                       {9}: {10, %1$s}.
                    """;

    private static final String PREFIX = "info.kgeorgiy.ja.kosolapov.i18n.properties.";
    private static final String NUMBER_FORMAT = String.format(NUMBER_STYLE_PATTERN, "number");
    private static final String CURRENCY_FORMAT = String.format(NUMBER_STYLE_PATTERN, "number, currency");
    private static final String DATE_FORMAT = String.format(NUMBER_STYLE_PATTERN, "date");

    private final Locale locale;


    protected String format(TextStatisticsReport report,
                            ResourceBundle resourceBundle) {
        MessageFormat messageFormat = new MessageFormat(PREAMBLE_FORMAT, locale);
        StringBuffer buffer = new StringBuffer();
        Object[] preamble = preambleFormat(report, resourceBundle);
        var field = new FieldPosition(0);
        messageFormat.format(preamble, buffer, field);
        messageFormat = new MessageFormat(WORD_STYLE_FORMAT, locale);

        if (!report.sentences().isEmpty()) {
            Object[] sentences = getWordLike("sentence", resourceBundle, report, report.sentences());
            field.setBeginIndex(0);
            messageFormat.format(sentences, buffer, field);
        }

        if (!report.words().isEmpty()) {
            Object[] words = getWordLike("word", resourceBundle, report, report.words());
            field.setBeginIndex(0);
            messageFormat.format(words, buffer, field);
        }

        if (!report.numbers().isEmpty()) {
            messageFormat = new MessageFormat(NUMBER_FORMAT, locale);
            Object[] numbers = getNumberLike("number", resourceBundle, report.numbers(), report.averageNumber(), RussianGender.NEUTRAL);
            field.setBeginIndex(0);
            messageFormat.format(numbers, buffer, field);
        }

        if (!report.currencies().isEmpty()) {
            messageFormat = new MessageFormat(CURRENCY_FORMAT, locale);
            Object[] currencies = getNumberLike("currency", resourceBundle, report.currencies(), report.averageCurrency(), RussianGender.FEMALE);
            field.setBeginIndex(0);
            messageFormat.format(currencies, buffer, field);
        }

        if (!report.dates().isEmpty()) {
            messageFormat = new MessageFormat(DATE_FORMAT, locale);
            Object[] dates = getNumberLike("date", resourceBundle, report.dates(), report.averageDate(), RussianGender.FEMALE);
            field.setBeginIndex(0);
            messageFormat.format(dates, buffer, field);

        }
        return buffer.toString();
    }

    private <T extends Comparable<? super T>>
    Object[] getNumberLike(String prefix, ResourceBundle resourceBundle, List<T> list, T average, RussianGender gender) {
        int uniqCount = TextStatisticsReport.uniq(list);
        return new Object[]{
                resourceBundle.getString(prefix + "Stat"),
                resourceBundle.getString(prefix + "Count"), list.size(), uniqCount, differentWord(uniqCount, gender),
                resourceBundle.getString(prefix + "Min"), TextStatisticsReport.min(list),
                resourceBundle.getString(prefix + "Max"), TextStatisticsReport.max(list),
                resourceBundle.getString(prefix + "Average"), average
        };
    }

    private Object[] getWordLike(String prefix, ResourceBundle resourceBundle, TextStatisticsReport report,
                                 List<String> list) {
        int uniqCount = TextStatisticsReport.uniq(list);
        String sentenceMinLength = TextStatisticsReport.minByLength(list);
        String sentenceMaxLength = TextStatisticsReport.maxByLength(list);
        return new Object[]{
                resourceBundle.getString(prefix + "Stat"),
                resourceBundle.getString(prefix + "Count"), list.size(), uniqCount, differentWord(uniqCount, RussianGender.NEUTRAL),
                resourceBundle.getString(prefix + "Min"), report.minString(list),
                resourceBundle.getString(prefix + "Max"), report.maxString(list),
                resourceBundle.getString(prefix + "MinLength"), sentenceMinLength.length(), sentenceMinLength,
                resourceBundle.getString(prefix + "MaxLength"), sentenceMaxLength.length(), sentenceMaxLength,
                resourceBundle.getString(prefix + "AverageLength"), TextStatisticsReport.averageLength(list)
        };
    }

    private Object[] preambleFormat(TextStatisticsReport report, ResourceBundle resourceBundle) {
        return new Object[]{
                resourceBundle.getString("analyzedFile"), report.fileName(),
                resourceBundle.getString("summaryStatistics"),
                resourceBundle.getString("sentenceCount"), report.sentenceCount(),
                resourceBundle.getString("wordCount"), report.wordCount(),
                resourceBundle.getString("numberCount"), report.numberCount(),
                resourceBundle.getString("currencyCount"), report.currencyCount(),
                resourceBundle.getString("dateCount"), report.dateCount()
        };
    }

    /**
     * Format report in locale of enum entity
     * @param textStatisticsReport report formatting
     * @return formatted report
     */
    public abstract String format(TextStatisticsReport textStatisticsReport);

    protected abstract String differentWord(int value, RussianGender russianGender);
}
