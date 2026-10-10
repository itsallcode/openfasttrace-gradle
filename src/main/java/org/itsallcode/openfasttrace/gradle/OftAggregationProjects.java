package org.itsallcode.openfasttrace.gradle;

import java.util.Map;

/** Immutable settings-provided project paths used by root file-channel aggregation. */
public record OftAggregationProjects(Map<String, String> projectDirectories)
{
    /** Extension name installed on the root project by the settings plugin. */
    public static final String EXTENSION_NAME = "oftAggregationProjects";

    /** Creates a defensive immutable copy. */
    public OftAggregationProjects
    {
        projectDirectories = Map.copyOf(projectDirectories);
    }
}
