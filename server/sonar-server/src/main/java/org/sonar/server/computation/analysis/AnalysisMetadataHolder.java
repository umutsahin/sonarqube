/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.analysis;

import java.util.Date;
import javax.annotation.CheckForNull;
import org.sonar.server.computation.snapshot.Snapshot;

public interface AnalysisMetadataHolder {
  /**
   * @throws IllegalStateException if no analysis date has been set
   */
  Date getAnalysisDate();

  /**
   * Convenience method equivalent to calling {@link #getBaseProjectSnapshot() == null}
   *
   * @throws IllegalStateException if baseProjectSnapshot has not been set
   */
  boolean isFirstAnalysis();

  /**
   * Return the last snapshot of the project.
   * If it's the first analysis, it will return null.
   *
   * @throws IllegalStateException if baseProjectSnapshot has not been set
   */
  @CheckForNull
  Snapshot getBaseProjectSnapshot();

  /**
   * @throws IllegalStateException if cross project duplication flag has not been set
   */
  boolean isCrossProjectDuplicationEnabled();

}
