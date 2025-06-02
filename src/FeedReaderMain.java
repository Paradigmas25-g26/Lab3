
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
                urls.add(singSub.getFeedToRequest(i));
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

        // 3. Extraer todos los artículos de todos los feeds
        Dataset<Article> articlesDS = feedsDS.flatMap((FlatMapFunction<Feed, Article>) feed -> feed.getArticleList().iterator(),
                Encoders.javaSerialization(Article.class)
        );

        // 4. Procesar entidades nombradas en paralelo
        Dataset<Article> processedArticlesDS = articlesDS.map((MapFunction<Article, Article>) article -> {
            namedEntity.heuristic.QuickHeuristic qh = new namedEntity.heuristic.QuickHeuristic();
            article.computeNamedEntities(qh);
            return article;
        }, Encoders.javaSerialization(Article.class));

        // 5. Recoger los resultados si los necesitas en memoria
        List<Article> processedArticles = processedArticlesDS.collectAsList();

        // Aquí puedes imprimir, guardar o seguir procesando los artículos
        // processedArticles.forEach(System.out::println);
        // for (SingleSubscription singSub : subscriptionList) {
        //     // Llamar al httpRequester para obtener el feed del servidor
        //     httpRequester http = new httpRequester();
        //     if (!singSub.getUrlType().equals("rss")) {
        //         // si entramos es porque NO es un rss
        //         System.out.println("Unable to parse feeds different from RSS format.");
        //         System.out.println("Attempted to parse feed type: " + singSub.getUrlType());
        //         continue;
        //     }
        //     for (int i = 0; i < singSub.getUlrParamsSize(); i++) {
        //         String urlFeed = singSub.getFeedToRequest(i);
        //         String feedContent = http.getFeedRss(urlFeed);
        //         RssParser rssParser = new RssParser();
        //         feed = rssParser.parser(feedContent);
        //         if (feed != null) {
        //             feeds.add(feed);
        //         }
        //     }
        // }
        // if (args.length == 0) {
        //     System.out.println("printing without -ne");
        //     for (Feed feedaux : feeds) {
        //         feedaux.prettyPrint();
        //         System.out.println("------------------------");
        //         System.out.println("|feed :" + feedaux.getSiteName() + "|");
        //         System.out.println("------------------------");
        //     }
        // } else if (args.length == 1) {
        //     if (!args[0].equals("-ne")) {
        //         printHelp();
        //         return;
        //     }
        //     System.out.println("printing with -ne");
        //     QuickHeuristic qh = new QuickHeuristic();
        //     for (Feed feedaux : feeds) {
        //         for (Article a : feedaux.getArticleList()) {
        //             a.computeNamedEntities(qh);
        //         }
        //         feedaux.addAllNamedEntities();
        //         feedaux.prettyPrintAllNamedEntities();
        //         System.out.println("------------------------");
        //         System.out.println("|feed :" + feedaux.getSiteName() + "|");
        //         System.out.println("------------------------");
        //     }
        //} else {
        //  printHelp();
        //}
        spark.stop(); // Cerrar la sesión al final
    }
}
