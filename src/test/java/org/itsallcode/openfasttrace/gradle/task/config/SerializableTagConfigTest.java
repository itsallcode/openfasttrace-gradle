package org.itsallcode.openfasttrace.gradle.task.config;

import org.junit.jupiter.api.Test;

import com.jparams.verifier.tostring.ToStringVerifier;

class SerializableTagConfigTest
{
    @Test
    void testToString()
    {
        ToStringVerifier.forClass(SerializableTagConfig.class).verify();
    }
}
