# Bug Reporting

## Problem Statement:
Bug reporting also needs an overhaul. At present, reporting an issue whisks users away to GitHub. While that’s great for developers, it’s not the best user experience.

Users will be able to submit issues within the RSS Reader itself - to make things simpler an issue is just a statement (description) with a timestamp. The admin will have access to a bug dashboard where they can view, mark bugs as resolved, or delete irrelevant reports. Users should also be able to delete their reported bugs (We imagine a few of you would like to do that for the doubts you send us).

## Implementation:
The implementation for bug reporting feature utilises 2 design patterns:
* Observer pattern (for triggering bug creation/deletion/update events and observing them)
* Singleton pattern (for BugReportManager to ensure that all users see only one instance of the bug list)

Folder structure:
``` 
├── reader
│   ├── core
│   │   ├── reporting 
│   │   │   ├── events
│   │   │   │   ├── BugFiledEvent.java
│   │   │   │   ├── BugEvent.java
│   │   │   │   ├── BugDeletedEvent.java
│   │   │   │   ├── BugStatusEvent.java
│   │   │   ├── Bug.java
│   │   │   ├── BugReportManager.java
│   │   │   ├── BugLogger.java
│   │   │   ├── BugStatus.java
│   │   │   ├── Observer.java
│   │   │   ├── Subject.java
```

### Classes, Interfaces and Their uses:
#### Enums:
* **BugStatus**: Enum for the status of the bug (FILED, RESOLVED)   
 `Additional Note: This could have been implemented as a boolean as well but going with enums allows for easy addition of new features such as soft-deletes, new statuses (In-progress, Assigned, etc).`
```java
public enum BugStatus {
    FILED, 
    RESOLVED
}
```

#### Interfaces:
* **Observer**: Interface for the observer pattern. Subscribes to listen to bug related events.
```java
public interface Observer {
    void update(BugEvent event);
}
```
* **BugEvent.java**: Interface for bug related events. Implemented for 3 events:
    * BugFiledEvent
    * BugDeletedEvent
    * BugStatusEvent
```java
public interface BugEvent {
    String getId();
    void trigger(List<Bug> bugList);
}
```
#### Classes:
##### Events:
* **BugFiledEvent.java**: Event for filing a bug. Triggers when a bug is filed.
* **BugDeletedEvent.java**: Event for deleting a bug. Triggers when a bug is deleted.
* **BugStatusUpdatedEvent.java**: Event for changing the status of a bug. Triggers when a bug status is changed to resolved. Currently, it only marks the given bug as resolved but the functionality can be easily extended
##### Model class:
* **Bug.java**: Model class for the bug. Contains the description, status and id of the bug.
```java
public class Bug {
    private String id;
    private BugStatus status;
    private String description;

    public Bug(String id, String description) {
        this.id = id;
        this.status = BugStatus.FILED;// Every bug is initially marked as FILED
        this.description = description; 
    }   
    // Getters and setters
    // Override for toString()
}
```

##### Manager class:
* **BugReportManager**: Singleton class for managing the bug reports. It is responsible for creating, deleting and updating the status of the bugs. It also maintains a list of observers and triggers events for them. This class maintains a list of reported bugs and uses singleton pattern.
```java
public class BugReportManager {
    private static BugReportManager instance;
    private final Subject subject;
    private final List<Bug> bugList = new ArrayList<>();

    private BugReportManager() {
        subject = new Subject();
    }

    public static BugReportManager getInstance() {
        if (instance == null) {
            instance = new BugReportManager();
        }
        return instance;
    }

    public void registerObserver(Observer observer) {
        subject.registerObserver(observer);
    }   

    public void reportBug(String description) {
        BugEvent event = new BugFiledEvent(UUID.randomUUID().toString(), description);
        event.trigger(bugList);
        subject.notifyObservers(bugList);
    }

    public void resolveBug(String id) {
        BugEvent event = new BugStatusUpdateEvent(id, BugStatus.RESOLVED);
        event.trigger(bugList);
        subject.notifyObservers(bugList);
    }

    public void deleteBug(String id) {
        BugEvent event = new BugDeletedEvent(id);
        event.trigger(bugList);
        subject.notifyObservers(bugList);
    }

    public List<Bug> getBugs() {
        return bugList;
    }
}
```
#### Others:
* **Subject**: Class for the observer pattern. It maintains a list of observers and notifies them when an event is triggered.
```java
public class Subject {
    private List<Observer> observers = new ArrayList<>();

    public void registerObserver(Observer observer) {
        observers.add(observer);
    }   
    public void notifyObservers(List<Bug> bugs) {
        // Notify all observers
    }
}
```
* **BugLogger**: Class for logging the bug reports. It implements the Observer interface and logs the bug reports. Currently, it onl prints the bug reports to console but if a separate logging system is used, this class can be used.
```java
public class BugLogger implements Observer {
    @Override
    public void update(List<Bug> bugs) {
        for (Bug bug : bugs) {
            System.out.println(bug.toString());
        }
    }
}
```
