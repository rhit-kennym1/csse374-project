package testclasses.example;


import java.util.ArrayList;
import java.util.List;

/**
 * Test class for ObserverPatternLinter.
 */
public class TestObserverPattern {

    // SHOULD TRIGGER: Valid Subject
    class WeatherStation {
        private List<Observer> observers = new ArrayList<>();

        public void addObserver(Observer o) {
            observers.add(o);
        }

        public void notifyObservers() {
            for (Observer o : observers) {
                o.update();
            }
        }
    }

    interface Observer {
        void update();
    }

    // SHOULD NOT TRIGGER:
    // Has list but no notify loop
    class JustAListHolder {
        private List<String> items = new ArrayList<>();

        public void addItem(String s) {
            items.add(s);
        }
    }

    // SHOULD NOT TRIGGER:
    // Has update call but no list
    class SingleObserverCaller {
        private Observer observer;

        public void callOnce() {
            observer.update();
        }
    }

    // SHOULD TRIGGER:
    // Different naming but still observer-like
    class EventManager {
        private List<Listener> listeners = new ArrayList<>();

        public void fireEvent() {
            for (Listener l : listeners) {
                l.notifyChange();
            }
        }
    }

    interface Listener {
        void notifyChange();
    }
}
