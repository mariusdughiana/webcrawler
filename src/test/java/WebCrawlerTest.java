import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import test.webcrawler.WebCrawler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class WebCrawlerTest {

    private static final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(8080).bindAddress("localhost"));

    private static final Path FILE = Paths.get("crawling_output.txt");



    @BeforeClass
    public static void startServer() {
        wireMockServer.start();
        configureFor("localhost", 8080);
    }

    @AfterClass
    public static void stopServer() throws IOException {
        wireMockServer.stop();
        Files.deleteIfExists(FILE);
    }

    @Test
    public void runWithNoLinks() throws Exception {
        givenThat(get(urlEqualTo("/"))
                .willReturn(
                        aResponse().withStatus(200)
                                   .withHeader("Content-Type", "text/html")
                                   .withBody( "<html>Page1</html>")));

        WebCrawler webCrawler = new WebCrawler("http://localhost:8080/", s -> s.contains("http://localhost:8080"));
        webCrawler.run();

        List<String> lines = Files.readAllLines(FILE);

        Assert.assertEquals(2, lines.size());
        Assert.assertEquals("Valid links: ", lines.get(0));
        Assert.assertEquals("http://localhost:8080/", lines.get(1));

    }

    @Test
    public void runWithNestedLinks() throws Exception {
        givenThat(get(urlEqualTo("/"))
                .willReturn(
                        aResponse().withStatus(200)
                                   .withHeader("Content-Type", "text/html")
                                   .withBody( "<html>" +
                                                "<body> " +
                                                    "<a href=\"http://localhost:8080/page1\">Page1</a>" +
                                                "</body>" +
                                                "</html>")));
        givenThat(get(urlEqualTo("/page1"))
                .willReturn(
                        aResponse().withStatus(200)
                                   .withHeader("Content-Type", "text/html")
                                   .withBody( "<html>" +
                                                "<body> " +
                                                    "<a href=\"http://localhost:8080/\">Home</a>" +
                                                "</body>" +
                                                "</html>")));

        WebCrawler webCrawler = new WebCrawler("http://localhost:8080/", s -> s.contains("http://localhost:8080"));
        webCrawler.run();

        List<String> lines = Files.readAllLines(FILE);

        Assert.assertEquals(3, lines.size());
        Assert.assertEquals("Valid links: ", lines.get(0));
        Assert.assertEquals("http://localhost:8080/", lines.get(1));
        Assert.assertEquals("http://localhost:8080/page1", lines.get(2));

    }

    @Test
    public void runWithInvalidLinks() throws Exception {
        givenThat(get(urlEqualTo("/"))
                .willReturn(
                        aResponse().withStatus(200)
                                   .withHeader("Content-Type", "text/html")
                                   .withBody( "<html>" +
                                                "<body> " +
                                                    "<a href=\"http://localhost:8080/page2\">Page2</a>" +
                                                "</body>" +
                                                "</html>")));
        WebCrawler webCrawler = new WebCrawler("http://localhost:8080/", s -> s.contains("http://localhost:8080"));
        webCrawler.run();

        List<String> lines = Files.readAllLines(FILE);

        Assert.assertEquals(8, lines.size());
        Assert.assertEquals("Valid links: ", lines.get(0));
        Assert.assertEquals("http://localhost:8080/", lines.get(1));
        Assert.assertEquals("Invalid links: ", lines.get(6));
        Assert.assertEquals("http://localhost:8080/page2", lines.get(7));

    }

}