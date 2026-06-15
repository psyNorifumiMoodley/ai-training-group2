## ADDED Requirements

### Requirement: Code editor is pre-populated with a language-specific scaffold when no saved code exists
When `CodingAnswerComponent` initialises with no prior saved code (`savedAnswer` is absent or its `code` field is empty), the code editor SHALL be pre-filled with the per-language scaffold string rather than an empty string. If the candidate has previously saved code, the saved code takes precedence and the scaffold is NOT applied.

The scaffolds are:
- **JAVA**: `public class Solution {\n    public static void main(String[] args) {\n        \n    }\n}`
- **PYTHON**: `def main():\n    pass\n\nif __name__ == '__main__':\n    main()`
- **CSHARP**: `using System;\n\nclass Solution {\n    static void Main(string[] args) {\n        \n    }\n}`

#### Scenario: New Java question shows Java scaffold in editor
- **WHEN** a candidate opens a CODING question with `language = JAVA` and has no previously saved code
- **THEN** the code editor is pre-filled with the Java scaffold string

#### Scenario: New Python question shows Python scaffold in editor
- **WHEN** a candidate opens a CODING question with `language = PYTHON` and has no previously saved code
- **THEN** the code editor is pre-filled with the Python scaffold string

#### Scenario: New C# question shows C# scaffold in editor
- **WHEN** a candidate opens a CODING question with `language = CSHARP` and has no previously saved code
- **THEN** the code editor is pre-filled with the C# scaffold string

#### Scenario: Previously saved code takes precedence over scaffold
- **WHEN** a candidate re-opens a CODING question that already has saved code
- **THEN** the editor shows the saved code, not the scaffold
