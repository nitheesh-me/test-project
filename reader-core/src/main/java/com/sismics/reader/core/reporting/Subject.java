package com.sismics.reader.core.reporting;

import java.util.ArrayList;
import java.util.List;

import com.sismics.reader.core.reporting.events.BugEvent;

public class Subject {
    private List<Observer> observers = new ArrayList<>();

    public void registerObserver(Observer observer) {
        observers.add(observer);
    }   

    public void notifyObservers(List<Bug> bugs) {
        for (Observer observer : observers) {
            observer.update(bugs);
        }
    }
}
