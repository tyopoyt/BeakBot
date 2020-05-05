import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
public class TrackScheduler implements AudioLoadResultHandler {

    private final AudioPlayer player;

    public TrackScheduler(final AudioPlayer player) {
        this.player = player;
    }

    @Override
    public void trackLoaded(final AudioTrack track) {
        // LavaPlayer found an audio source for us to play
        System.out.println("single song");
        player.playTrack(track);
    }

    @Override
    public void playlistLoaded(final AudioPlaylist playlist) {
        player.playTrack(playlist.getSelectedTrack());
        System.out.println("Playlsit");
    }

    @Override
    public void noMatches() {
        System.out.println("matches)");
    }

    @Override
    public void loadFailed(final FriendlyException exception) {
        System.out.println("loadfail");
        // LavaPlayer could not parse an audio source for some reason
    }

    //stop the currently playing track.
    public void stopTrack() {
        player.stopTrack();
    }

    //pause the track
    public void pauseTrack(){
        player.setPaused(true);
    }

    //resume the track
    public boolean resumeTrack(){
        boolean paused = player.isPaused();
        if (paused) {
            player.setPaused(false);
        }
        return paused;
    }

    //check if a track is playing
    public boolean isPlaying() {
        return player.getPlayingTrack() != null;
    }

}