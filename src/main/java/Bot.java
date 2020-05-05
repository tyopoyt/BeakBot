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
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.util.Snowflake;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;


import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;

import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;


public class Bot extends HttpServlet {

    private static final Map<String, Command> commands = new HashMap<>();
    private static final AudioPlayerManager audioMan;
    private static final AudioPlayer player;
    private static final AudioProvider audioPro;
    private static final TrackScheduler trackSched; 
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
    private static Message initMessage;
    private static String prefix = "!", content = "";
    private static VoiceConnection joined = null;
    private static WBList<Snowflake> wblist;
    private static boolean joinBool;
    private static DiscordClient client;



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

        commands.put("blacklist", Bot::blacklist);

        commands.put("clearlist", Bot::clearlist);

        commands.put("help", Bot::help);
        System.out.println("Program ended");
        
        String key = "NzAxMTAzMDk2ODc2NjMwMTM4.Xp9IRw._Tei7T0kCKF0Mv3A0boj9PDCa9Q"; 
        final DiscordClientBuilder builder = DiscordClientBuilder.create(key);
        builder.setInitialPresence(Presence.online(Activity.listening(prefix + "commands")));
        client = builder.build();
        joinBool = false;
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .subscribe(event -> {
                	//Creating a global event object for twitter functionality.
                	initMessage = event.getMessage();
                	
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
        //client.login().block();
        System.out.println(client.isConnected());
        client.login().block();
        System.out.println(client.isConnected());
        System.out.println(client.getResponseTime());
        
        
        System.out.println("Login complete");
    }

    /**
     * Main method.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
    	System.out.println("Main Method");
    }
    /*
    public static void main(String[] args) {
        final DiscordClientBuilder builder = DiscordClientBuilder.create(args[0]);
        builder.setInitialPresence(Presence.online(Activity.listening(prefix + "commands")));
        client = builder.build();
        joinBool = false;
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .subscribe(event -> {
                	//Creating a global event object for twitter functionality.
                	initMessage = event.getMessage();
                	
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
    } */
    
    /**
     * Action when the webhook listener receives a get response.
     *
     * @param HTTPs requests from other endpoints.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	response.setContentType("text/html");
    	PrintWriter out = response.getWriter();
    	out.println("<h3>Webhook Listener for BeakBot</h3>");
    	out.println(client.getResponseTime());
    	
    }
    
    /**
     * Action when the webhook listener receives a get response.
     *
     * @param HTTPs requests from other endpoints.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	resp.setContentType("text/html");
    	PrintWriter out = resp.getWriter();

    	StringBuilder builder = new StringBuilder();
    	String aux = "";

    	while ((aux = req.getReader().readLine()) != null) {  
    		builder.append(aux);
    	}
    	String payLoad = builder.toString();
    	try {
    		//Convert to Json
    		JSONObject jsonPayLoad = new JSONObject(payLoad);
    		String s = jsonPayLoad.toString() + System.lineSeparator();
    		
    		//write to stdout
    		System.out.println("Received Payload:: " + jsonPayLoad.toString());
    		
    		//write to a Discord
    		Objects.requireNonNull(initMessage.getChannel().block()).createMessage(s).block();
    		
    		//Write to local file.
    		/*Path path = Paths.get(filePath);
    		byte[] strToByte = s.getBytes();
    		Files.write(path,strToByte,APPEND,CREATE);*/
    		out.println("{\"success\":\"true\"}");
    		
    	} catch (Exception e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
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
     * @param event   the messageEvent
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
            sendMessage(event, "Prefix set to " + prefix);
            client.updatePresence(Presence.online(Activity.listening(prefix + "help"))).block();
        } catch (StringIndexOutOfBoundsException e) {
            sendMessage(event, "Error setting prefix! Kept as: " + prefix);
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
                    if(joinBool){joined.disconnect();}
                    joined = channel.join(spec -> spec.setProvider(audioPro)).block();
                    joinBool = true;
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
        joinBool = false;
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
        if (wblist.isWhiteList() || wblist.isEmpty()) {
            if (wblist.isEmpty()) {
                wblist.reset(true);
            }
            if (!wblist.contains(message.getChannelId())) {
                wblist.add(message.getChannelId());
                sendMessage(event, "Successfully whitelisted this channel.");
            } else {
                sendMessage(event, "This channel is already whitelisted.");
            }
        } else {
            sendMessage(event, "Currently using a blacklist.\nTo change to a whitelist try "
                    + prefix + "clearlist white");
        }
    }

    /**
     * Add a channel to blackList.
     *
     * @param event the messageEvent
     */
    private static void blacklist(MessageCreateEvent event) {
        Message message = event.getMessage();
        if (wblist.isBlackList() || wblist.isEmpty()) {
            if (wblist.isWhiteList()) {
                wblist.reset(false);
            }
            if (!wblist.contains(message.getChannelId())) {
                wblist.add(message.getChannelId());
                sendMessage(event, "Successfully blacklisted this channel.");
            } else {
                sendMessage(event, "This channel is already blacklisted.");
            }
        } else {
            sendMessage(event, "Currently using a whitelist.\nTo change to a blacklist try "
                    + prefix + "clearlist black");
        }
    }

    /**
     * Reset the WBList.
     *
     * @param event the messageEvent
     */
    private static void clearlist(MessageCreateEvent event) {
        final String content;
        if (event.getMessage().getContent().isPresent()) {
            content = event.getMessage().getContent().get();
            final List<String> command = Arrays.asList(content.split(" "));
            if (command.size() != 2
                    || !(command.get(1).equals("black") || command.get(1).equals("white"))) {
                sendMessage(event, String.format("Improper syntax.\n\nTry: %sclearlist [black/white]",
                        prefix));
            } else {
                boolean action = command.get(1).equals("white");
                wblist.reset(action);
                sendMessage(event, String.format("Successfully reset list to %slist.",
                        action ? "white" : "black"));
            }
        } else {
            sendMessage(event, "Content not present... whatever that means...");
        }
    }

    private static void help(MessageCreateEvent event) {
        sendMessage(event, "Hi I'm BeakBot!\n" +
                "Here is a list of my commands!\n" +
                "```css\n" +
                "#ping [none] pong\n" +
                "#prefix [none] Changes my prefix\n" +
                "#localtime [none] Gives you my clock time\n" +
                "#join [none] Joins the voice channel you are in\n" +
                "#leave [none] Leaves the voice channel\n" +
                "#play [Song link] Plays the requested song, will end the current song to play it\n" +
                "#pause [none] Pauses the current song\n" +
                "#stop [none] Stops playing the song\n" +
                "#resume [none] Resumes a paused song\n" +
                "#whitelist [none] Adds this channel to the whitelist, this is the default list, and empty by default\n" +
                "#blacklist [none] Adds this channel to the blacklist\n" +
                "#clearlist [blacklist/whitelist] Clears either the white or black list, then makes that list the one in use```");
    }

}