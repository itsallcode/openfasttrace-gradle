/**
 * openfasttrace-gradle - Gradle plugin for tracing requirements using OpenFastTrace
 * Copyright (C) 2017 It's all code <christoph at users.sourceforge.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.itsallcode.openfasttrace.gradle;

public enum GradleTestConfig
{
    CURRENT_VERSION(null), SEVEN_ZERO("7.0"), SEVEN_FIVE("7.5.1");

    public final String gradleVersion;

    private GradleTestConfig(final String gradleVersion)
    {
        this.gradleVersion = gradleVersion;
    }

    public boolean supportedWithJvm()
    {
        final boolean isJava11 = System.getProperty("java.version").startsWith("11");
        if (this == SEVEN_ZERO)
        {
            return isJava11;
        }
        return true;
    }
}
