package bot;

import com.google.gson.GsonBuilder;

public class TextBotMessage extends BotMessage {

    private String text;

    public TextBotMessage(String chatId, String text) {
        super(chatId);
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }

    @Override
    public void store() {
        // Optional: override if needed
    }
}
