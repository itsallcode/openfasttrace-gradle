package org.itsallcode.openfasttrace.gradle.task.config;

import org.junit.jupiter.api.Test;

import com.jparams.verifier.tostring.ToStringVerifier;

class SerializableTagPathConfigTest
{
    @Test
    void testToString()
    {
        ToStringVerifier.forClass(SerializableTagPathConfig.class).verify();
    }
}
