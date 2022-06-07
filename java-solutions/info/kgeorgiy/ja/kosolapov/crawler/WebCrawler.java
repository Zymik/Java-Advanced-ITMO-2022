package info.kgeorgiy.ja.kosolapov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class WebCrawler implements Crawler {

    private final Downloader downloader;
    private final ConcurrentHashMap<String, HostTasks> hostTasks;
    private final ExecutorService downloadExecutor;
    private final int perHost;
    private final ExecutorService extractExecutor;

    private static int extractArgOrGetOne(String[] args, int pos) throws NumberFormatException {
        try {
            return args.length > pos ? Integer.parseInt(args[pos]) : 1;
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Value at " + pos + " is invalid int: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length < 1 || args.length > 5) {
            return;
        }
        String url = args[0];
        try {
            int depth = extractArgOrGetOne(args, 1);
            int downloads = extractArgOrGetOne(args, 2);
            int extractors = extractArgOrGetOne(args, 3);
            int perHost = extractArgOrGetOne(args, 4);
            var path = Files.createTempDirectory(Path.of(""), "downloaded");
            Downloader downloader = new CachingDownloader(path);
            System.out.println("Will save to: " + path.toAbsolutePath());
            Result result;
            try (var crawler = new WebCrawler(downloader, downloads, extractors, perHost)) {
                result = crawler.download(url, depth);
            }
            var downloaded = result.getDownloaded();
            System.out.println("Downloaded: " + downloaded.size());
            result.getDownloaded().forEach(System.out::println);
            var errors = result.getErrors();
            System.out.println("Errors: " + errors.size());
            errors.forEach((x, y) -> System.out.println("URL: " + x + " Error: " + y.getMessage()));
        } catch (NumberFormatException e) {
            // :NOTE: err
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println("Can not create CachingDownloader: " + e.getMessage());
        }

    }

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        downloadExecutor = Executors.newFixedThreadPool(downloaders);
        extractExecutor = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
        hostTasks = new ConcurrentHashMap<>();
    }

    private void extractLinks(Document document, Deque<String> next,
                              Phaser phaser, int depth) {

        if (depth != 1) {
            phaser.register();
            extractExecutor.submit(linkExtraction(document, next, phaser));
        }
    }

    private Runnable linkExtraction(Document document, Deque<String> next, Phaser phaser) {

        return () -> {
            try {
                next.addAll(document.extractLinks());
            } catch (IOException ignored) {
            } finally {
                phaser.arriveAndDeregister();
            }
        };
    }


    private Runnable runnableFromUrl(String url, HostTasks hostTasks, Set<String> downloaded, Phaser phaser,
                                     Map<String, IOException> errors, Consumer<Document> linkExtractor) {
        return () -> {
            try {
                var document = downloader.download(url);
                downloaded.add(url);
                linkExtractor.accept(document);
            } catch (IOException exception) {
                errors.put(url, exception);
            } finally {
                phaser.arriveAndDeregister();
                hostTasks.finish();
            }
        };
    }

    private Deque<String> downloadUrlsAndExtractLInks(List<String> urls, Map<String, IOException> errors,
                                                      Set<String> downloaded,
                                                      int depth) throws InterruptedException {
        // :NOTE: Queue
        Deque<String> next = new ConcurrentLinkedDeque<>();

        var phaser = new Phaser(1);
        Consumer<Document> linkExtractor = (document) -> extractLinks(document, next, phaser, depth);
        for (var url : urls) {
            try {
                var hostTasks = getHost(url);
                phaser.register();
                hostTasks.addTask(runnableFromUrl(url, hostTasks, downloaded, phaser, errors, linkExtractor));
            } catch (MalformedURLException e) {
                errors.put(url, e);
            }
        }
        phaser.awaitAdvanceInterruptibly(phaser.arrive());
        return next;
    }

    @Override
    public Result download(String url, int depth) {
        List<String> current = List.of(url);

        Set<String> marked = new HashSet<>();
        Set<String> downloaded = ConcurrentHashMap.newKeySet();
        Map<String, IOException> errors = new ConcurrentHashMap<>();

        marked.add(url);

        for (int i = depth; i > 0; i--) {
            try {
                current = downloadUrlsAndExtractLInks(current, errors, downloaded, depth)
                        .stream()
                        .filter(x -> !marked.contains(x))
                        .peek(marked::add)
                        .toList();
            } catch (InterruptedException e) {
                System.err.println("Unexpected interruption, on " + (depth - i + 1) + " levels of " + depth);
                break;
            }
        }
        return new Result(downloaded.stream().toList(), errors);
    }

    private HostTasks getHost(String url) throws MalformedURLException {
        return hostTasks.computeIfAbsent(URLUtils.getHost(url), x -> new HostTasks());
    }


    private class HostTasks {
        private final Queue<Runnable> tasks = new ConcurrentLinkedDeque<>();
        private final Semaphore semaphore = new Semaphore(perHost);

        public void addTask(Runnable task) {
            if (semaphore.tryAcquire()) {
                downloadExecutor.submit(task);
            } else {
                tasks.add(task);
            }

        }

        public void finish() {
            var task = tasks.poll();
            if (task != null) {
                downloadExecutor.submit(task);
            } else {
                semaphore.release();
            }
        }
    }

    @Override
    public void close() {
        downloadExecutor.shutdown();
        extractExecutor.shutdown();
        forceShutDown(downloadExecutor);
        forceShutDown(extractExecutor);
    }


    private void forceShutDown(ExecutorService executorService) {
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

}
