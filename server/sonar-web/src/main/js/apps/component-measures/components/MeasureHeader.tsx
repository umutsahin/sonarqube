/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import * as React from 'react';
import { Link } from 'react-router';
import LanguageDistribution from '../../../components/charts/LanguageDistribution';
import Tooltip from '../../../components/controls/Tooltip';
import HistoryIcon from '../../../components/icons/HistoryIcon';
import IssueTypeIcon from '../../../components/icons/IssueTypeIcon';
import Measure from '../../../components/measure/Measure';
import { getLocalizedMetricName, translate } from '../../../helpers/l10n';
import { isDiffMetric } from '../../../helpers/measures';
import { getMeasureHistoryUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { ComponentMeasure, Measure as TypeMeasure, Metric, Period } from '../../../types/types';
import { hasFullMeasures } from '../utils';
import LeakPeriodLegend from './LeakPeriodLegend';

interface Props {
  branchLike?: BranchLike;
  component: ComponentMeasure;
  leakPeriod?: Period;
  measureValue?: string;
  metric: Metric;
  secondaryMeasure?: TypeMeasure;
}

export default function MeasureHeader(props: Props) {
  const { branchLike, component, leakPeriod, measureValue, metric, secondaryMeasure } = props;
  const isDiff = isDiffMetric(metric.key);
  const hasHistory =
    ['VW', 'SVW', 'APP', 'TRK'].includes(component.qualifier) && hasFullMeasures(branchLike);
  const displayLeak = hasFullMeasures(branchLike);
  return (
    <div className="measure-details-header big-spacer-bottom">
      <div className="measure-details-primary">
        <div className="measure-details-metric">
          <IssueTypeIcon className="little-spacer-right text-text-bottom" query={metric.key} />
          {getLocalizedMetricName(metric)}
          <span className="measure-details-value spacer-left">
            <strong>
              <Measure
                className={isDiff && displayLeak ? 'leak-box' : undefined}
                metricKey={metric.key}
                metricType={metric.type}
                value={measureValue}
              />
            </strong>
          </span>
          {!isDiff && hasHistory && (
            <Tooltip overlay={translate('component_measures.show_metric_history')}>
              <Link
                className="js-show-history spacer-left button button-small"
                to={getMeasureHistoryUrl(component.key, metric.key, branchLike)}>
                <HistoryIcon />
              </Link>
            </Tooltip>
          )}
        </div>
        <div className="measure-details-primary-actions">
          {displayLeak && leakPeriod && (
            <LeakPeriodLegend className="spacer-left" component={component} period={leakPeriod} />
          )}
        </div>
      </div>
      {secondaryMeasure &&
        secondaryMeasure.metric === 'ncloc_language_distribution' &&
        secondaryMeasure.value !== undefined && (
          <div className="measure-details-secondary">
            <LanguageDistribution distribution={secondaryMeasure.value} />
          </div>
        )}
    </div>
  );
}
