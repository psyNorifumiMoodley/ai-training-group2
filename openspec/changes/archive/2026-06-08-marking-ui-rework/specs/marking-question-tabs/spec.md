## ADDED Requirements

### Requirement: Marking page displays a tabbed question filter
The marking page SHALL display a tab bar above the question card list with one tab per question type present in the assessment, plus an "All" tab that is always shown first. Tabs for types with zero questions in the current assessment SHALL NOT be rendered.

#### Scenario: All tab shows every card
- **WHEN** the marker is on the marking page and the "All" tab is active
- **THEN** every `ResponseReviewItem` card is rendered regardless of type

#### Scenario: Type tab filters cards
- **WHEN** the marker clicks a type tab (e.g. "MCQ")
- **THEN** only cards whose `questionType` matches that type are rendered; all other cards are hidden

#### Scenario: Empty type tabs are hidden
- **WHEN** the assessment contains no DOC questions
- **THEN** the "Doc" tab is not rendered in the tab bar

#### Scenario: Active tab is visually distinguished
- **WHEN** a tab is the currently selected tab
- **THEN** it is styled differently from inactive tabs (e.g. underline or background highlight)

#### Scenario: Marked count reflects visible questions
- **WHEN** a type tab is active
- **THEN** the "X of Y reviewed" counter in the sub-topbar reflects the full set (all questions), not just the filtered view, so the marker understands overall progress
