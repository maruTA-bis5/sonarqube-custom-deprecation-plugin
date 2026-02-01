# Custom Deprecation SonarQube Plugin

## Overview
This SonarQube plugin lets you define project-specific deprecated APIs in JSON and detect their usage in Java code. It is useful when a library does not officially mark an API as deprecated but your project wants to treat it as such.

## Installation
1. Build the plugin.
2. Copy the generated JAR into the SonarQube extensions directory.
3. Restart SonarQube.

### Build
```bash
mvn clean package
```

## Usage
In the SonarQube rule settings, configure the `deprecatedApis` parameter of the `Custom Deprecation` rule with a JSON array.

### JSON configuration example
```json
[
  {
    "fqcn": "com.example.library.OldClass",
    "member": "legacyMethod",
    "signature": "(Ljava/lang/String;)V",
    "migration": "Use com.example.library.NewClass#newMethod() instead",
    "note": "This method will be removed in version 3.0"
  }
]
```

## Multiple configuration example
```json
[
  {
    "fqcn": "com.example.library.OldClass",
    "member": "legacyMethod",
    "signature": null,
    "migration": "Use com.example.library.NewClass#newMethod() instead",
    "note": ""
  },
  {
    "fqcn": "com.example.library.Constants",
    "member": "OLD_CONSTANT",
    "signature": null,
    "migration": "Use Constants.NEW_CONSTANT",
    "note": ""
  },
  {
    "fqcn": "com.example.library.DeprecatedClass",
    "member": "<init>",
    "signature": "(Ljava/lang/String;Ljava/lang/String;)V",
    "migration": "Use Builder pattern instead",
    "note": "Constructor with all parameters"
  }
]
```

## Development
Common development tasks:

### Run tests
```bash
mvn test
```

### Update license headers
Apply or update license headers on source files:
```bash
mvn license:update-file-header
```

## Troubleshooting
- If the JSON is invalid, the plugin runs with an empty configuration and logs a warning.
- If the rule does not appear, verify the JAR location and restart SonarQube.

## License
Apache License 2.0
