import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.text.SimpleDateFormat;

public class Crawler {
    private Connection conn; // соединение с БД
    private boolean debug;
    private CsvWriter csvWriter;

    protected Crawler(String fileName, boolean debug) throws SQLException, IOException {
        this.debug = debug;
        if (debug) System.out.println("Конструктор");

        String db_URL = "jdbc:sqlite:" + fileName;
        this.conn = DriverManager.getConnection(db_URL);
        this.conn.setAutoCommit(true);
        dropDB();
        createIndexTables();

        csvWriter = new CsvWriter("test.csv");
        csvWriter.writeToCSV("Author", "Text", "Tags");
    }

    protected void finalize() throws SQLException, IOException {
        conn.close();
        csvWriter.finalize();
    }

    /* Удаление таблиц в БД */
    private void dropDB() throws SQLException {
        if (debug) System.out.println("Удаление таблиц");

        Statement statement = this.conn.createStatement();
        String request;
        request = "DROP TABLE IF EXISTS URLList;";
        if (debug) System.out.println("\t" + request);
        statement.execute(request);
    }

    /* Инициализация таблиц в БД */
    private void createIndexTables() throws SQLException {
        if (debug) System.out.println("Создание таблиц");

        Statement statement = this.conn.createStatement();
        String request;
        // Создание таблицы URLList в БД
        request = "CREATE TABLE IF NOT EXISTS URLList(rowId INTEGER PRIMARY KEY AUTOINCREMENT, URL TEXT NOT NULL, isIndexed BOOLEAN DEFAULT FALSE);";
        if (debug) System.out.println("\t" + request);
        statement.execute(request);
    }

    /* Вспомогательный метод для получения идентификатора, добавления и обновления записи */
    private int SelectInsertUpdate(String table, String field, String value, int num, boolean createNew, boolean updateVal) throws Exception {
        Statement statement = this.conn.createStatement(); // получить Statement для выполнения SQL-запроса
        String request;
        if (field.equals("LAST_INSERT_ROWID()")) { // получение Id последнего добавленного элемента
            request = "SELECT LAST_INSERT_ROWID();";
            if (debug) System.out.println("\t\t\t" + request);
            ResultSet resultRow = statement.executeQuery(request);
            if (resultRow.next()) return resultRow.getInt("LAST_INSERT_ROWID()");
        }
        if (!createNew && !updateVal) { // запрос Id элемента, отвечающего условиям
            request = "SELECT rowId FROM " + table + " WHERE ";
            String fields[] = field.split(", ");
            String values[] = value.split(", ");
            for (int i = 0; i < num; i++) {
                request += fields[i] + " = " + values[i];
                if (i + 1 != num) request += " AND ";
            }
            request += ";";
            if (debug) System.out.println("\t\t\t" + request);
            ResultSet resultRow = statement.executeQuery(request);
            if (resultRow.next()) return resultRow.getInt("rowId");
        } else if (!updateVal) { // занесение нового элемента в таблицу
            request = "INSERT INTO " + table + " (" + field + ") VALUES (" + value + ");";
            if (debug) System.out.println("\t\t\t" + request);
            statement.execute(request);
            return SelectInsertUpdate("", "LAST_INSERT_ROWID()", "", 0, false, false);
        } else { // обновления значений полей определенного элемента
            request = "UPDATE " + table + " SET ";
            String fields[] = field.split(", ");
            String values[] = value.split(", ");
            int i;
            for (i = 0; i < num; i++) {
                request += fields[i] + " = " + values[i];
                if (i + 1 != num) request += ", ";
            }
            request += " WHERE " + fields[i] + " = " + values[i] + ";";
            if (debug) System.out.println("\t\t\t" + request);
            statement.execute(request);
        }
        return -1;
    }

    /* Проиндексирован ли URL */
    private boolean isIndexed(String URL) throws Exception {
        Statement statement = this.conn.createStatement();
        String request = "SELECT isIndexed FROM URLList WHERE URL = '" + URL + "';";
        if (debug) System.out.println("\t\t\t" + request);
        ResultSet resultRow = statement.executeQuery(request);
        if (resultRow.next() && resultRow.getBoolean("isIndexed")) return true;
        else return false;
    }

