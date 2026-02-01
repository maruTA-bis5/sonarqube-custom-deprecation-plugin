---
name: commit-coauthor
description: Ensures every git commit message includes the required co-author trailer "Co-Authored-By: Copilot <175728472+Copilot@users.noreply.github.com>"; use when creating or amending commits.
---

# Commit co-author trailer enforcement

## Purpose
Always include this exact trailer line in any commit message you create or amend:

Co-Authored-By: Copilot <175728472+Copilot@users.noreply.github.com>

## When to use
- You are about to create a commit.
- You are asked to write a commit message.
- You are amending or squashing commits.

## Steps
1. Draft the commit message as usual.
2. Append a blank line, then add the required trailer exactly as shown.
3. If the commit already exists and the trailer is missing, amend the commit message to add it.
4. If multiple trailers exist, keep each on its own line; do not alter the required line.

## Examples
### New commit message
Fix null check in parser

Co-Authored-By: Copilot <175728472+Copilot@users.noreply.github.com>

### Amended commit message with additional trailers
Refactor config loading

Reviewed-by: Jane Doe <jane@example.com>
Co-Authored-By: Copilot <175728472+Copilot@users.noreply.github.com>

## Edge cases
- Multi-line commit bodies: place the trailer at the end, after a blank line.
- Squash merges: ensure the final message includes the required trailer.
- Do not change the casing or spacing of the required line.
