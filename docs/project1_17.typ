// Documentation at https://typst.app/docs/
// Install Tinimist package in vscode (for preview)
// `typst compile project1_17.typ project1_17.pdf` to compile the document
#import "@preview/aio-studi-and-thesis:0.1.0": *
#import "@preview/gviz:0.1.0": *
#show raw.where(lang: "dot-render"): it => render-image(it.text)
// Manuals: https://github.com/typst/packages/raw/main/packages/preview/aio-studi-and-thesis/0.1.0/docs/manual-en.pdf

#show: project.with(
  lang: "en",
  authors: (
     (
      name: "Sanket Adlak",
      id: "2024204005",
      address: "team-17 (P90)",
      // email: "sanket.adlak@students.iiit.ac.in"
     ),
     (
      name: "Priyank Nagarnaik",
      id: "2024204011",
      address: "team-17 (P90)",
      // email: "priyank.nagarnaik@students.iiit.ac.in"
    ),
    (
      name: "Aditya Singh Rathore",
      id: "2024204012",
      address: "team-17 (P90)",
      // email: "aditya.sing@students.iiit.ac.in"
    ),
    (
      name: "Yash Sonkar",
      id: "2024801001",
      address: "team-17 (P90)",
      // email: "yash.sonkar@research.iiit.ac.in"
    ),
    (
      name: "Nitheesh Chandra",
      id: "2024801002",
      address: "team-17 (P90)",
      // email: "nitheeshchandra.y@research.iiit.ac.in"
    ),
  ),
  title: [Reverse Engineering and Refactoring of \ Rudra's Subscription Service],
  subtitle: "By Team - 17",
  cover-sheet: (
    cover-image:  image("./assets/iiith.png"),
    description: [],
    faculty: "Karthik Vaidhyanathan",
    semester: "Spring 2025",
    course: "CS6.401 Software Engineering",
  ),

  // Color settings
  primary-color: dark-blue,
  secondary-color: blue,
  text-color: dark-grey,
  background-color: light-blue,

  // Other Settings
  show-list-of-figures: true,
  show-list-of-tables: true,
)

= Introduction


The goal of this project is to analyze, document, and improve the existing Rudra's Subscription Service (RSS) Reader. The project involves reverse engineering the current codebase, identifying design smells and code metrics, and implementing refactoring strategies. The project aims to improve the maintainability, readability, and performance of the RSS Reader while applying software engineering principles learned in the course.

= Project Objectives

- Analyze the existing RSS Reader codebase to understand its structure and functionality.
- Identify and document relevant system classes and components for the three major subsystems: Subscription and Content, Feed Organization, and User Management.
- Detect and justify design smells using tools like SonarQube and Designite Java, supported by manual inspection.
- Measure software quality through code metrics such as Cyclomatic Complexity, Code Duplication, and OOP-specific metrics (e.g., Chidamber and Kemerer metrics).
- Refactor the system based on identified design smells and metrics, ensuring adherence to software engineering principles.
- Compare manual refactoring with AI-generated suggestions using Large Language Models (LLMs) like GPT-4 or Claude.
- Automate the refactoring pipeline by integrating LLMs to detect design smells, refactor code, and generate pull requests.

= System Overview

Rudra's Subscription Service (RSS) Reader is a web-based RSS aggregator that allows users to subscribe to and organize RSS feeds. The major subsystems include:

+ Subscription and Content Subsystem - Handles feed subscriptions, imports/exports, and displays recent articles.
+ Feed Organization Subsystem - Allows users to manage, categorize, and search for articles.
+ User Management Subsystem - Manages user accounts and authentication.

= Task 1: Reverse Engineering

== Identification of Relevant Classes

=== Subscription and Content Subsystem

+ *`SubscriptionResource` (REST Resource)*
  - Handles feed subscription management via REST API
  - _Functionality_: Add/update/delete subscriptions, import/export feeds (OPML/Google Takeout), fetch recent articles
  - _Behavior_: Validates inputs, coordinates with DAOs for persistence, triggers feed synchronization
  
