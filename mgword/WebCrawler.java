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
    // 爬取监听器接口，用于通知爬取过程中的URL
    private CrawlListener crawlListener;

    // 用于存储已访问的URL集合
    private final Set<String> visitedUrls = new HashSet<>();

    // 存储每个URL和其链接关系
    private final Map<String, Set<String>> urlRelations = new HashMap<>();

    // 线程池执行器，用于管理并发爬取任务
    private final ExecutorService executorService;

    // 运行标志，控制爬虫的运行状态
    private volatile boolean running = true;

    // 存储每个URL的HTML内容
    private final Map<String, String> htmlContents = new HashMap<>();

    // 存储每个URL的纯文本内容
    private final Map<String, String> textContents = new HashMap<>();

    // 爬取监听器接口定义
    public interface CrawlListener {
        void onCrawling(String url);
    }

    // 构造方法，初始化线程池
    public WebCrawler(int numThreads) {
        executorService = Executors.newFixedThreadPool(numThreads);
    }

    // 设置爬取监听器
    public void setCrawlListener(CrawlListener crawlListener) {
        this.crawlListener = crawlListener;
    }

    // 开始爬取，提交初始URL任务
    public void startCrawling(String startUrl) {
        running = true;
        executorService.submit(() -> crawl(startUrl));
    }

    // 停止爬取，关闭线程池
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

    // 爬取指定URL
    private void crawl(String url) {
        //检查运行状态和已访问URL
        if (!running || visitedUrls.contains(url)) {
            return;
        }
        visitedUrls.add(url);
        //通知监听器当前正在爬取的URL
        if (crawlListener != null) {
            crawlListener.onCrawling(url);
        }
        try {
            // 使用Jsoup连接并获取网页内容
            Document document = Jsoup.connect(url).get();
            //Jsoup的connect方法连接到指定的URL，并使用get方法获取整个HTML文档
            //这个方法将返回一个Document对象
            System.out.println("Crawling: " + url);

            // 存储HTML和纯文本内容
            htmlContents.put(url, document.html());
            textContents.put(url, document.text());//获取去除HTML标签后的纯文本内容

            // 查找并处理页面中的所有链接
            Elements links = document.select("a[href]");//<a>标签即链接
            Set<String> linkedUrls = urlRelations.getOrDefault(url, new HashSet<>());
            for (Element link : links) {
                String nextUrl = link.absUrl("href");
                if (nextUrl.startsWith("http")) {
                    linkedUrls.add(nextUrl);
                    if (!visitedUrls.contains(nextUrl)) {
                        executorService.submit(() -> crawl(nextUrl));
                        //递归调用crawl方法，爬取下一个URL
                    }
                }
            }
            urlRelations.put(url, linkedUrls);
            //URL与其链接关系存储在urlRelations
        } catch (IOException e) {
            System.err.println("Error crawling " + url + ": " + e.getMessage());
        }
    }

    // 获取已访问的URL集合
    public Set<String> getVisitedUrls() {
        return visitedUrls;
    }

    // 获取指定URL的HTML内容
    public String getHtmlContent(String url) {
        return htmlContents.get(url);
    }

    // 获取指定URL的纯文本内容
    public String getTextContent(String url) {
        return textContents.get(url);
    }

    // 获取URL之间的关系映射
    public Map<String, Set<String>> getUrlRelations() {
        return urlRelations;
    }
}
