package bot;

public class BotRunner {
    public static void main(String[] args) throws Exception {
        // Single bot instance
        AutoReplyBot bot = new AutoReplyBot();
        bot.registerBot(); // start bot once
        System.out.println("[INFO] Telegram bot started.");

        // Start notifier in its own thread
        Thread notifierThread = new Thread(() -> {
            try {
                Notifier notifier = new Notifier(bot);
                notifier.watchMessagesFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        notifierThread.start();

        System.out.println("[INFO] Notifier started...");
    }
}
