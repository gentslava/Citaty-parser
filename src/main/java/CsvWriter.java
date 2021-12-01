import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class CsvWriter { // Класс для записи в CSV файл
    private BufferedWriter writer;
    private String separator;

    protected CsvWriter(String fileName, String separator) throws IOException {
        writer = new BufferedWriter(new FileWriter(fileName));
        this.separator = separator;
    }

    protected void finalize() throws IOException {
        writer.close();
    }

    protected void writeToCSV (String... columns) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String column : columns) {
            sb.append(column);
            sb.append(separator);
        }
        sb.append('\n');
        writer.write(sb.toString());
    }
}