+ *`FeedService` (Service)*
    - Core service for feed synchronization and maintenance
    - _Functionality_: Periodic feed updates, feed parsing, article deduplication, favicon updates
    - _Behavior_: Uses Quartz scheduler, coordinates with `FeedDao`/`ArticleDao`, handles feed parsing errors

+ *`Feed` (Model)*
    - Represents RSS feed entity
    - _Attributes_: RSS URL, website URL, title, description, synchronization status
    - _Behavior_: Persisted via `FeedDao`, contains article relationships

+ *`Article` (Model)*
    - Represents individual article content
    - _Attributes_: Title, content URL, publication date, enclosures
    - _Behavior_: Managed by `ArticleDao`, linked to parent Feed

+ *`OpmlReader` (Utility)*
    - Parses OPML subscription files
    - _Functionality_: Imports/Exports feed lists in standard OPML format
    - _Behavior_: XML parsing, outline hierarchy processing

+ *`FeedSubscriptionDao` (DAO)*
    - Manages user-feed subscription relationships
    - _Functionality_: Subscription CRUD operations, unread count tracking
    - _Behavior_: Uses `FeedSubscriptionCriteria` for queries, updates denormalized counts

+ *`FeedSynchronizationDao` (DAO)*
    - Tracks feed update history
    - _Functionality_: Records synchronization attempts and outcomes
    - _Behavior_: Maintains performance metrics for feed updates
    
=== Feed Organization Subsystem

+ *`CategoryResource` (REST Resource)*
    - Manages folder organization via REST
    - _Functionality_: Create/update/delete categories, manage article visibility
    - _Behavior_: Enforces folder hierarchy, coordinates with `CategoryDao`

+ *`CategoryDao` (DAO)*
    - Handles folder persistence and hierarchy
    - _Functionality_: Parent-child category relationships, ordering
    - _Behavior_: Uses nested set pattern for tree operations

+ *`UserArticleDao` (DAO)*
    - Manages user-specific article states
    - _Functionality_: Read/unread tracking, starring, bulk operations
    - _Behavior_: Uses complex `UserArticleCriteria` for filtered queries

+ *`SearchResource` (REST Resource)*
    - Provides full-text search capabilities
    - _Functionality_: Lucene-based article search
    - _Behavior_: Integrates with `IndexingService`, handles pagination

+ *`ArticleAssembler` (DTO Mapper)*
    - Transforms domain models to API responses
    - _Functionality_: `JSON` serialization, field filtering
    - _Behavior_: Used by `REST` resources to format outputs

+ *`StarredResource` (REST Resource)*
    - Manages starred articles
    - _Functionality_: Bulk starring/unstarring operations
    - _Behavior_: Coordinates with `UserArticleDao` for state changes

+ *`IndexingService` (Service)*
    - Maintains search index
    - _Functionality_: Lucene index management, query processing
    - _Behavior_: Async index rebuilding, handles search pagination

=== User Management Subsystem

+ *`UserResource` (REST Resource)*
    - Exposes user management API
    - _Functionality_: Registration, login, profile updates
    - _Behavior_: Uses `BCrypt` password hashing, `JWT` token issuance

+ *`UserDao` (DAO)*
    - Manages user persistence
    - _Functionality_: CRUD operations, password hashing
    - _Behavior_: Enforces unique usernames, soft deletes

+ *`AuthenticationTokenDao` (DAO)*
    - Manages session tokens
    - _Functionality_: Token generation/validation, session cleanup
    - _Behavior_: Supports both cookie and header-based auth

+ *`SecurityFilter` (Security)*
    - Authentication/Authorization filter
    - _Functionality_: Request validation, role checking
    - _Behavior_: Integrates with `UserPrincipal`/`IPrincipal`

+ *`UserPrincipal` (Security)*
    - Represents authenticated user context
    - _Functionality_: Role-based access control
    - _Behavior_: Carries user permissions (`BaseFunction` `enum`)

