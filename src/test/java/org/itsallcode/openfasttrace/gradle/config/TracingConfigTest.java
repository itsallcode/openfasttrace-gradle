package org.itsallcode.openfasttrace.gradle.config;

import static org.junit.jupiter.api.Assertions.*;

import org.gradle.testfixtures.ProjectBuilder;
import org.itsallcode.openfasttrace.api.ColorScheme;
import org.itsallcode.openfasttrace.api.DetailsSectionDisplay;
import org.itsallcode.openfasttrace.api.report.ReportVerbosity;
import org.junit.jupiter.api.Test;

class TracingConfigTest
{
    private final TracingConfig tracingConfig = new TracingConfig(ProjectBuilder.builder().build());

    @Test
    void acceptsColorSchemeNameIgnoringCase()
    {
        tracingConfig.setReportColorScheme("color");

        assertEquals(ColorScheme.COLOR, tracingConfig.getReportColorScheme().get());
    }

    @Test
    void acceptsNullColorScheme()
    {
        tracingConfig.setReportColorScheme((String) null);

        assertFalse(tracingConfig.getReportColorScheme().isPresent());
    }

    @Test
    void rejectsInvalidColorSchemeWithValueAndValidValues()
    {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tracingConfig.setReportColorScheme("rainbow"));

        assertEquals(
                "Invalid color scheme 'rainbow'. Valid color schemes are: BLACK_AND_WHITE, MONOCHROME, COLOR",
                exception.getMessage());
    }

    @Test
    void acceptsVerbosityNameIgnoringCase()
    {
        tracingConfig.setReportVerbosity("summary");

        assertEquals(ReportVerbosity.SUMMARY, tracingConfig.getReportVerbosity().get());
    }

    @Test
    void acceptsNullVerbosity()
    {
        tracingConfig.setReportVerbosity((String) null);

        assertFalse(tracingConfig.getReportVerbosity().isPresent());
    }

    @Test
    void rejectsInvalidVerbosityWithValueAndValidValues()
    {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tracingConfig.setReportVerbosity("extreme"));

        assertEquals(
                "Invalid verbosity 'extreme'. Valid verbosities are: QUIET, MINIMAL, SUMMARY, FAILURES, DIRECT_FAILURES, FAILURE_SUMMARIES, DIRECT_FAILURE_SUMMARIES, FAILURE_DETAILS, DIRECT_FAILURE_DETAILS, ALL",
                exception.getMessage());
    }

    @Test
    void acceptsDetailsSectionDisplayNameIgnoringCase()
    {
        tracingConfig.setDetailsSectionDisplay("collapse");

        assertEquals(DetailsSectionDisplay.COLLAPSE, tracingConfig.getDetailsSectionDisplay().get());
    }

    @Test
    void acceptsNullDetailsSectionDisplay()
    {
        tracingConfig.setDetailsSectionDisplay((String) null);

        assertFalse(tracingConfig.getDetailsSectionDisplay().isPresent());
    }

    @Test
    void rejectsInvalidDetailsSectionDisplayWithValueAndValidValues()
    {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tracingConfig.setDetailsSectionDisplay("invalid"));

        assertEquals(
                "Invalid details section display 'invalid'. Valid values are: COLLAPSE, EXPAND",
                exception.getMessage());
    }
}