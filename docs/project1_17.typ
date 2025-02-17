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
#todo("Details to be added to below sections")

== Identification of Relevant Classes

=== Subscription and Content Subsystem

+ *SubscriptionResource (REST Resource)*
  - Handles feed subscription management via REST API
  - _Functionality_: Add/update/delete subscriptions, import/export feeds (OPML/Google Takeout), fetch recent articles
  - _Behavior_: Validates inputs, coordinates with DAOs for persistence, triggers feed synchronization

+ *FeedService (Service)*
    - Core service for feed synchronization and maintenance
    - _Functionality_: Periodic feed updates, feed parsing, article deduplication, favicon updates
    - _Behavior_: Uses Quartz scheduler, coordinates with FeedDao/ArticleDao, handles feed parsing errors

+ *Feed (Model)*
    - Represents RSS feed entity
    - _Attributes_: RSS URL, website URL, title, description, synchronization status
    - _Behavior_: Persisted via FeedDao, contains article relationships

+ *Article (Model)*
    - Represents individual article content
    - _Attributes_: Title, content URL, publication date, enclosures
    - _Behavior_: Managed by ArticleDao, linked to parent Feed

+ *OpmlReader (Utility)*
    - Parses OPML subscription files
    - _Functionality_: Imports/Exports feed lists in standard OPML format
    - _Behavior_: XML parsing, outline hierarchy processing

+ *FeedSubscriptionDao (DAO)*
    - Manages user-feed subscription relationships
    - _Functionality_: Subscription CRUD operations, unread count tracking
    - _Behavior_: Uses FeedSubscriptionCriteria for queries, updates denormalized counts

+ *FeedSynchronizationDao (DAO)*
    - Tracks feed update history
    - _Functionality_: Records synchronization attempts and outcomes
    - _Behavior_: Maintains performance metrics for feed updates

=== Feed Organization Subsystem

+ *CategoryResource (REST Resource)*
    - Manages folder organization via REST
    - _Functionality_: Create/update/delete categories, manage article visibility
    - _Behavior_: Enforces folder hierarchy, coordinates with CategoryDao

+ *CategoryDao (DAO)*
    - Handles folder persistence and hierarchy
    - _Functionality_: Parent-child category relationships, ordering
    - _Behavior_: Uses nested set pattern for tree operations

+ *UserArticleDao (DAO)*
    - Manages user-specific article states
    - _Functionality_: Read/unread tracking, starring, bulk operations
    - _Behavior_: Uses complex UserArticleCriteria for filtered queries

+ *SearchResource (REST Resource)*
    - Provides full-text search capabilities
    - _Functionality_: Lucene-based article search
    - _Behavior_: Integrates with IndexingService, handles pagination

+ *ArticleAssembler (DTO Mapper)*
    - Transforms domain models to API responses
    - _Functionality_: JSON serialization, field filtering
    - _Behavior_: Used by REST resources to format outputs

+ *StarredResource (REST Resource)*
    - Manages starred articles
    - _Functionality_: Bulk starring/unstarring operations
    - _Behavior_: Coordinates with UserArticleDao for state changes

+ *IndexingService (Service)*
    - Maintains search index
    - _Functionality_: Lucene index management, query processing
    - _Behavior_: Async index rebuilding, handles search pagination

=== User Management Subsystem

+ *UserResource (REST Resource)*
    - Exposes user management API
    - _Functionality_: Registration, login, profile updates
    - _Behavior_: Uses BCrypt password hashing, JWT token issuance

+ *UserDao (DAO)*
    - Manages user persistence
    - _Functionality_: CRUD operations, password hashing
    - _Behavior_: Enforces unique usernames, soft deletes

+ *AuthenticationTokenDao (DAO)*
    - Manages session tokens
    - _Functionality_: Token generation/validation, session cleanup
    - _Behavior_: Supports both cookie and header-based auth

+ *SecurityFilter (Security)*
    - Authentication/Authorization filter
    - _Functionality_: Request validation, role checking
    - _Behavior_: Integrates with UserPrincipal/IPrincipal

+ *UserPrincipal (Security)*
    - Represents authenticated user context
    - _Functionality_: Role-based access control
    - _Behavior_: Carries user permissions (BaseFunction enum)

+ *PasswordChangedEvent (Event)*
    - Domain event for credential changes
    - _Functionality_: Triggers security updates
    - _Behavior_: Published through AppContext event bus

+ *AppContext (Singleton)*
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
    inset: 10pt,
    align: horizon,
    table.header(
      [*Design Smell*], [*Description*], [*Justification*],
    ),
    [Deficient Encapsulation], [This smell occurs when the declared accessibility of one or more members of an abstraction is more permissive than actually required.], [The class fields or methods are not properly encapsulated, leading to potential misuse or unintended access from outside the class.],
    [Broken Modularization], [This smell arises when data and/or methods that ideally should have been localized into a single abstraction are separated and spread across multiple abstractions], [],
    [Imperative Abstraction],[This smell arises when an operation is turned into a class],[],
    [God Class],[This smell occurs when a class is large containing too many variables and methods],[],
    [Cyclic Dependency],[This smell arises when two or more abstractions depend on each other directly or indirectly],[],
    [],[],[],
  ),
  caption: [Design Smells Identified in the RSS Reader],
)
== Code Metrics

*Tools Used*
- Sonarqube
- CodeMR
- Designite

Extracted Metrics

#figure(
  table(
    columns: (auto, auto, auto),
    inset: 10pt,
    align: horizon,
    table.header(
      [*Metric*], [*Value*], [*Implication*],
    ),
    [Cyclomatic Complexity], [5], [Moderate complexity.],
    [Code Duplication], [10%], [High code duplication.],
  ),
  caption: [Code Metrics Extracted from the RSS Reader],
)

== Implications Discussions

= Task 3: Refactoring

== Addressing Design Smells

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
  table(
    columns: (auto, auto, auto),
    inset: 10pt,
    align: horizon,
    table.header(
      [*Metric*], [*Value*], [*Implication*],
    ),
    [Cyclomatic Complexity], [3], [Reduced complexity.],
    [Code Duplication], [5%], [Reduced code duplication.],
  ),
  caption: [Code Metrics Post Refactoring],
)

== Leveraging LLMs for Refactoring

*LLM based Refactoring Analysis*

- Manual Refactoring vs LLM based suggestions.
- Accuracy and efficiency of LLM suggestions.

== Automation of Refactoring Pipeline

- Automated Design Smell Detection
- Github Actions Pipeline for Continuous Refactoring

= Conclusion

...

#bibliography("refs.bib")

= Acknowledgements
== Contributions
Following are the contributions made by the team:
#let team = ("Sanket", "Priyank", "Aditya", "Nitheesh", "Yash")
  - UML Design: #team.at(0), #team.at(1), #team.at(2), #team.at(3), #team.at(4)
  - Identifying Smells:
  - Code Metrics:
  - Manual Refactoring:
  - LLM Refactoring:
  - Automating Refactoring:

#pagebreak()
#set heading(numbering: none)
= Appendices

== Appendix A: AI Prompt Screenshots

Checkout content at #link("./prompts.md")[
  Prompts file
]

#page(width: 25cm, height: 150cm, margin:1em)[
  == Appendix B: Additional UML Diagrams

  #figure(
    image("./UML/full.classdiagram.v0-manual.svg", height:98%),
    caption: [UML before refactoring.],
  ) <additional-uml>
]