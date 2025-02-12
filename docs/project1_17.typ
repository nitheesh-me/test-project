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
      name: "Priyank N",
      id: "",
      address: "team-17 (P90)",
      email: "name@email.address"
    ),
    (
      name: "Aditya Singh Rathore",
      id: "",
      address: "team-17 (P90)",
      email: "name@email.address"
    ),
    (
      name: "Yash Sonkar",
      id: "2024801002",
      address: "team-17 (P90)",
      email: "name@email.address"
    ),
    (
      name: "Sanket Adlak",
      id: "",
      address: "team-17 (P90)",
      email: "name@email.address"
    ),
    (
      name: "Nitheesh Chandra",
      id: "2024801002",
      address: "team-17 (P90)",
      email: "nitheeshchandra.y@research.iiit.ac.in"
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

The goal of this project is to analyze, document, and improve the existing Rudra's Subscription Service (RSS) Reader. The project involves reverse engineering the current codebase, identifying design smells and code metrics, and implementing refactoring strategies. The project aims to improve the maintainability, readability, and performance of the RSS Reader.
#todo("Still to be finalized")

= Project Objectives

- Analyze the existing RSS Reader codebase.
- Identify and document relevant system classes and components.
- Detect and justify design smells.
- Measure software quality through code metrics.
- Refactor the system based on software engineering principles.
- Compare manual refactoring with AI-generated suggestions.
- Automate the refactoring pipeline.

= System Overview

Rudra's Subscription Service (RSS) Reader is a web-based RSS aggregator that allows users to subscribe to and organize RSS feeds. The major subsystems include:

+ Subscription and Content Subsystem - Handles feed subscriptions, imports/exports, and displays recent articles.
+ Feed Organization Subsystem - Allows users to manage, categorize, and search for articles.
+ User Management Subsystem - Manages user accounts and authentication.

= Task 1: Reverse Engineering
#todo("Details to be added to below sections")

== Identification of Relevant Classes

== UML Class Diagram

#figure(
  image("./assets/iiith.png", width: 80%),
  caption: [A curious figure.],
) <subscription-content>

== Observation and Comments

As per @subscription-content we can observe that ...



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
    // image("cylinder.svg"),
    [Feature Envy], [UPDATE: A method in "This" Class accesses another class's data more than its own.], [High coupling between classes.],
  ),
  caption: [Design Smells Identified in the RSS Reader],
)
== Code Metrics

*Tools Used*
- Sonarqube
- CodeMR
- Checkstyle
- PMD

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

= Task 3: Refactoring

== Addressing Design Smells

=== Refactoring Strategy

#codly(number-format: none)
```diff
@@ -29,26 +51,10 @@ public class FaviconUpdateRequestedAsyncListener {
      */
     @Subscribe
     public void onFaviconUpdateRequested(final FaviconUpdateRequestedEvent faviconUpdateRequestedEvent) throws Exception {
-        if (log.isInfoEnabled()) {
-            log.info(MessageFormat.format("Favicon update requested event: {0}", faviconUpdateRequestedEvent.toString()));
-        }
-
+
+        logFaviconRequest(faviconUpdateRequestedEvent);
+
```

Or

#figure(
  image("./assets/iiith.png", width: 40%),
  caption: [Example 1],
) <example-1>

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