import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.voice.AudioProvider;
import reactor.core.publisher.Mono;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;

public class Bot {

  private static final Map<String, Command> commands = new HashMap<>();
  private static final AudioPlayerManager audioMan;
  private static final AudioPlayer player;
  private static final AudioProvider audioPro;
  private static final TrackScheduler trackSched;
  private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
  private static String prefix = "!", content = "";
  private static VoiceChannel joined = null;

  static {
    //initialize objects required for audio
    audioMan = new DefaultAudioPlayerManager();
    audioMan.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
    AudioSourceManagers.registerRemoteSources(audioMan);
    player = audioMan.createPlayer();
    audioPro = new LavaPlayerAudioProvider(player);
    trackSched = new TrackScheduler(player);

    //add commands to our command map
    commands.put("ping", event -> Objects.requireNonNull(event.getMessage()
            .getChannel().block()).createMessage("Pong!").block());

    commands.put("prefix ", Bot::setPrefix);

    commands.put("localtime", Bot::tellTime);

    commands.put("play", Bot::play);

    commands.put("join", Bot::join);

    commands.put("stop", Bot::stop);

    commands.put("leave", Bot::leave);
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
   * Send a message in a channel.
   *
   * @param event the messageEvent
   * @param message the message to send
   */
  private static void sendMessage(MessageCreateEvent event, String message) {
    Objects.requireNonNull(event.getMessage().getChannel().block()).createMessage(message).block();
  }

  /**
   * Change the prefix for the bot.
   *
   * @param event the event triggering this call
   */
  private static void setPrefix(MessageCreateEvent event) {
    try {
      prefix = content.substring(prefix.length() + "prefix ".length());
      sendMessage(event,"Prefix set to " + prefix);
    } catch (StringIndexOutOfBoundsException e) {
      sendMessage(event,"Error setting prefix! Kept as: " + prefix);
    }
  }

  /**
   * Send a message telling the time in the bot's timezone.
   *
   * @param event the messageEvent
   */
  private static void tellTime(MessageCreateEvent event) {
    sendMessage(event, "Bot's local time is: " + dtf.format(LocalDateTime.now()));
  }

  /**
   * Join a user's voice channel.
   *
   * @param event the messageEvent
   */
  private static void join(MessageCreateEvent event) {
    final Member member = event.getMember().orElse(null);
    if (member != null) {
      final VoiceState voiceState = member.getVoiceState().block();
      if (voiceState != null) {
        final VoiceChannel channel = voiceState.getChannel().block();
        if (channel != null) {
          channel.join(spec -> spec.setProvider(audioPro)).block();
          joined = channel;
          sendMessage(event, String.format("Joined voice channel: %s!", channel.getMention()));
        }
      } else {
        sendMessage(event, "You're not in a voice channel!");
      }
    }
  }

  private static void leave(MessageCreateEvent event) {

  }

  /**
   * Play audio in joined channel.
   *
   * @param event the messageEvent
   */
  private static void play(MessageCreateEvent event) {
    final String content;
    if (event.getMessage().getContent().isPresent()) {
      content = event.getMessage().getContent().get();
      final List<String> command = Arrays.asList(content.split(" "));
      audioMan.loadItem(command.get(1), trackSched);
      sendMessage(event, "Now playing!");
    } else {
      sendMessage(event, "Content not present... whatever that means...");
    }
  }

  /**
   * Stop the currently playing track.
   *
   * @param event the messageEvent
   */
  private static void stop(MessageCreateEvent event) {
    trackSched.stopTrack();
    sendMessage(event, "Playback stopped.");
  }
}
