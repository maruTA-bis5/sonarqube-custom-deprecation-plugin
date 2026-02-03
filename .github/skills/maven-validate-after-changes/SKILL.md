---
name: maven-validate-after-changes
description: Validates Maven project after modifying pom.xml or source files by running mvn clean verify. Ensures clean builds are used when Javadoc or other regeneratable artifacts might be affected. Use this skill after making changes to project configuration, Java code, or resource files.
license: Apache-2.0
metadata:
  version: "1.0"
---

# Maven Validation After Changes Skill

This skill ensures that Maven projects are properly validated after modifications by running the appropriate Maven commands with clean flags when needed.

## When to Use

Activate this skill when:
- The user modifies `pom.xml`
- The user modifies Java source files in `src/main/` or `src/test/`
- The user modifies resource files
- You've made code changes and need to verify the build
- Javadoc-related changes are made (documentation, code comments affecting docs)

## Validation Rules

### Clean Build Required
Use `mvn clean verify` (with `clean` flag) in these cases:
- **Javadoc modifications**: Previous Javadoc builds cache output; `clean` ensures fresh generation
- **Resource files changes**: To ensure resource processing is fresh
- **First-time validation**: When validation hasn't been run since file modifications

### Standard Verification
Use `mvn verify` (without `clean`) only for:
- Minor code changes where no regeneratable artifacts are affected
- Quick validation to save time (if you're confident clean isn't needed)

## Implementation Steps

1. **Determine if `clean` is needed**:
   - Check if any Javadoc-related files were modified (comments, documentation, resource files)
   - Check if this is the first validation since modifications started
   - Default to using `clean` if uncertain

2. **Run the appropriate command**:
   ```bash
   # For Javadoc changes or first validation
   mvn clean verify

   # For other code changes (if you're certain clean isn't needed)
   mvn verify
   ```

3. **Verify the output**:
   - Check for `BUILD SUCCESS` in the output
   - Report any test failures or compilation errors
   - If Javadoc validation is part of verify, ensure no documentation warnings remain

## Known Issues and Workarounds

- **Stale Javadoc cache**: If Javadoc changes aren't reflected, run `mvn clean` to clear the cache
- **Previous failed builds**: Clean builds resolve most caching issues
- **IDE caching**: Even if Maven build passes, IDE might show stale results; rebuild in IDE

## Edge Cases

Monitor for future scenarios requiring `clean`:
- Addition of new resource processing plugins
- Introduction of annotation processing with caching
- Build artifact modifications (e.g., custom JAR packaging changes)

If you discover new cases where `clean` is needed, update this skill and commit the change separately with a descriptive message explaining the new requirement.

## Future Enhancements

- Parse Maven output to provide detailed build failure summaries
- Detect specific build failure patterns and suggest fixes
- Cache Maven repo locally for faster builds
