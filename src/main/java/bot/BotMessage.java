package bot;

public abstract class BotMessage implements Storable {
    private String chatId;

    public BotMessage(String chatId) {
        this.chatId = chatId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    // All subclasses must implement toJson
    public abstract String toJson();

    
    @Override
    public void store() {
        
    }
}
