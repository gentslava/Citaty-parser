import java.text.SimpleDateFormat;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        boolean searching_or_indexing = true;
        if (searching_or_indexing) {
            List<String> pageList = new ArrayList<String>();
            pageList.add("https://citaty.info/");
            Map<String, List<String>> parentUrls = new HashMap<String, List<String>>();
            parentUrls.put("", pageList);
            try {
                Crawler my_crawler = new Crawler("citaty.db", false);
                my_crawler.dropDB();
                my_crawler.createIndexTables();
                my_crawler.crawl(parentUrls, 100, "https://citaty.info/");
                my_crawler.finalize();
                Date date = new Date();
                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
                System.out.println(formatter.format(date) + " - Индексация завершена");
            } catch (Exception e) {
                Date date = new Date();
                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
                System.out.println(formatter.format(date) + " - Индексация завершена с ошибкой:");
                e.printStackTrace();
            }
        } else {
            try {
                Searcher mySearcher = new Searcher("citaty.db");
                String mySearchQuery = "Новосибирск карантин";
                mySearcher.search(mySearchQuery);
                mySearcher.finalize();
            } catch (Exception e) {
                System.out.println("Поиск завершен с ошибкой:");
                e.printStackTrace();
            }
        }
    }
}