+ *`PasswordChangedEvent` (Event)*
    - Domain event for credential changes
    - _Functionality_: Triggers security updates
    - _Behavior_: Published through `AppContext` event bus

+ *`AppContext` (Singleton)*
    - Manages application-wide services
    - _Functionality_: DI container, event bus coordination
    - _Behavior_: Initializes DAOs/Services, handles async operations
    
== UML Class Diagram

=== Subscription and Content Subsystem
#figure(
  image("./UML/SubscriptionSubsystem.svg", width: 80%),
  caption: [Class Diagram of Subscription and Content Subsystem],
) <subscription-content>

As per @subscription-content we can observe that ...

=== Feed Organization Subsystem
#figure(
  image("./UML/FeedOrganizationSubsystem.svg", width: 80%),
  caption: [Class Diagram of Feed Organization Subsystem],
) <feed-organization>

#figure(
  image("./UML/IndexingService.svg", width: 80%),
  caption: [Class Diagram of Indexing Service],
) <indexing-service>

As per @feed-organization & @indexing-service we can observe that ...

=== User Management Subsystem
#figure(
  image("./UML/UserManagement.svg", width: 80%),
  caption: [Class Diagram of User Management Subsystem],
) <user-management>

As per @user-management we can observe that ...

== Observation and Comments

*STRENGHTS*

    - *Modular Design:*
        - The system is well-organized into packages and sub-packages, making it easier to manage and maintain. Each package has a clear responsibility, such as `reader-core` for core functionalities and `reader-web` for web-related functionalities.

    - *Separation of Concerns:*
        - The system separates concerns like data access (DAO), business logic (services), and presentation (web resources). For example, `ArticleDao` handles database operations, while `ArticleResource` handles web requests.

    - *Event-Driven Architecture:*
        - The system uses events (e.g., `ArticleCreatedAsyncEvent`, `FaviconUpdateRequestedEvent`) to handle asynchronous tasks, which improves scalability and decouples components.

    - *Database and ORM Integration:*
        - The system uses JPA (Java Persistence API) for database interactions, which simplifies database operations and ensures consistency. The `BaseDao` class provides a common interface for CRUD operations.

    - *Localization and Internationalization:*
        - The system supports multiple locales (e.g., `LocaleUtil`), making it adaptable to different languages and regions.


*WEAKNESSES*

    - *Complexity:*
        - The system is quite complex, with many layers (e.g., DAO, services, resources) and dependencies. This complexity can make it difficult for new developers to understand and contribute to the codebase.

    - *Tight Coupling in Some Areas:*
        - While the system generally follows good design principles, there are areas where components are tightly coupled. For example, some classes directly create instances of other classes, which can make testing and maintenance harder.

    - *Potential Performance Bottlenecks:*
        - The system relies heavily on database operations and synchronous processing in some areas (e.g., `FeedService`), which could lead to performance bottlenecks, especially under heavy load.

    - *Lack of Documentation:*
        - The UML diagram does not provide detailed documentation for each class or method. This lack of documentation can make it difficult to understand the purpose and usage of certain components.

    - *Limited Scalability in Some Areas:*
        - Some components, like `FeedService`, may not scale well if the number of feeds or users grows significantly. The system might need to be optimized for large-scale deployments.

    - *Hardcoded Values:*
        - Some classes contain hardcoded values, which reduces flexibility and makes it harder to configure the system for different environments.

    - *Testing Challenges:*
        - The system's complexity and tight coupling in some areas could make unit testing and integration testing challenging. Mocking dependencies and ensuring test coverage might require significant effort.


*SUMMARY*

The system has complexity, tight coupling in some areas, and potential performance bottlenecks could pose challenges. Improving documentation, reducing hardcoded values, and optimizing for scalability would enhance the system further.

= Task 2: Design Smells and Code Metrics

== Design Smell Analysis

