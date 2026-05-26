---
name: log-prompt
description: Records a prompt entry (prompt text, author, branch, time) into a date-based log file at .claude/logs/YYYY-MM-DD.md for collaborative project auditing. Invoke after completing work on a prompt.
metadata:
  author: Gregory Robson
  version: "1.2"
---

Log this prompt session to `.claude/logs/<today's date>.md`.

---

**Input**: Optional argument — the prompt text can be passed inline, e.g.:
`/log-prompt "Implement login form"`

**Steps**

1. **Get author**
   Run:
   ```bash
   git config user.name
   ```
   If that returns nothing, fall back to:
   ```bash
   git config user.email
   ```
   If both fail, ask the user: "What is your name or email for the log?"

2. **Get branch**
   Run:
   ```bash
   git branch --show-current
   ```

3. **Get prompt text**
    - If the prompt text was passed as an inline argument, use it.
    - Otherwise ask the user:
      > "Paste the prompt text you submitted for this session."

4. **Get date and time**
   Use PowerShell:
   ```powershell
   Get-Date -Format "yyyy-MM-dd"
   Get-Date -Format "HH:mm"
   ```

5. **Determine log file path**
   `.claude/logs/<YYYY-MM-DD>.md` (one file per calendar day).

6. **Format the log entry**
   Use this exact template:

   ```markdown
   **Prompt:**
   > <prompt text>

   | Field  | Value    |
   |--------|----------|
   | Time   | <HH:MM>  |
   | Author | <author> |
   | Branch | <branch> |

   ---
   ```

   If the prompt text spans multiple lines, prefix each line with `> `.

7. **Write to the log file**
    - **If the file does not exist**, create it with this header first:
      ```markdown
      # Prompt Log — <YYYY-MM-DD>

      ---
      ```
    - **Append** the formatted entry to the end of the file.
    - Do **not** overwrite or truncate any existing content.

8. **Confirm**
   Output a single line to the user:
   > "Logged to `.claude/logs/<YYYY-MM-DD>.md`."

---

**Guardrails**
- Never overwrite an existing log file — always append.
- Never skip steps 1–2 — author and branch are required.
- If any `git` command fails, note `—` for that field and continue.
- Keep the entry compact: no extra commentary beyond the template.
