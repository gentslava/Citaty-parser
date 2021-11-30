import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class CsvWriter { // Класс для записи в CSV файл
    private BufferedWriter writer;

    protected CsvWriter(String fileName) throws IOException {
        writer = new BufferedWriter(new FileWriter(fileName));
    }

    protected void finalize() throws IOException {
        writer.close();
    }

    protected void writeToCSV (String... columns) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String column : columns) {
            sb.append(column);
            sb.append(';');
        }
        sb.append('\n');
        writer.write(sb.toString());
    }
}
