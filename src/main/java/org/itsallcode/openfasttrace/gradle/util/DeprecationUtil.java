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
package org.itsallcode.openfasttrace.gradle.util;

import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;

public class DeprecationUtil
{
    private DeprecationUtil()
    {
        // Not instantiable
    }

    // Deprecated in Gradle 5.0
    // New method: project.getObjects().fileProperty()
    @SuppressWarnings("deprecation")
    public static RegularFileProperty createFileProperty(Project project)
    {
        return project.getLayout().fileProperty();
    }
}