#figure(
  table(
    columns: (auto, auto, auto),
    inset: 5pt,
    align: horizon,
    table.header(
      [*Design Smell*], [*Description*], [*Justification*],
    ),
    [Deficient Encapsulation], [This smell occurs when the declared accessibility of one or more members of an abstraction is more permissive than actually required.], [The class fields or methods are not properly encapsulated, leading to potential misuse or unintended access from outside the class. \ https://github.com/SE-course-serc/project-1-team-17/issues/4],
    [Primitive Obsession / Broken Modularization (as per Designite)], [This smell occurs when primitive data types are used where an abstraction encapsulation the primitives could serve better], [MIME types are representd as bare string constants. \ https://github.com/SE-course-serc/project-1-team-17/issues/5],
    [Imperative Abstraction],[This smell arises when an operation is turned into a class],[The tool detected this design smell in 3 classes and we merged them into a single class \ https://github.com/SE-course-serc/project-1-team-17/issues/6],
    [God Class],[This smell occurs when a class is large containing too many variables and methods],[The AppContext was a God Class that was refactored by implementing EvenBusManager and AsyncTaskManager \ https://github.com/SE-course-serc/project-1-team-17/issues/7],
    [Cyclic Dependency],[This smell arises when two or more abstractions depend on each other directly or indirectly],[https://github.com/SE-course-serc/project-1-team-17/issues/8]
  ),
  caption: [Design Smells Identified in the RSS Reader],
)
== Code Metrics

*Tools Used*
- Sonarqube
- CodeMR
- Designite

*Extracted Metrics*
  #figure(
    image("./assets/codemr-c3-bf.png", height:50%),
    caption: [C3 before refactoring],
  ) <c3-bf>
#figure(
    image("./assets/codemr-metrics-bf.png", height:70%),
    caption: [Metrics before refactoring],
  ) <metrics-bf>
#figure(
  image("./assets/sonarqube.png", width:100%),
  caption: [SonarQube Stats before refactoring],
) <sq-bf>
#figure(
  image("./assets/sonarqube-issues-1.png", width:100%),
  caption: [SonarQube Issues before refactoring],
) <sq-iss-1>
#figure(
  image("./assets/sonarqube-issues-2.png", width:100%),
  caption: [SonarQube Issues before refactoring],
) <sq-iss-2>
#figure(
  image("./assets/sonarqube-issues-3.png", width:100%),
  caption: [SonarQube Issues before refactoring],
) <sq-iss-3>
// #figure(
//   table(
//     columns: (auto, auto, auto),
//     inset: 10pt,
//     align: horizon,
//     table.header(
//       [*Metric*], [*Value*], [*Implication*],
//     ),
//     [Cyclomatic Complexity], [5], [Moderate complexity.],
//     [Code Duplication], [10%], [High code duplication.],
//   ),
//   caption: [Code Metrics Extracted from the RSS Reader],
// )

== Implications Discussions

For the identified metrics, the following are their implications in terms of software quality, maintainability, and potential performance issues:

- *Coupling*

  Coupling refers to the degree of interdependence between classes, characterized by various relationships such as attributes, method calls, and inheritance. Tightly coupled systems often lead to a ripple effect of changes, requiring more effort to maintain and making classes harder to reuse due to their dependencies.

- *Lack of Cohesion*

  Lack of cohesion measures how related the methods of a class are to one another. High cohesion is desirable as it enhances software traits like robustness and understandability, while low cohesion indicates that a class may be handling multiple responsibilities, making it difficult to maintain and test.

- *Complexity*

  Complexity reflects how difficult it is to understand the interactions among various entities within the software. Increased complexity raises the risk of unintended interactions, which can lead to defects when changes are made, thereby complicating maintenance and development.

- *Size*

  Size is a traditional metric for software measurement, typically assessed by counting lines of code or methods. A high size count may suggest that a class or method is overloaded with responsibilities, indicating a need for refactoring to improve maintainability.

- *Weighted Method Count*

  The Weighted Method Count (WMC) quantifies the complexity of a class by summing the complexities of its methods. A high WMC can indicate that a class is domain-specific and less reusable, as well as more prone to changes and defects, especially if it inherits many methods from a base class.

