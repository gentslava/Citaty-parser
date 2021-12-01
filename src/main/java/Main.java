import java.text.SimpleDateFormat;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        List<String> pageList = new ArrayList<String>();
        pageList.add("https://citaty.info/man/faina-ranevskaya");
        pageList.add("https://citaty.info/man/mark-tven");
        pageList.add("https://citaty.info/man/oskar-uaild");
        pageList.add("https://citaty.info/man/omar-haiyam");
        pageList.add("https://citaty.info/man/siddhartha-gautama-budda");
        pageList.add("https://citaty.info/man/vinsent-van-gog");
        pageList.add("https://citaty.info/man/zigmund-freid");
        pageList.add("https://citaty.info/man/kurt-kobein");
        pageList.add("https://citaty.info/man/koko-shanel");
        Map<String, List<String>> parentUrls = new HashMap<String, List<String>>();
        parentUrls.put("", pageList);
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            Date date = new Date();
            System.out.println(formatter.format(date) + " - Индексация начата");
            Crawler my_crawler = new Crawler("Done/citaty.db", false);
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
