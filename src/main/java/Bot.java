import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.MessageChannel;
import reactor.core.publisher.Mono;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Bot {

  private static final Map<String, Command> commands = new HashMap<>();
  private static String prefix = "!", content = "";
  private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");

  static {
    commands.put("ping", event -> Objects.requireNonNull(event.getMessage()
            .getChannel().block()).createMessage("Pong!").block());

    commands.put("prefix ", Bot::setPrefix);

    commands.put("localtime", Bot::tellTime);
  }

  /**
   * Main method.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    final DiscordClient client = DiscordClientBuilder.create(args[0]).build();

    client.getEventDispatcher().on(MessageCreateEvent.class)
            .subscribe(event -> {
              content = event.getMessage().getContent().orElse("");
              for (final Map.Entry<String, Command> entry : commands.entrySet()) {
                if (content.startsWith(prefix + entry.getKey())) {
                  entry.getValue().execute(event);
                  break;
                }
              }
            });

    client.login().block();
  }


  private static void sendMessage(Mono<MessageChannel> channel, String message) {
    Objects.requireNonNull(channel.block()).createMessage(message).block();
  }

  /**
   * Change the prefix for the bot.
   *
   * @param event the event triggering this call
   */
  private static void setPrefix(Event event) {
    try {
      prefix = content.substring(prefix.length() + "prefix ".length());
      sendMessage(((MessageCreateEvent)event).getMessage().getChannel(),
              "Prefix set to " + prefix);

      System.out.println( "\\u" + Integer.toHexString(prefix.charAt(0) | 0x10000).substring(1) );
      //System.out.printf("character: %d\n", (int)prefix.charAt(0));

    } catch (StringIndexOutOfBoundsException e) {
      sendMessage(((MessageCreateEvent)event).getMessage().getChannel(),
              "Error setting prefix! Kept as: " + prefix);
    }
  }

  private static void tellTime(MessageCreateEvent event) {
    sendMessage(event.getMessage().getChannel(),
            "Bot's local time is: " + dtf.format(LocalDateTime.now()));
  }

}
