package testclasses.example;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestObserverPattern {

    interface Observer {
        void update();
    }

    interface Listener {
        void notifyChange();
    }

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

    class StockMarket {
        private List<Observer> subscribers = new ArrayList<>();

        public void subscribe(Observer o) {
            subscribers.add(o);
        }

        public void unsubscribe(Observer o) {
            subscribers.remove(o);
        }

        public void broadcastUpdate() {
            for (Observer o : subscribers) {
                o.update();
            }
        }
    }

    class EventManager {
        private Set<Listener> listeners = new HashSet<>();

        public void addListener(Listener l) {
            listeners.add(l);
        }

        public void fireEvent() {
            for (Listener l : listeners) {
                l.notifyChange();
            }
        }
    }

    class TemperatureDisplay implements Observer {
        private double temperature;

        public void update() {
            temperature += 1.0;
        }

        public double getTemperature() {
            return temperature;
        }
    }

    class AlertSystem implements Listener {
        public void notifyChange() {
            System.out.println("Alert triggered");
        }
    }

    class LeakyBroadcaster {
        private List<Observer> permanentObservers = new ArrayList<>();

        public void register(Observer o) {
            permanentObservers.add(o);
        }

        public void broadcastUpdate() {
            for (Observer o : permanentObservers) {
                o.update();
            }
        }
    }

    class JustAListHolder {
        private List<String> items = new ArrayList<>();

        public void addItem(String s) {
            items.add(s);
        }

        public void clearAll() {
            items.clear();
        }
    }

    class SingleObserverCaller {
        private Observer observer;

        public void callOnce() {
            observer.update();
        }
    }
}
