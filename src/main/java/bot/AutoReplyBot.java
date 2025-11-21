package bot;
/**IMPLEMENTATION NOTES 
 * WE'LL NEED A TELEGRAM ID ALSO IN THE DATABASE TO IDENTIFY USERS
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class AutoReplyBot extends TelegramLongPollingBot {

    private static final String FILE_PATH = "messages.txt";

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage())
         return;
        Message message = update.getMessage();
        String chatId = message.getChatId().toString();

        // Store message
        storeMessage(message);

        // Handle start command
        if (message.hasText() && message.getText().equals("/start")) {
            sendWelcome(chatId);
            return;
        }

        // Handle reply
        String reply = "Notifying clinic";
        if (message.hasText() && message.getText().equalsIgnoreCase("help")) {
            reply = "Initiating help protocol. Clinic has been notified.";
        } else if (message.hasLocation()) {
            Location loc = message.getLocation();
            reply = "Received your location: \n Latitude=" + loc.getLatitude() +
                    ", Longitude=" + loc.getLongitude();
        }

        sendText(chatId, reply);
    }

    public void registerBot() throws Exception {
    TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
    botsApi.registerBot(this);
    System.out.println("AutoReplyBot started successfully...");
}


    private void sendWelcome(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Hi! \n Use the buttons below to send messages or location.");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Ask Help"));

        KeyboardRow row2 = new KeyboardRow();
        KeyboardButton locationButton = new KeyboardButton("Share Location");
        locationButton.setRequestLocation(true);
        row2.add(locationButton);

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        keyboardRows.add(row1);
        keyboardRows.add(row2);
        keyboard.setKeyboard(keyboardRows);

        message.setReplyMarkup(keyboard);

        try { execute(message); }
         catch (TelegramApiException e) 
         { System.out.println("Error sending welcome message: " + e.getMessage()); }
    }

    public void sendText(String chatId, String text) { //bCOZ THIS EVEN NOTIFIER USING
        SendMessage message = new SendMessage(chatId, text);
        try { execute(message); } catch (TelegramApiException e) { e.printStackTrace(); }
    }

    private void storeMessage(Message message) {
        try (FileWriter writer = new FileWriter(new File(FILE_PATH), true)) {
            Storable botMsg;
            if (message.hasLocation()) {
                botMsg = new LocationBotMessage(
                        message.getChatId().toString(),
                        message.getLocation().getLatitude(),
                        message.getLocation().getLongitude()
                );
            } else {
                botMsg = new TextBotMessage(
                        message.getChatId().toString(),
                        message.getText()
                );
            }
            writer.write(botMsg.toJson() + System.lineSeparator());
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public String getBotUsername() {
        return "med_help_bits_bot";
    }

    @Override
    public String getBotToken() {
        return "8509775935:AAH64ApWxy7TPfNWpMSWqN_NfNSFt8FvM1k";
    }

    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new AutoReplyBot());
            System.out.println("TeleChat started successfully...");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
