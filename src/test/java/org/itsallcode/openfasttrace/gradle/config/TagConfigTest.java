package org.itsallcode.openfasttrace.gradle.config;

import org.junit.jupiter.api.Test;

import com.jparams.verifier.tostring.ToStringVerifier;

class TagConfigTest
{
    @Test
    void testToString()
    {
        ToStringVerifier.forClass(TagConfig.class).withIgnoredFields("project").verify();
    }
}
