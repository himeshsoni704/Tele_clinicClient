package bot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class Notifier {

    private static final String MESSAGES_FILE = "C:/Users/Kusha/OneDrive/Desktop/oops/messages.txt";
    private static final String STUDENTS_FILE = "C:/Users/Kusha/OneDrive/Desktop/oops/src/main/java/bot/data.xlsx";

    private static final Gson gson = new Gson();

    // Mappings for student info
    private final Map<String, String> studentToGuardian = new HashMap<>();
    private final Map<String, String> studentIdToName = new HashMap<>();

    private final AutoReplyBot bot; // your existing bot instance

    public Notifier(AutoReplyBot bot) {
        this.bot = bot;
        loadExcelDatabase();
    }

    // Load student -> guardian and student name mappings
    private void loadExcelDatabase() {
        try (FileInputStream fis = new FileInputStream(STUDENTS_FILE);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // skip header
                String studentName = row.getCell(0).getStringCellValue();
                String studentId = String.valueOf((long) row.getCell(1).getNumericCellValue());
                String guardianId = String.valueOf((long) row.getCell(2).getNumericCellValue());

                studentToGuardian.put(studentId, guardianId);
                studentIdToName.put(studentId, studentName);
            }
            System.out.println("Loaded student -> guardian mapping: " + studentToGuardian);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Watch the messages.txt file for new logs
    public void watchMessagesFile() throws IOException, InterruptedException {
        Path path = Paths.get(MESSAGES_FILE).toAbsolutePath();
        Path dir = path.getParent();

        WatchService watchService = FileSystems.getDefault().newWatchService();
        dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

        System.out.println("Watching " + MESSAGES_FILE + " for new logs...");

        long lastSize = new File(MESSAGES_FILE).length();

        while (true) {
            WatchKey key = watchService.take();

            for (WatchEvent<?> event : key.pollEvents()) {
                Path changed = (Path) event.context();
                if (changed.endsWith(path.getFileName())) {
                    File file = new File(MESSAGES_FILE);
                    long newSize = file.length();

                    if (newSize > lastSize) { // new data added
                        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                            raf.seek(lastSize);
                            String line;
                            while ((line = raf.readLine()) != null) {
                                processLogLine(line);
                            }
                        }
                        lastSize = newSize;
                    }
                }
            }
            key.reset();
        }
    }

    // Process a single JSON log line and notify guardian
    private void processLogLine(String line) {
        try {
            JsonObject json = gson.fromJson(line, JsonObject.class);
            String studentId = json.get("chatId").getAsString(); // matches Student Telegram ID
            String note = json.has("text") ? json.get("text").getAsString() : "Location sent";

            String guardianId = studentToGuardian.get(studentId);
            String studentName = studentIdToName.get(studentId);

            if (guardianId != null && studentName != null) {
                String message = "Student: " + studentName + " | Note: " + note;
                System.out.println("Sending message to guardian " + guardianId + ": " + message);

                // Send via Telegram bot
                bot.sendText(guardianId, message);

                // TODO: flag entry in DB / update frontend
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Start the notifier
    public static void main(String[] args) throws IOException, InterruptedException {
        AutoReplyBot bot = new AutoReplyBot();
        Notifier notifier = new Notifier(bot);
        notifier.watchMessagesFile();
    }
}
