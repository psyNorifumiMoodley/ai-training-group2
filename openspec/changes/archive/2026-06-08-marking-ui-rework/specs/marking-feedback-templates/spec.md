## ADDED Requirements

### Requirement: Marking page offers three outcome-aligned feedback templates
The marking page feedback composition section SHALL provide exactly three pre-filled feedback templates, replacing the previous "Strong performance", "Good effort", and "Needs improvement" buttons.

The three templates are:

**Set up interview**
> We were impressed with your performance on the assessment and would like to invite you for an interview. Please let us know your availability — we're looking at scheduling this for [DATE/TIME]. We look forward to meeting you and discussing the role further.

**Additional assessment**
> Thank you for completing the assessment. We have reviewed your submission and would like to invite you to take a follow-up assessment to explore certain areas in more depth. We will be in touch shortly with the details.

**Unfortunately looking elsewhere**
> Thank you for taking the time to complete our assessment. After careful consideration, we have decided to move forward with other candidates whose profiles more closely match our current requirements. We appreciate your interest and wish you the best in your job search.

#### Scenario: "Set up interview" template loads pre-filled text
- **WHEN** the marker clicks "Set up interview"
- **THEN** the overall feedback textarea is populated with the pre-filled interview invitation text including the `[DATE/TIME]` placeholder

#### Scenario: "Additional assessment" template loads pre-filled text
- **WHEN** the marker clicks "Additional assessment"
- **THEN** the overall feedback textarea is populated with the pre-filled follow-up assessment text

#### Scenario: "Unfortunately looking elsewhere" template loads pre-filled text
- **WHEN** the marker clicks "Unfortunately looking elsewhere"
- **THEN** the overall feedback textarea is populated with the pre-filled rejection text

#### Scenario: Loaded template is editable
- **WHEN** a template has been loaded into the textarea
- **THEN** the marker can freely edit, add to, or clear the text before finalising
