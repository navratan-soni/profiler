package nuffwritten.com.profiler.pubsub;

/**
 * Created by navratan on 09/10/18
 */
public class Event {
    private String eventType;

    private Object payload;

    public Event(String eventType, Object payload) {
        this.eventType = eventType;
        this.payload = payload;
    }

    public String getEventType() {
        return eventType;
    }

    public Object getPayload() {
        return payload;
    }
}
