
import feed.*;
import httpRequest.*;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.api.java.function.MapPartitionsFunction;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.MapFunction;
import parser.*;
import subscription.*;
import java.util.List;
import java.util.ArrayList;

public class FeedReaderMain {

    private static List<SingleSubscription> subscriptionList = new ArrayList<>();
    private static List<Feed> feeds = new ArrayList<>();

    private static void printHelp() {
        System.out.println("Please, call this program correctly: FeedReader [-ne]");
    }

    public static void main(String[] args) {
        System.out.println("************* FeedReader version 1.0 *************");
        // 1. Crear la SparkSession
        SparkSession spark = SparkSession
                .builder()
                .appName("FeedReaderSpark")
                .master("local[*]")
                .getOrCreate();
        spark.sparkContext().setLogLevel("WARN");
        SubscriptionParser sp = new SubscriptionParser();
        Subscription s = sp.jsonParser("config/subscriptions.json");
        if (s == null) {
            System.err.println("Error: Could not load subscriptions from ../config/subscriptions.json");
            spark.stop();
            return;
        }
        subscriptionList = s.getSubscriptionsList();
        List<String> urls = new ArrayList<>();
        Feed feedDS = null;

        for (SingleSubscription singSub : subscriptionList) {
            for (int i = 0; i < singSub.getUlrParamsSize(); i++) {
                if (singSub.getFeedToRequest(i) == null) {
                    System.out.print("error catching feed:" + i);
                } else {
                    urls.add(singSub.getFeedToRequest(i));
                }

            }
        }
        Dataset<String> urlsDS = spark.createDataset(urls, Encoders.STRING());
        Dataset<Feed> feedsDS = urlsDS.mapPartitions((MapPartitionsFunction<String, Feed>) iterator -> {
            httpRequest.httpRequester requester = new httpRequest.httpRequester();
            parser.RssParser parser = new parser.RssParser();
            List<Feed> results = new ArrayList<>();
            while (iterator.hasNext()) {
                String url = iterator.next();
                String feedContent = requester.getFeedRss(url);
                if (feedContent != null) {
                    Feed feed = parser.parser(feedContent);
                    if (feed != null) {
                        results.add(feed);
                    }
                }
            }
            return results.iterator();

        }, Encoders.javaSerialization(Feed.class));
        List<Feed> feeds = feedsDS.collectAsList();
        if (args.length == 0) {
            System.out.println("printing without -ne");
            for (Feed feedaux : feeds) {
                System.out.println("------------------------");
                System.out.println("|feed :" + feedaux.getSiteName() + "|");
                System.out.println("------------------------");
                feedaux.prettyPrint();
            }

        } else if (args.length == 1) {
            if (!args[0].equals("-ne")) {
                printHelp();
                return;
            }
            System.out.println("printing with -ne");

            Dataset<Article> articlesDS = feedsDS.flatMap((FlatMapFunction<Feed, Article>) feed -> feed.getArticleList().iterator(),
                    Encoders.javaSerialization(Article.class)
            );

            Dataset<Article> processedArticlesDS = articlesDS.map(
                    (MapFunction<Article, Article>) article -> {
                        namedEntity.heuristic.QuickHeuristic qh = new namedEntity.heuristic.QuickHeuristic();
                        article.computeNamedEntities(qh);
                        return article;
                    }, Encoders.javaSerialization(Article.class)
            );

            List<Article> listAricles = processedArticlesDS.collectAsList();
            for (Article article : listAricles) {
                article.prettyPrint();
                System.out.println("Named Entities:");
                for (namedEntity.NamedEntity ne : article.getList()) {
                    System.out.println(" - " + ne.getName() + " (" + ne.getCategory() + ") - Frequency: " + ne.getFrequency());
                }
                System.out.println("**********************************************************************************************");

            }

        } else {
            printHelp();
        }
        spark.stop(); // Cerrar la sesión al final
    }

}
