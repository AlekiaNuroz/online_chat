import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ChatClient {
    private static boolean timestampEnabled = false;
    private static final Map<String, String> emojiMap = Map.ofEntries(
            Map.entry(":smile:", "😊"),
            Map.entry(":heart:", "❤️"),
            Map.entry(":laugh:", "😂"),
            Map.entry(":thumbsup:", "👍"),
            Map.entry(":sad:", "😢"),
            Map.entry(":angry:", "😠"),
            Map.entry(":star:", "⭐"),
            Map.entry(":fire:", "🔥"),
            Map.entry(":clap:", "👏"),
            Map.entry(":ok:", "👌"),
            Map.entry(":pray:", "🙏"),
            Map.entry(":wink:", "😉"),
            Map.entry(":cool:", "😎"),
            Map.entry(":cry:", "😭"),
            Map.entry(":surprised:", "😲"),
            Map.entry(":sleepy:", "😴"),
            Map.entry(":sweat:", "😅"),
            Map.entry(":thinking:", "🤔"),
            Map.entry(":hug:", "🤗"),
            Map.entry(":confused:", "😕"),
            Map.entry(":vomit:", "🤮"),
            Map.entry(":poop:", "💩"),
            Map.entry(":kiss:", "😘"),
            Map.entry(":brokenheart:", "💔"),
            Map.entry(":check:", "✅"),
            Map.entry(":x:", "❌"),
            Map.entry(":100:", "💯"),
            Map.entry(":boom:", "💥"),
            Map.entry(":eyes:", "👀"),
            Map.entry(":gift:", "🎁"),
            Map.entry(":tada:", "🎉"),
            Map.entry(":balloon:", "🎈"),
            Map.entry(":cake:", "🎂"),
            Map.entry(":coffee:", "☕"),
            Map.entry(":beer:", "🍺"),
            Map.entry(":pizza:", "🍕"),
            Map.entry(":soccer:", "⚽"),
            Map.entry(":music:", "🎵"),
            Map.entry(":phone:", "📱"),
            Map.entry(":globe:", "🌍"),
            Map.entry(":moon:", "🌙"),
            Map.entry(":sun:", "☀️"),
            Map.entry(":rainbow:", "🌈"),
            Map.entry(":snowflake:", "❄️"),
            Map.entry(":skull:", "💀"),
            Map.entry(":robot:", "🤖"),
            Map.entry(":warning:", "⚠️"),
            Map.entry(":lock:", "🔒"),
            Map.entry(":unlock:", "🔓"),
            Map.entry(":smirk:", "😏")
    );

    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12345);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            new Thread(() -> {
                try {
                    String serverMsg;
                    while ((serverMsg = reader.readLine()) != null) {
                        if (timestampEnabled) {
                            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                            System.out.println("[" + time + "] " + serverMsg);
                        } else {
                            System.out.println(replaceEmojis(serverMsg));
                        }
                    }
                    System.out.println("Server closed the connection.");
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                } finally {
                    System.out.println("Shutting down client.");
                    System.exit(0);
                }
            }).start();

            // Sending input
            String userInput;
            while ((userInput = console.readLine()) != null) {
                if (userInput.startsWith("/timestamp ")) {
                    String option = userInput.substring(11).trim().toLowerCase();
                    timestampEnabled = option.equals("on");
                    System.out.println("Timestamps " + (timestampEnabled ? "enabled" : "disabled") + ".");
                } else if (userInput.startsWith("/clear")) {
                    clearConsole();
                }
                else {
                    writer.println(replaceEmojis(userInput));
                }
            }

        } catch (IOException e) {
            System.out.println("Could not connect to server.");
        }
    }

    private static void clearConsole() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Failed to clear screen.");
        }
    }


    private static String replaceEmojis(String message) {
        for (Map.Entry<String, String> entry : emojiMap.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        return message;
    }
}
