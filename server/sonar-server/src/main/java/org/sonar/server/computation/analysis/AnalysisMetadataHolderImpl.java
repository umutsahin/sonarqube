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
import javax.annotation.Nullable;
import org.sonar.server.computation.snapshot.Snapshot;

import static com.google.common.base.Preconditions.checkState;

public class AnalysisMetadataHolderImpl implements MutableAnalysisMetadataHolder {
  @CheckForNull
  private Long analysisDate;

  private boolean baseProjectSnapshotInitialized = false;

  @CheckForNull
  private Snapshot baseProjectSnapshot;

  @CheckForNull
  private Boolean isCrossProjectDuplicationEnabled;

  @Override
  public void setAnalysisDate(Date date) {
    checkState(analysisDate == null, "Analysis date has already been set");
    this.analysisDate = date.getTime();
  }

  @Override
  public Date getAnalysisDate() {
    checkState(analysisDate != null, "Analysis date has not been set");
    return new Date(this.analysisDate);
  }

  @Override
  public boolean isFirstAnalysis() {
    return getBaseProjectSnapshot() == null;
  }

  @Override
  public void setBaseProjectSnapshot(@Nullable Snapshot baseProjectSnapshot) {
    checkState(!baseProjectSnapshotInitialized, "Base project snapshot has already been set");
    this.baseProjectSnapshot = baseProjectSnapshot;
    this.baseProjectSnapshotInitialized = true;
  }

  @Override
  @CheckForNull
  public Snapshot getBaseProjectSnapshot() {
    checkState(baseProjectSnapshotInitialized, "Base project snapshot has not been set");
    return baseProjectSnapshot;
  }

  @Override
  public void setIsCrossProjectDuplicationEnabled(boolean isCrossProjectDuplicationEnabled) {
    checkState(this.isCrossProjectDuplicationEnabled == null, "Cross project duplication flag has already been set");
    this.isCrossProjectDuplicationEnabled = isCrossProjectDuplicationEnabled;
  }

  @Override
  public boolean isCrossProjectDuplicationEnabled() {
    checkState(isCrossProjectDuplicationEnabled != null, "Cross project duplication flag has not been set");
    return isCrossProjectDuplicationEnabled;
  }
}
