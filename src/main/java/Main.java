import java.text.SimpleDateFormat;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        List<String> pageList = new ArrayList<String>();
        pageList.add("https://citaty.info/man");
        Map<String, List<String>> parentUrls = new HashMap<String, List<String>>();
        parentUrls.put("", pageList);
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            Date date = new Date();
            System.out.println(formatter.format(date) + " - Индексация начата");
            Crawler my_crawler = new Crawler("citaty.db", false);
            my_crawler.crawl(parentUrls, 100, "https://citaty.info/man");
            my_crawler.finalize();
            date = new Date();
            System.out.println(formatter.format(date) + " - Индексация завершена");
        } catch (Exception e) {
            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            System.out.println(formatter.format(date) + " - Индексация завершена с ошибкой:");
            e.printStackTrace();
        }
    }
}
