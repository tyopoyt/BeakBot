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
import discord4j.core.object.util.Snowflake;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;

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
  private static VoiceConnection joined = null;
  private static WBList<Snowflake> wblist;

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

    commands.put("pause", Bot::pause);

    commands.put("resume", Bot::resume);

    commands.put("whitelist", Bot::whitelist);
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
              Snowflake channel = Objects.requireNonNull(event.getMessage().getChannelId());
              boolean contained = wblist.contains(channel);
              if ((wblist.isWhiteList() && contained) || (wblist.isBlackList() &&
                              !contained) || wblist.isEmpty() || commandOverride(content)) {
                for (final Map.Entry<String, Command> entry : commands.entrySet()) {
                  if (content.startsWith(prefix + entry.getKey())) {
                    entry.getValue().execute(event);
                    break;
                  }
                }
              }
            });

    wblist = new WBList<>(true);

    client.login().block();
  }

  /**
   * Return whether this is a command that should override white/blacklist settings.
   *
   * @param content the command to check
   * @return whether this command should be granted an override
   */
  private static boolean commandOverride(String content) {
    boolean override = false;
    if (content.startsWith(prefix + "whitelist")) {
      override = true;
    } else if (content.startsWith(prefix + "blacklist")) {
      override = true;
    } else if (content.startsWith(prefix + "clearlist")) {
      override = true;
    }
    return override;
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
          joined = channel.join(spec -> spec.setProvider(audioPro)).block();
          sendMessage(event, String.format("Joined voice channel: %s!", channel.getMention()));
        }
      } else {
        sendMessage(event, "You're not in a voice channel!");
      }
    }
  }

  /**
   * Leave the voice channel.
   *
   * @param event the messageEvent
   */
  private static void leave(MessageCreateEvent event) {
    joined.disconnect();
    sendMessage(event, "Leaving voice channel.");
    stop(event);
  }

  /**
   * Play audio in joined channel.
   *
   * @param event the messageEvent
   */
  private static void play(MessageCreateEvent event) {
    if (trackSched.resumeTrack()) {
      sendMessage(event, "Playback resumed.");
    } else {
      final String content;
      if (event.getMessage().getContent().isPresent()) {
        content = event.getMessage().getContent().get();
        final List<String> command = Arrays.asList(content.split(" "));

        if (command.size() != 2) {
          sendMessage(event, String.format("Improper syntax.\n\nTry: %splay [link]", prefix));
        } else {
          audioMan.loadItem(command.get(1), trackSched);
          sendMessage(event, "Now playing!");
        }
      } else {
        sendMessage(event, "Content not present... whatever that means...");
      }
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

  /**
   * Pause the track.
   *
   * @param event the messageEvent
   */
  private static void pause(MessageCreateEvent event) {
    trackSched.pauseTrack();
    sendMessage(event, "Pausing audio playback.");
  }

  /**
   * Resume the paused track, if any.
   *
   * @param event the messageEvent
   */
  private static void resume(MessageCreateEvent event) {
    if (trackSched.resumeTrack()) {
      sendMessage(event, "Playback resumed.");
    } else {
      sendMessage(event, "No playback to resume.");
    }
  }

  /**
   * Add a channel to whitelist.
   *
   * @param event the messageEvent
   */
  private static void whitelist(MessageCreateEvent event) {
    Message message = event.getMessage();
    if (wblist.isWhiteList()) {
      if (!wblist.contains(message.getChannelId())) {
        wblist.add(message.getChannelId());
        sendMessage(event, "Successfully whitelisted this channel.");
      } else {
        sendMessage(event, "This channel is already whitelisted.");
      }
    } else {
      sendMessage(event, "Currently using a blacklist.\nTo change to a whitelist try "
                                  + prefix + "clearList white");
    }
  }

  /**
   * Add a channel to blackList.
   *
   * @param event the messageEvent
   */
  private static void blacklist(MessageCreateEvent event) {
    Message message = event.getMessage();
    if (wblist.isBlackList()) {
      if (!wblist.contains(message.getChannelId())) {
        wblist.add(message.getChannelId());
        sendMessage(event, "Successfully blacklisted this channel.");
      } else {
        sendMessage(event, "This channel is already blacklisted.");
      }
    } else {
      sendMessage(event, "Currently using a whitelist.\nTo change to a blacklist try "
              + prefix + "clearList black");
    }
  }
}