    /* Вспомогательный метод для формирования ссылки на следующую страницу */
    private String generateLink(String URL, String nextUrl) {
        nextUrl = nextUrl.replace('\\', '/');
        if (nextUrl.startsWith("http") && nextUrl.length() > 6) { // абсолютная ссылка начинается с http или https
            URL = "";
        } else if (nextUrl.startsWith(".")) { // относительная ссылка, для перемещения по каталогам
            URL = URL.substring(0, URL.lastIndexOf("/"));
            while (nextUrl.contains("/") && nextUrl.substring(0, nextUrl.indexOf("/")).equals("..")) { // перемещение на каталог вверх
                URL = URL.substring(0, URL.lastIndexOf("/") + 1);
                nextUrl = nextUrl.substring(nextUrl.indexOf("/") + 1);
            }
            if (nextUrl.startsWith(".")) { // текущий каталог
                nextUrl = nextUrl.substring(nextUrl.indexOf("/"));
            }
        } else if (nextUrl.startsWith("//")) { // ссылка относительно протокола текущей страницы
            URL = URL.substring(0, URL.indexOf("//"));
        } else if (nextUrl.startsWith("/")) { // ссылка относительно домена текущей страницы
            int endIndex = URL.indexOf("/", URL.indexOf("//") + 2);
            if (endIndex != -1)
                URL = URL.substring(0, URL.indexOf("//") + 2) + URL.substring(URL.indexOf("//") + 2, endIndex);
        } else { // невалидная ссылка
            URL = "";
            nextUrl = "";
        }
        nextUrl = URL + nextUrl;
        while (nextUrl.endsWith("/")) nextUrl = nextUrl.substring(0, nextUrl.length() - 1); // удаление "/" на конце ссылки
        if (nextUrl.contains("://www.")) nextUrl = nextUrl.replace("://www.", "://"); // удаление "www" из ссылки
        return nextUrl;
    }

    /* Очистка страницы от комментариев и тегов noindex */
    protected void removeComments(Node node) {
        for (int i = 0; i < node.childNodeSize();) {
            Node child = node.childNode(i);
            if (child.nodeName().equals("#comment")) {
                if (child.outerHtml().equals("<!--noindex-->")) // удаление тега noindex Яндекса
                    while (!child.outerHtml().equals("<!--/noindex-->")) {
                        child.remove();
                        child = node.childNode(i);
                    }
                child.remove();
            }
            else {
                removeComments(child);
                i++;
            }
        }
    }

    protected Document getDocument(String URL) throws IOException {
        Document html_doc;
        if (debug) System.out.println("\t\tПопытка открыть " + URL);
        try {
            html_doc = Jsoup.connect(URL).get(); // получить HTML-код страницы
            if (debug) System.out.print("\t\tWEB файл ");
        } catch (java.net.MalformedURLException e) { // если не удалось, страница может быть локальным файлом
            if (debug) System.out.print("\t\tЛокальный файл ");
            String fileName = URL.substring(7);
            if (debug) System.out.println(fileName);
            File input = new File(fileName);
            html_doc = Jsoup.parse(input, "UTF-8");
        } catch (Exception e) {
            // обработка исключений при ошибке запроса содержимого страницы
            System.out.println("\t\tОшибка. " + URL);
            System.out.print(e);
            return null;
        }
        return html_doc;
    }

