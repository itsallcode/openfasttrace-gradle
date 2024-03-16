package org.itsallcode.openfasttrace.gradle.config;

import org.junit.jupiter.api.Test;

import com.jparams.verifier.tostring.ToStringVerifier;

class TagPathConfigurationTest
{
    @Test
    void testToString()
    {
        ToStringVerifier.forClass(TagPathConfiguration.class).withIgnoredFields("project").verify();
    }
}