- *Lack of Cohesion of Methods (LCOM3)*

  LCOM3 measures the interrelatedness of methods within a class, with low cohesion suggesting that the class is handling multiple responsibilities. High LCOM3 values indicate a need for refactoring, as they imply that the class should be split into smaller, more focused subclasses to enhance understandability and maintainability.

- *C3*

  C3 is a composite metric that captures the maximum values of coupling, cohesion, and complexity within a class. This metric provides a holistic view of a class's design quality, helping to identify potential areas for improvement in software architecture.

= Task 3: Refactoring

== Addressing Design Smells
We discovered many design smells using Designite, and we refactored few of them; however, some were false positives. 

#figure(
  image("./assets/designite.png", width:90%),
  caption: [Design smells given by Designite],
) <designite>

Following were the design smells we addressed: 

https://github.com/SE-course-serc/project-1-team-17/issues?q=is%3Aissue

// === Refactoring Strategy

// #codly(number-format: none)
// ```diff
// @@ -29,26 +51,10 @@ public class FaviconUpdateRequestedAsyncListener {
//       */
//      @Subscribe
//      public void onFaviconUpdateRequested(final FaviconUpdateRequestedEvent faviconUpdateRequestedEvent) throws Exception {
// -        if (log.isInfoEnabled()) {
// -            log.info(MessageFormat.format("Favicon update requested event: {0}", faviconUpdateRequestedEvent.toString()));
// -        }
// -
// +
// +        logFaviconRequest(faviconUpdateRequestedEvent);
// +
// ```

// Or

// #figure(
//   image("./assets/iiith.png", width: 40%),
//   caption: [Example 1],
// ) <example-1>

== Code Metrics Post Refactoring
#figure(
  image("./assets/codemr-c3-af.png", height:50%),
  caption: [C3 after refactoring],
) <c3-af>
#figure(
    image("./assets/codemr-metrics-af.png", height:70%),
    caption: [Metrics after refactoring],
  ) <metrics-af>

== Analysis of Code Metrics after Refactoring

After refactoring the code, we observed noticeable improvements in most of the code metrics. Many of the issues related to design smells were addressed, and adherence to clean code practices and SOLID principles resulted in enhanced maintainability and readability. This overall trend aligns with our expectations, as better design usually reduces complexity and coupling while increasing cohesion.

However, not all metrics showed improvement; some tradeoffs became evident during the refactoring process. For instance, while complexity and cohesion improved overall, certain changes led to a slight increase in size or coupling in specific areas. This outcome is not uncommon since optimizing one aspect of the code can sometimes impact another. In summary, although most metrics improved it is rare to see an all over enhancement in every metric due to inevitable tradeoffs in software architecture.
  
== Leveraging LLMs for Refactoring

*LLM based Refactoring Analysis*

We utilized DeepSeek to refactor one of the design smells we identified and compared the results with our manual refactoring. We addressed the design smell manually in this issue: https://github.com/SE-course-serc/project-1-team-17/issues/7, and below are the suggestions provided by DeepSeek:

#set quote(block: true)
#quote(attribution: [DeepSeek])[
  *Suggested Refactoring Approach:*

    + Encapsulate Event Bus Management:

        - Create `EventBusManager` to handle event bus creation and listener registration, but encapsulate event posting behind methods.

        - Use interfaces for event buses to enable easier mocking in tests.

    + Improve `AsyncTaskManager`:

        - Centralize thread pool configuration and enforce lifecycle management (e.g., `PreDestroy` hooks for shutdown).

        - Use distinct executors for different event types (mail, imports) with appropriate configurations.

    + Simplify `AppContext`:

        - Remove all event bus/async logic; delegate entirely to `EventBusManager` and `AsyncTaskManager`.

        - Use constructor injection for dependencies instead of a static `initialize` method.
        
    + Code Modernization:

        - Use Java’s `Executors` utilities for thread pools.

        - Replace legacy `Lists.newArrayList()` with modern new `ArrayList<>()`.
]

