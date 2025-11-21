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
import com.google.gson.JsonSyntaxException;

public class Notifier {


private static final String MESSAGES_FILE = "C:/Users/Kusha/OneDrive/Desktop/oops/messages.txt";
private static final String STUDENTS_FILE = "C:/Users/Kusha/OneDrive/Desktop/oops/src/main/java/bot/data.xlsx";
private static final Gson gson = new Gson();

private final Map<String, String> studentToGuardian = new HashMap<>();
private final Map<String, String> studentIdToName = new HashMap<>();
private final AutoReplyBot bot;

// ANSI color codes
private static final String GREEN = "\u001B[32m";
private static final String YELLOW = "\u001B[33m";
private static final String RED = "\u001B[31m";
private static final String RESET = "\u001B[0m";

public Notifier(AutoReplyBot bot) throws Exception {
    this.bot = bot;
    bot.registerBot();
    loadExcelDatabase();
}

private void loadExcelDatabase() {
    try (FileInputStream fis = new FileInputStream(STUDENTS_FILE);
         Workbook workbook = new XSSFWorkbook(fis)) {

        Sheet sheet = workbook.getSheetAt(0);
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            String studentName = row.getCell(0).getStringCellValue();
            String studentId = String.valueOf((long) row.getCell(1).getNumericCellValue());
            String guardianId = String.valueOf((long) row.getCell(2).getNumericCellValue());
            studentToGuardian.put(studentId, guardianId);
            studentIdToName.put(studentId, studentName);
        }
        System.out.println(GREEN + "[INFO] Loaded student -> guardian mapping: " + studentToGuardian + RESET);

    } catch (IOException e) {
        System.out.println(RED + "[ERROR] Failed to load Excel: " + e.getMessage() + RESET);
        e.printStackTrace();
    }
}

public void watchMessagesFile() throws IOException, InterruptedException {
    Path path = Paths.get(MESSAGES_FILE).toAbsolutePath();
    Path dir = path.getParent();

    WatchService watchService = FileSystems.getDefault().newWatchService();
    dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

    System.out.println(GREEN + "[INFO] Watching " + MESSAGES_FILE + " for new logs..." + RESET);

    long lastSize = new File(MESSAGES_FILE).length();
    StringBuilder buffer = new StringBuilder();
    int openBraces = 0;

    while (true) {
        WatchKey key = watchService.take();

        for (WatchEvent<?> event : key.pollEvents()) {
            Path changed = (Path) event.context();
            if (changed.endsWith(path.getFileName())) {
                File file = new File(MESSAGES_FILE);
                long newSize = file.length();

                if (newSize > lastSize) {
                    try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                        raf.seek(lastSize);
                        String line;
                        while ((line = raf.readLine()) != null) {
                            line = line.trim();
                            if (line.isEmpty()) continue;

                            // Count braces for multi-line JSON
                            for (char c : line.toCharArray()) {
                                if (c == '{') openBraces++;
                                else if (c == '}') openBraces--;
                            }

                            buffer.append(line);

                            if (openBraces == 0 && buffer.length() > 0) {
                                processLogLine(buffer.toString());
                                buffer.setLength(0);
                            }
                        }
                    }
                    lastSize = newSize;
                }
            }
        }
        key.reset();
    }
}

private void processLogLine(String jsonLine) {
    try {
        System.out.println(GREEN + "[INFO] Detected new log: " + jsonLine + RESET);

        JsonObject json = gson.fromJson(jsonLine, JsonObject.class);
        if (json == null || !json.has("chatId")) {
            System.out.println(RED + "[WARN] Invalid JSON or missing chatId: " + jsonLine + RESET);
            return;
        }

        String studentId = json.get("chatId").getAsString();
        String note = json.has("text") ? json.get("text").getAsString() : "Location sent";

        String guardianId = studentToGuardian.get(studentId);
        String studentName = studentIdToName.get(studentId);

        if (guardianId != null && studentName != null) {
            String message = "Student: " + studentName + " | Note: " + note;
            System.out.println(YELLOW + "[INFO] Sending message to guardian " + guardianId + ": " + message + RESET);
            bot.sendText(guardianId, message);
            System.out.println(GREEN + "[INFO] Notification sent successfully!" + RESET);
        } else {
            System.out.println(RED + "[WARN] Guardian or student not found for studentId: " + studentId + RESET);
        }

    } catch (JsonSyntaxException e) {
        System.out.println(RED + "[ERROR] Malformed JSON, skipping line: " + jsonLine + RESET);
    } catch (Exception e) {
        System.out.println(RED + "[ERROR] Failed to process log line: " + jsonLine + RESET);
        e.printStackTrace();
    }
}

public static void main(String[] args) throws Exception {
    AutoReplyBot bot = new AutoReplyBot();
    Notifier notifier = new Notifier(bot);
    notifier.watchMessagesFile();
}

}
