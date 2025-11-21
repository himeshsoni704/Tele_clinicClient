package bot;

public class BotRunner {

    public static void main(String[] args) {
        try {
            // Create one bot instance
            AutoReplyBot bot = new AutoReplyBot();

            // Start Telegram bot in a separate thread
            Thread botThread = new Thread(() -> {
                try {
                    bot.registerBot();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            botThread.start();

            // Start notifier in another thread
            Notifier notifier = new Notifier(bot);
            Thread notifierThread = new Thread(() -> {
                try {
                    notifier.watchMessagesFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            notifierThread.start();

            System.out.println("AutoReplyBot and Notifier are running...");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
