# Jira MCP — Team Setup Prompt

Copy everything inside the horizontal rules below and paste it into your Claude Code prompt.

---

## Prerequisites

Run these once in your terminal before starting:

```bash
npm install -g mcp-atlassian
npm install -g jsdom
```

## Instructions for Claude Code

I need you to set up my personal Jira MCP credentials for this project.

**Context:**
- The project already has the Jira MCP server defined in `.mcp.json` (pointing to `psybergate.atlassian.net`).
- My personal credentials must go into `.claude/settings.local.json`, which is gitignored so they are never committed.

**Please do the following:**

1. Ask me for:
   - My Atlassian account email (the one I use to log in to psybergate.atlassian.net)
   - My Atlassian API token (I can generate one at https://id.atlassian.com/manage-profile/security/api-tokens if I don't have one)

2. Read my existing `.claude/settings.local.json` (create it if it doesn't exist).

3. Merge the following into it, preserving any existing settings:
   ```json
   {
     "env": {
       "ATLASSIAN_EMAIL": "<norifumi.moodley@psybergate.co.za>",
       "ATLASSIAN_API_TOKEN": "<ATATT3xFfGF0l_JAqHi6XRxl_MNf_tXJHQiNnHYoTqxCPrhDcdt7AoBxnw-sB_M1yyn4-umVyXWGCJz1kTAlq8ILDeBATZedAWzEJ6b5hfkAI0sd9y54QDEN5LN3POWstYZE9-hQXlHwhWUKBZR6NX4BMy0D15Ds4ryp_Fe6sMk0O6_YwA47vUM=CD41DFB9>"
     },
     "enabledMcpjsonServers": ["jira"]
   }
   ```

4. Confirm the file was written and remind me to **restart Claude Code** so the MCP server loads.

5. After I restart, tell me I can verify the connection with `! claude mcp list` in the Claude Code prompt.

---

> **Note:** Never commit `.claude/settings.local.json`. It is already listed in `.gitignore`.
