---
name: commit-message
description: Enforces conventional commits format (type(scope) subject) and includes required co-author trailer "Co-Authored-By Copilot"; use when creating, amending, or reviewing commits.
---

# Commit Message Enforcement

## Purpose
Ensure all git commits follow:
1. **Conventional Commits** format for semantic versioning and changelog generation
2. **Co-author trailer** to track AI assistance in development

## When to use
- Creating a new commit
- Amending or squashing commits
- Writing commit messages
- Reviewing commit history

## Commit Format

### Structure
```
<type>(<scope>): <subject>

<body>

<footer>

Co-Authored-By: Copilot <175728472+Copilot@users.noreply.github.com>
```

### Type (required)
Choose one of:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only changes
- `style`: Code style/formatting (no logic change)
- `refactor`: Code change that neither fixes a bug nor adds a feature
- `perf`: Performance improvement
- `test`: Adding or correcting tests
- `build`: Changes to build system or dependencies
- `ci`: CI configuration changes
- `chore`: Other changes that don't modify src or test files
- `revert`: Reverts a previous commit

### Scope (optional)
Component or module affected (e.g., `parser`, `api`, `config`)

### Subject (required)
- Max 72 characters
- Lowercase (except proper nouns)
- No period at the end
- Imperative mood (e.g., "add" not "added" or "adds")

### Body (optional)
- Blank line after subject
- Explain *what* and *why*, not *how*
- Wrap at 72 characters

### Footer (optional)
- Breaking changes: `BREAKING CHANGE: <description>`
- Issue references: `Closes #123`, `Refs #456`

### Co-author trailer (required)
Always end with a blank line followed by:
```
Co-Authored-By: Copilot <175728472+Copilot@users.noreply.github.com>
```

## Examples

### Simple feature commit
```
feat(parser): add support for nested JSON arrays

Co-Authored-By: Copilot <175728472+Copilot@users.noreply.github.com>
```

### Bug fix with body
```
fix(config): handle null values in deprecation settings

Previously, null arguments in JSON config caused NullPointerException
during parsing. Now defaults to empty string for null values.

Closes #42

Co-Authored-By: Copilot <175728472+Copilot@users.noreply.github.com>
```

### Breaking change
```
feat(api)!: remove deprecated getConfig method

BREAKING CHANGE: getConfig() is removed. Use getDeprecatedApis() instead.
Migration guide: replace all getConfig() calls with getDeprecatedApis().

Co-Authored-By: Copilot <175728472+Copilot@users.noreply.github.com>
```

### Documentation update
```
docs: update README with configuration examples

Co-Authored-By: Copilot <175728472+Copilot@users.noreply.github.com>
```

### Refactoring with scope
```
refactor(check): extract argument matching to separate method

Improves readability and allows reuse in future overload detection.

Co-Authored-By: Copilot <175728472+Copilot@users.noreply.github.com>
```

## Validation checklist

Before committing, verify:
- [ ] Type is one of the allowed types
- [ ] Subject is imperative mood, lowercase, no period
- [ ] Subject is 72 characters or less
- [ ] Blank line separates subject from body (if body exists)
- [ ] Body lines wrap at 72 characters (if body exists)
- [ ] Breaking changes start with `BREAKING CHANGE:` or include `!` after type/scope
- [ ] Co-author trailer is present exactly as specified
- [ ] Co-author trailer is separated from body/footer by blank line

## Edge cases

### Multiple trailers
Keep each on its own line; the co-author trailer should be last:
```
fix(security): patch XSS vulnerability

Reviewed-by: Jane Doe <jane@example.com>
Co-Authored-By: Copilot <175728472+Copilot@users.noreply.github.com>
```

### Squash merges
The final squashed message must include co-author trailer and follow conventional format.

### Amending commits
If amending an existing commit, preserve the conventional format and ensure the co-author trailer is present.

### Revert commits
```
revert: feat(parser): add support for nested arrays

This reverts commit abc123def456.

Co-Authored-By: Copilot <175728472+Copilot@users.noreply.github.com>
```

## References
- [Conventional Commits Specification](https://www.conventionalcommits.org/)
- [Angular Commit Guidelines](https://github.com/angular/angular/blob/main/CONTRIBUTING.md#commit)
