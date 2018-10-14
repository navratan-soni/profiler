package nuffwritten.com.profiler.pubsub;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by navratan on 15/10/18
 */
public class PubSub extends Handler implements Runnable {
    private final Thread mThread;

    private final BlockingQueue<Event> mQueue;

    private final BlockingQueue<Event> mUiQueue;

    private Map<String, Set<Listener>> listeners;

    private Map<String, Set<UiListener>> uiListeners;

    private boolean handlerActive = false;

    private static volatile PubSub _instance = null;

    private PubSub() {
        super(Looper.getMainLooper());
        listeners = new ConcurrentHashMap<>();
        uiListeners = new ConcurrentHashMap<>();
        mQueue = new LinkedBlockingQueue<>();
        mUiQueue = new LinkedBlockingQueue<>();
        mThread = new Thread(this);
        mThread.start();
    }

    public static PubSub getPubSub() {
        if (_instance == null) {
            synchronized (PubSub.class) {
                if (_instance == null) {
                    _instance = new PubSub();
                }
            }
        }
        return _instance;
    }

    public void addListener(String type, Listener listener) {
        add(type, listener);
    }

    public void addListeners(Listener listener, String... types) {
        for (String type : types) {
            add(type, listener);
        }
    }

    private void add(String type, Listener listener) {
        Set<Listener> list;
        list = listeners.get(type);
        if (list == null) {
            synchronized (this) // take a smaller lock
            {
                if ((list = listeners.get(type)) == null) {
                    list = new CopyOnWriteArraySet<>();
                    listeners.put(type, list);
                }
            }
        }
        list.add(listener);
    }

    public void addUiListener(UiListener listener, String type) {
        add(type, listener);
    }

    public void addUiListener(UiListener listener, String... types) {
        for (String type : types) {
            add(type, listener);
        }
    }

    private void add(String type, UiListener listener) {
        Set<UiListener> list;
        list = uiListeners.get(type);
        if (list == null) {
            synchronized (this) // take a smaller lock
            {
                if ((list = uiListeners.get(type)) == null) {
                    list = new CopyOnWriteArraySet<>();
                    uiListeners.put(type, list);
                }
            }
        }
        list.add(listener);
    }

    /*
     * We also need to make removeListener a synchronized method. if we don't do that it would lead to memory inconsistency issue. in our case some activities won't get destroyed
     * unless we unregister all listeners and in that slot if activity receives a pubsub event it would try to handle this event which may lead to anything unusual.
     */
    public void removeListener(String type, Listener listener) {
        remove(type, listener);
    }

    public void removeListeners(Listener listener, String... types) {
        for (String type : types) {
            remove(type, listener);
        }
    }

    private void remove(String type, Listener listener) {
        Set<Listener> l = null;
        l = listeners.get(type);
        if (l != null) {
            l.remove(listener);
        }
    }

    public void removeUiListener(UiListener listener, String type) {
        remove(type, listener);
    }

    public void removeUiListener(UiListener listener, String... types) {
        for (String type : types) {
            remove(type, listener);
        }
    }

    private void remove(String type, UiListener listener) {
        Set<UiListener> l = null;
        l = uiListeners.get(type);
        if (l != null) {
            l.remove(listener);
        }
    }

    public boolean publish(Event event) {
        Set<Listener> l = listeners.get(event.getEventType());
        if (l != null && l.size() >= 0) {
            mQueue.add(event);
            return true;
        }

        return false;
    }

    public boolean publishOnUI(Event event) {
        Set<UiListener> uiListenerSet = uiListeners.get(event.getEventType());
        if (uiListenerSet != null && uiListenerSet.size() >= 0) {
            mUiQueue.add(event);

            if (!handlerActive) {
                handlerActive = true;
                if (!sendMessage(obtainMessage())) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    @Override
    public void run() {
        Event event;
        while (true) {
            try {
                event = mQueue.take();
            } catch (InterruptedException e) {
                continue;
            }
            String type = event.getEventType();
            Set<Listener> list = listeners.get(type);

            if (list == null || list.isEmpty()) {
                continue;
            }

            for (Listener l : list) {
                l.onEventReceived(event);
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        while (true) {
            Event event = mUiQueue.poll();
            if (event == null) {
                synchronized (this) {
                    // Check again, this time in synchronized
                    event = mUiQueue.poll();
                    if (event == null) {
                        handlerActive = false;
                        return;
                    }
                }
            }
            String type = event.getEventType();

            Set<UiListener> list = uiListeners.get(type);

            if (list == null || list.isEmpty()) {
                handlerActive = false;
                return;
            }

            for (UiListener l : list) {
                l.onUiEventReceived(event);
            }
        }
    }
}
