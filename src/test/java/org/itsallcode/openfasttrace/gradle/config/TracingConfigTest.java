package org.itsallcode.openfasttrace.gradle.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.gradle.testfixtures.ProjectBuilder;
import org.itsallcode.openfasttrace.api.ColorScheme;
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
    void rejectsInvalidColorSchemeWithValueAndValidValues()
    {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tracingConfig.setReportColorScheme("rainbow"));

        assertEquals(
                "Invalid color scheme 'rainbow'. Valid color schemes are: BLACK_AND_WHITE, MONOCHROME, COLOR",
                exception.getMessage());
    }
}