#quote(attribution: [DeepSeek])[
  *Example Refactored Code Snippets:*
  
  ```// EventBusManager.java (simplified)
public class EventBusManager {
    private final EventBus asyncEventBus;
    private final EventBus mailEventBus;

    public EventBusManager(AsyncTaskManager taskManager) {
        this.asyncEventBus = new AsyncEventBus(taskManager.createExecutor("async"));
        this.mailEventBus = new AsyncEventBus(taskManager.createExecutor("mail"));
        registerListeners();
    }

    private void registerListeners() {
        asyncEventBus.register(new ArticleCreatedAsyncListener());
        // ... other listeners
    }

    // Encapsulate posting events:
    public void postAsyncEvent(Object event) {
        asyncEventBus.post(event);
    }
}```

```
// AsyncTaskManager.java
public class AsyncTaskManager {
    private final Map<String, ExecutorService> executors = new ConcurrentHashMap<>();

    public ExecutorService createExecutor(String type) {
        ExecutorService executor = Executors.newFixedThreadPool(2); // Example config
        executors.put(type, executor);
        return executor;
    }

    @PreDestroy
    public void shutdown() {
        executors.values().forEach(ExecutorService::shutdown);
    }
}
```

```
// AppContext.java (minimalist)
public class AppContext {
    private final FeedService feedService;
    private final EventBusManager eventBusManager;

    public AppContext(FeedService feedService, EventBusManager eventBusManager) {
        this.feedService = feedService;
        this.eventBusManager = eventBusManager;
    }

    // No static initialization logic; use DI.
}
```
]

*Comparison with Our Version*

#figure(
  table(
    columns: (auto, auto, auto),
    inset: 5pt,
    align: horizon,
    table.header(
      [*Aspect*], [*Our Version*], [*Suggested Version*],
    ),
    [Clarity], [Clear separation via `EventBusManager` and `AsyncTaskManager`], [Enhanced by encapsulating event posting (e.g., `postAsyncEvent()` vs. exposing buses).],
    [Conciseness], [Direct but exposes internal event buses (e.g., `getAsyncEventBus()`)], [More concise API surface; clients don’t interact directly with Guava classes.],
    [Best Practices],[Follows SRP but uses static initialization.],[Uses constructor injection and lifecycle hooks (e.g., `PreDestroy` for shutdown).],
    [Testability],[Relies on \ `AppContext.getInstance()`, which is harder to mock],[Constructor injection allows easier mocking in unit tests.],
    [Resource Management],[Manually calls \ `waitForAsyncCompletion()`],[Automates shutdown via `PreDestroy` and consolidates executors by type.]
  ),
  caption: [Comparison b/w our vs LLM refactoring],
)

== Automation of Refactoring Pipeline
#todo("Automated Pipeline to be done @Nitheesh")
// - Automated Design Smell Detection
// - Github Actions Pipeline for Continuous Refactoring

// = Conclusion

// #bibliography("refs.bib")

= Acknowledgements
== Contributions
Following are the contributions made by the team:
#let team = ("Sanket", "Priyank", "Aditya", "Nitheesh", "Yash")
  - UML Design: #team.at(0), #team.at(1), #team.at(2), #team.at(3), #team.at(4)
  - Identifying Smells: #team.at(0), #team.at(1), #team.at(2)
  - Code Metrics: #team.at(0), #team.at(3)
  - Manual Refactoring: #team.at(0), #team.at(2)
  - LLM Refactoring: #team.at(1), #team.at(2)
  - Automating Refactoring: #team.at(3)
  - Documentation: #team.at(0), #team.at(1), #team.at(3)

#pagebreak()
#set heading(numbering: none)
= Appendices

// == Appendix A: AI Prompt Screenshots

// Checkout content at #link("./prompts.md")[
//   Prompts file
// ]

#page(width: 25cm, height: 150cm, margin:1em)[
  == Appendix B: Additional UML Diagrams

  #figure(
    image("./UML/full.classdiagram.v0-manual.svg", height:98%),
    caption: [UML before refactoring.],
  ) <additional-uml>
]