    /* Индексирование страницы */
    private List<String> addToIndex(String URL, String URLstart) throws Exception {
        if (debug) System.out.println("\t\tИндексирование страницы");
        URL = URL.replace('\\', '/');
        List<String> nextUrlSet = null;

        // Если страница не проиндексирована
        if (!isIndexed(URL)) {
            int URLId = SelectInsertUpdate("URLList", "URL", "'" + URL + "'", 1, false, false);
            if (URLId != -1) SelectInsertUpdate("URLList", "isIndexed, URL", "TRUE, '" + URL + "'", 1, false, true);
            else SelectInsertUpdate("URLList", "URL, isIndexed", "'" + URL + "', TRUE", 1, true, false);

            Document html_doc = getDocument(URL);
            if (html_doc != null) {
                if (debug) System.out.println("открыт " + URL);

                Element content = html_doc.select("#content").first();
                removeComments(content);
                Elements articles = content.select("article");
                // Обработка статей
                for (Element article : articles) {
                    String author = article.select("a[title=\"Автор цитаты\"]").text();
                    String quote = article.select("p").first().text();
                    String tags = "";
                    Elements tagsHTML = article.select(".node__topics a");
                    for (Element tag : tagsHTML) tags += tag.text() + ", ";
                    if (tags.length() > 0) tags = tags.substring(0, tags.length() - 2);
                    csvWriter.writeToCSV(author, quote, tags);
                }

                // Получить все ссылки на следующие страницы
                Elements links = html_doc.select(".pager a");
                nextUrlSet = new ArrayList<String>(); // множество уникальных ссылок
                for (Element link : links) {
                    String nextUrl = link.attr("href");
                    nextUrl = generateLink(URL, nextUrl); // Проверка соответствия ссылок требованиям
                    if (!nextUrl.startsWith(URLstart)) {
                        if (debug) System.out.println("\t\t\tссылка ведет на другой сайт " + nextUrl);
                    } else {
                        if (nextUrl.length() == 0) {
                            if (debug) System.out.println("\t\t\tссылка не валидная - пропустить " + nextUrl);
                        } else if (SelectInsertUpdate("URLList", "URL", "'" + nextUrl + "'", 1, false, false) == -1) {
                            if (debug) System.out.println("\t\t\tссылка валидная - добавить " + nextUrl);
                            nextUrlSet.add(nextUrl);
                            SelectInsertUpdate("URLList", "URL", "'" + nextUrl + "'", 1, true, false);
                        }
                    }
                }
            }
            if (debug) System.out.println("\t\tСтраница проиндексирована");
        }
        return nextUrlSet;
    }

    /* Метод сбора данных.
     * Начиная с заданного списка страниц, выполняет поиск в ширину
     * до заданной глубины, индексируя все встречающиеся по пути страницы */
    protected void crawl(Map<String, List<String>> parentUrls, int maxDepth, String URLstart) throws Exception {
        if (debug) System.out.println("Начало обхода всех страниц");

        // для каждого уровня глубины currDepth до максимального maxDepth
        for (int currDepth = 0; currDepth < maxDepth; currDepth++) {
            if (debug) System.out.println("\t== Глубина " + (currDepth + 1) + " ==");
            Map<String, List<String>> nextParentUrls = new HashMap<String, List<String>>();

            for (int i = 0; i < parentUrls.size(); i++) {
                String parentUrl = (String) parentUrls.keySet().toArray()[i];
                int N = parentUrls.get(parentUrl) == null ? 0 : parentUrls.get(parentUrl).size(); // количество элементов, которые предстоит обойти в списке URLList

                // обход всех URL на теущей глубине
                for (int j = 0; j < N; j++) {
                    List<String> URLList = parentUrls.get(parentUrl);
                    String URL = URLList.get(j); // получить URL-адрес из списка

                    Date date = new Date();
                    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
//                    if (debug)
                        System.out.println("\t" + (j + 1) + "/" + URLList.size() + " - " + formatter.format(date) + " - Индексируем страницу " + URL);

                    nextParentUrls.put(URL, addToIndex(URL, URLstart));
                    // конец обработки одной ссылки URL
                }
            }
            // заменить содержимое parentUrls на nextParentUrls
            parentUrls = nextParentUrls;

            // конец обхода ссылкок parentUrls на текущей глубине
        }
        if (debug) System.out.println("\tВсе страницы проиндексированы");
    }
}