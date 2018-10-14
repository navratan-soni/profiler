package nuffwritten.com.profiler.pubsub;

/**
 * Created by navratan on 15/10/18
 */
public interface Listener {
    void onEventReceived(Event event);
}
