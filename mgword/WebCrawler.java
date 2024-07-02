import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WebCrawler {


    private CrawlListener crawlListener;
    private final Set<String> visitedUrls = new HashSet<>();
    private final Map<String, Set<String>> urlRelations = new HashMap<>();
    private final ExecutorService executorService;
    private volatile boolean running = true;
    private final Map<String, String> htmlContents = new HashMap<>();
    private final Map<String, String> textContents = new HashMap<>();

    public interface CrawlListener {
        void onCrawling(String url);
    }

    public WebCrawler(int numThreads) {
        executorService = Executors.newFixedThreadPool(numThreads);
    }

    public void setCrawlListener(CrawlListener crawlListener) {
        this.crawlListener = crawlListener;
    }

    public void startCrawling(String startUrl) {
        running = true;
        executorService.submit(() -> crawl(startUrl));
    }

    public void stopCrawling() {
        running = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    private void crawl(String url) {
        if (!running || visitedUrls.contains(url)) {
            return;
        }
        visitedUrls.add(url);
        if (crawlListener != null) {
            crawlListener.onCrawling(url);
        }
        try {
            Document document = Jsoup.connect(url).get();
            System.out.println("Crawling: " + url);

            htmlContents.put(url, document.html());
            textContents.put(url, document.text());

            Elements links = document.select("a[href]");
            Set<String> linkedUrls = urlRelations.getOrDefault(url, new HashSet<>());
            for (Element link : links) {
                String nextUrl = link.absUrl("href");
                if (nextUrl.startsWith("http")) {
                    linkedUrls.add(nextUrl);
                    if (!visitedUrls.contains(nextUrl)) {
                        executorService.submit(() -> crawl(nextUrl));
                    }
                }
            }
            urlRelations.put(url, linkedUrls);
        } catch (IOException e) {
            System.err.println("Error crawling " + url + ": " + e.getMessage());
        }
    }

    public Set<String> getVisitedUrls() {
        return visitedUrls;
    }

    public String getHtmlContent(String url) {
        return htmlContents.get(url);
    }

    public String getTextContent(String url) {
        return textContents.get(url);
    }

    public Map<String, Set<String>> getUrlRelations() {
        return urlRelations;
    }
}
