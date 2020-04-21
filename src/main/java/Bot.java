import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.MessageCreateEvent;

import java.util.HashMap;
import java.util.Map;

public class Bot {

  private static final Map<String, Command> commands = new HashMap<>();
  private static String prefix = "!", content = "";

  static {
    commands.put("ping", event -> event.getMessage()
            .getChannel().block()
            .createMessage("Pong!").block());

    commands.put("prefix", event -> setPrefix(event));
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

  /**
   * Change the prefix for the bot.
   *
   * @param event the event triggering this call
   */
  private static void setPrefix(Event event) {

    try {
      prefix = content.substring(prefix.length() + "prefix ".length());

      ((MessageCreateEvent)event).getMessage()
              .getChannel().block().createMessage("Prefix set to " + prefix).block();

    } catch (StringIndexOutOfBoundsException e) {
      ((MessageCreateEvent)event).getMessage()
              .getChannel().block().createMessage("Error setting prefix! Kept as: " + prefix).block();
    }

    }
}
