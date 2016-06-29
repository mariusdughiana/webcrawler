package test.webcrawler;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 *
 * Ideally this should run a new thread for each link processing but for the purpose of this exercise it will be
 * single threaded - hence slower...
 */
public class WebCrawler {

    private final Predicate<String> filter;
    private final Set<String> linksToAccess = new LinkedHashSet<>();
    private final Set<String> accessedLinks = new LinkedHashSet<>();
    private final Set<String> invalidLinks = new LinkedHashSet<>();


    public WebCrawler(String domain, Predicate<String> filter) {
        linksToAccess.add(domain);
        this.filter = filter;
    }


    public void run() throws IOException {
        processLinks();
        exportToFile();
    }

    private void processLinks() {
        while (!linksToAccess.isEmpty()) {

            String link = linksToAccess.iterator().next();
            linksToAccess.remove(link);
            Connection connection = Jsoup.connect(link);

            try {
                System.out.println(link);

                Document htmlDocument = connection.get();
                accessedLinks.add(link);
                Elements linksOnPage = htmlDocument.select("a[href]");
                Set<String> linksToBeAccessed = linksOnPage.stream()
                                                           .map(linkElement -> linkElement.absUrl("href"))
                                                           .filter(linkUrl -> filter.test(linkUrl))
                                                           .filter(linkUrl -> !accessedLinks.contains(linkUrl))
                                                           .filter(linkUrl -> !linksToAccess.contains(linkUrl))
                                                           .collect(Collectors.toSet());
                linksToAccess.addAll(linksToBeAccessed);
            } catch (IOException e) {
                invalidLinks.add(link);
            }

        }
    }

    private void  exportToFile() throws IOException {

        List<String> lines = new ArrayList<>();
        lines.add("Valid links: ");
        lines.addAll(accessedLinks);
        if (!invalidLinks.isEmpty()) {
            lines.add("");
            lines.add("");
            lines.add("");
            lines.add("");
            lines.add("Invalid links: ");
            lines.addAll(invalidLinks);
        }


        Path file = Paths.get("crawling_output.txt");
        Files.deleteIfExists(file);
        Files.createFile(file);
        Files.write(file, lines, Charset.forName("UTF-8"));

        System.out.println("Result exported to: " + file);

    }
}
