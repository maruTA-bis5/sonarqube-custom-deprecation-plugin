/* SPDX-License-Identifier: Apache-2.0 */
package net.bis5.sonarqube.customdeprecation;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.util.Collections;
import java.util.List;

/**
 * Configuration model for a single deprecated API entry.
 * Represents the metadata needed to identify and report usage of a specific deprecated API.
 */
public class DeprecatedApiConfig {
    private String fqcn;
    private String member;
    private String signature;
    private String migration;
    private String note;

    /**
     * Gets the fully qualified class name of the deprecated API.
     *
     * @return the FQCN (e.g., "com.example.OldApi")
     */
    public String getFqcn() {
        return fqcn;
    }

    /**
     * Gets the member name (method or field) of the deprecated API.
     * Use "&lt;init&gt;" for constructors.
     *
     * @return the member name
     */
    public String getMember() {
        return member;
    }

    /**
     * Gets the JVM method descriptor for the deprecated API.
     * May be null to match all overloads of the member.
     *
     * @return the JVM signature (e.g., "(Ljava/lang/String;)V"), or null for all overloads
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Gets the recommended migration path for the deprecated API.
     *
     * @return the migration message (e.g., "Use NewApi.newMethod() instead")
     */
    public String getMigration() {
        return migration;
    }

    /**
     * Gets additional context or version information about the deprecation.
     *
     * @return the note text, may be empty
     */
    public String getNote() {
        return note;
    }

    /**
     * Checks if this configuration matches the given target API.
     *
     * @param targetFqcn the fully qualified class name to check
     * @param targetMember the member name to check
     * @param targetSignature the JVM signature to check (may be null if signature not required)
     * @return true if this configuration matches the target API
     */
    public boolean matches(String targetFqcn, String targetMember, String targetSignature) {
        if (targetFqcn == null || targetMember == null) {
            return false;
        }
        if (!targetFqcn.equals(this.fqcn) || !targetMember.equals(this.member)) {
            return false;
        }
        if (this.signature == null || this.signature.isEmpty()) {
            return true;
        }
        return this.signature.equals(targetSignature);
    }

    /**
     * Parses a JSON string into a list of DeprecatedApiConfig objects.
     *
     * @param jsonString the JSON array string to parse
     * @return a list of parsed configurations, or an empty list if the input is null or empty
     * @throws JsonSyntaxException if the JSON format is invalid
     */
    public static List<DeprecatedApiConfig> parseFromJson(String jsonString) throws JsonSyntaxException {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Gson gson = new Gson();
        return gson.fromJson(jsonString, new TypeToken<List<DeprecatedApiConfig>>() {}.getType());
    }
}
