package test.webcrawler;

import java.io.IOException;

public class WebCrawlerMain {

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            throw new RuntimeException("Is mandatory to provide the domain for web crawling...");
        }
        String domain = args[0];
        new WebCrawler(domain, s -> s.contains(domain)).run();
    }
}
