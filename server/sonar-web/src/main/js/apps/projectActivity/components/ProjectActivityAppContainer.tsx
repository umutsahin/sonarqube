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
import { Location } from 'history';
import * as React from 'react';
import { InjectedRouter } from 'react-router';
import { getAllMetrics } from '../../../api/metrics';
import {
  changeEvent,
  createEvent,
  deleteAnalysis,
  deleteEvent,
  getProjectActivity,
  ProjectActivityStatuses
} from '../../../api/projectActivity';
import { getAllTimeMachineData } from '../../../api/time-machine';
import {
  DEFAULT_GRAPH,
  getActivityGraph,
  getHistoryMetrics,
  isCustomGraph
} from '../../../components/activity-graph/utils';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { parseDate } from '../../../helpers/dates';
import { serializeStringArray } from '../../../helpers/query';
import { BranchLike } from '../../../types/branch-like';
import { MetricKey } from '../../../types/metrics';
import { GraphType, MeasureHistory } from '../../../types/project-activity';
import { Component, Metric, Paging, ParsedAnalysis, RawQuery } from '../../../types/types';
import * as actions from '../actions';
import {
  customMetricsChanged,
  parseQuery,
  Query,
  serializeQuery,
  serializeUrlQuery
} from '../utils';
import ProjectActivityApp from './ProjectActivityApp';

interface Props {
  branchLike?: BranchLike;
  component: Component;
  location: Location;
  router: Pick<InjectedRouter, 'push' | 'replace'>;
}

export interface State {
  analyses: ParsedAnalysis[];
  analysesLoading: boolean;
  graphLoading: boolean;
  initialized: boolean;
  metrics: Metric[];
  measuresHistory: MeasureHistory[];
  query: Query;
}

export const PROJECT_ACTIVITY_GRAPH = 'sonar_project_activity.graph';

const ACTIVITY_PAGE_SIZE_FIRST_BATCH = 100;
const ACTIVITY_PAGE_SIZE = 500;

export default class ProjectActivityAppContainer extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      analyses: [],
      analysesLoading: false,
      graphLoading: true,
      initialized: false,
      measuresHistory: [],
      metrics: [],
      query: parseQuery(props.location.query)
    };
  }

  componentDidMount() {
    this.mounted = true;
    if (this.shouldRedirect()) {
      const { graph, customGraphs } = getActivityGraph(
        PROJECT_ACTIVITY_GRAPH,
        this.props.component.key
      );
      const newQuery = { ...this.state.query, graph };
      if (isCustomGraph(newQuery.graph)) {
        newQuery.customMetrics = customGraphs;
      }
      this.props.router.replace({
        pathname: this.props.location.pathname,
        query: {
          ...serializeUrlQuery(newQuery),
          ...getBranchLikeQuery(this.props.branchLike)
        }
      });
    } else {
      this.firstLoadData(this.state.query, this.props.component);
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.location.query !== this.props.location.query) {
      const query = parseQuery(this.props.location.query);
      if (query.graph !== this.state.query.graph || customMetricsChanged(this.state.query, query)) {
        if (this.state.initialized) {
          this.updateGraphData(query.graph || DEFAULT_GRAPH, query.customMetrics);
        } else {
          this.firstLoadData(query, this.props.component);
        }
      }
      this.setState({ query });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  addCustomEvent = (analysisKey: string, name: string, category?: string) => {
    return createEvent(analysisKey, name, category).then(({ analysis, ...event }) => {
      if (this.mounted) {
        this.setState(actions.addCustomEvent(analysis, event));
      }
    });
  };

  addVersion = (analysis: string, version: string) => {
    return this.addCustomEvent(analysis, version, 'VERSION');
  };

  changeEvent = (eventKey: string, name: string) => {
    return changeEvent(eventKey, name).then(({ analysis, ...event }) => {
      if (this.mounted) {
        this.setState(actions.changeEvent(analysis, event));
      }
    });
  };

  deleteAnalysis = (analysis: string) => {
    return deleteAnalysis(analysis).then(() => {
      if (this.mounted) {
        this.updateGraphData(
          this.state.query.graph || DEFAULT_GRAPH,
          this.state.query.customMetrics
        );
        this.setState(actions.deleteAnalysis(analysis));
      }
    });
  };

  deleteEvent = (analysis: string, event: string) => {
    return deleteEvent(event).then(() => {
      if (this.mounted) {
        this.setState(actions.deleteEvent(analysis, event));
      }
    });
  };

  fetchActivity = (
    project: string,
    statuses: ProjectActivityStatuses[],
    p: number,
    ps: number,
    additional?: RawQuery
  ) => {
    const parameters = {
      project,
      statuses: serializeStringArray(statuses),
      p,
      ps,
      ...getBranchLikeQuery(this.props.branchLike)
    };
    return getProjectActivity({ ...additional, ...parameters }).then(({ analyses, paging }) => ({
      analyses: analyses.map(analysis => ({
        ...analysis,
        date: parseDate(analysis.date)
      })) as ParsedAnalysis[],
      paging
    }));
  };

  fetchMeasuresHistory = (metrics: string[]): Promise<MeasureHistory[]> => {
    if (metrics.length <= 0) {
      return Promise.resolve([]);
    }
    return getAllTimeMachineData({
      component: this.props.component.key,
      metrics: metrics.join(),
      ...getBranchLikeQuery(this.props.branchLike)
    }).then(({ measures }) =>
      measures.map(measure => ({
        metric: measure.metric,
        history: measure.history.map(analysis => ({
          date: parseDate(analysis.date),
          value: analysis.value!
        }))
      }))
    );
  };

  fetchAllActivities = (topLevelComponent: string) => {
    this.setState({ analysesLoading: true });
    this.loadAllActivities(topLevelComponent).then(
      ({ analyses }) => {
        if (this.mounted) {
          this.setState({
            analyses,
            analysesLoading: false
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ analysesLoading: false });
        }
      }
    );
  };

  loadAllActivities = (
    project: string,
    prevResult?: { analyses: ParsedAnalysis[]; paging: Paging }
  ): Promise<{ analyses: ParsedAnalysis[]; paging: Paging }> => {
    if (
      prevResult &&
      prevResult.paging.pageIndex * prevResult.paging.pageSize >= prevResult.paging.total
    ) {
      return Promise.resolve(prevResult);
    }
    const nextPage = prevResult ? prevResult.paging.pageIndex + 1 : 1;
    return this.fetchActivity(
      project,
      [
        ProjectActivityStatuses.STATUS_PROCESSED,
        ProjectActivityStatuses.STATUS_LIVE_MEASURE_COMPUTE
      ],
      nextPage,
      ACTIVITY_PAGE_SIZE
    ).then(result => {
      if (!prevResult) {
        return this.loadAllActivities(project, result);
      }
      return this.loadAllActivities(project, {
        analyses: prevResult.analyses.concat(result.analyses),
        paging: result.paging
      });
    });
  };

  getTopLevelComponent = (component: Component) => {
    let current = component.breadcrumbs.length - 1;
    while (
      current > 0 &&
      !['TRK', 'VW', 'APP'].includes(component.breadcrumbs[current].qualifier)
    ) {
      current--;
    }
    return component.breadcrumbs[current].key;
  };

  filterMetrics({ qualifier }: Component, metrics: Metric[]) {
    return ['VW', 'SVW'].includes(qualifier)
      ? metrics.filter(metric => metric.key !== MetricKey.security_hotspots_reviewed)
      : metrics.filter(metric => metric.key !== MetricKey.security_review_rating);
  }

  firstLoadData(query: Query, component: Component) {
    const graphMetrics = getHistoryMetrics(query.graph || DEFAULT_GRAPH, query.customMetrics);
    const topLevelComponent = this.getTopLevelComponent(component);
    Promise.all([
      this.fetchActivity(
        topLevelComponent,
        [
          ProjectActivityStatuses.STATUS_PROCESSED,
          ProjectActivityStatuses.STATUS_LIVE_MEASURE_COMPUTE
        ],
        1,
        ACTIVITY_PAGE_SIZE_FIRST_BATCH,
        serializeQuery(query)
      ),
      getAllMetrics(),
      this.fetchMeasuresHistory(graphMetrics)
    ]).then(
      ([{ analyses }, metrics, measuresHistory]) => {
        if (this.mounted) {
          this.setState({
            analyses,
            graphLoading: false,
            initialized: true,
            measuresHistory,
            metrics: this.filterMetrics(component, metrics)
          });

          this.fetchAllActivities(topLevelComponent);
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ initialized: true, graphLoading: false });
        }
      }
    );
  }

  updateGraphData = (graph: GraphType, customMetrics: string[]) => {
    const graphMetrics = getHistoryMetrics(graph, customMetrics);
    this.setState({ graphLoading: true });
    this.fetchMeasuresHistory(graphMetrics).then(
      measuresHistory => {
        if (this.mounted) {
          this.setState({ graphLoading: false, measuresHistory });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ graphLoading: false, measuresHistory: [] });
        }
      }
    );
  };

  updateQuery = (newQuery: Query) => {
    const query = serializeUrlQuery({
      ...this.state.query,
      ...newQuery
    });
    this.props.router.push({
      pathname: this.props.location.pathname,
      query: {
        ...query,
        ...getBranchLikeQuery(this.props.branchLike),
        id: this.props.component.key
      }
    });
  };

  shouldRedirect = () => {
    const locationQuery = this.props.location.query;
    if (!locationQuery) {
      return false;
    }
    const filtered = Object.keys(locationQuery).some(
      key => key !== 'id' && locationQuery[key] !== ''
    );

    const { graph, customGraphs } = getActivityGraph(
      PROJECT_ACTIVITY_GRAPH,
      this.props.component.key
    );
    const emptyCustomGraph = isCustomGraph(graph) && customGraphs.length <= 0;

    // if there is no filter, but there are saved preferences in the localStorage
    // also don't redirect to custom if there is no metrics selected for it
    return !filtered && graph != null && graph !== DEFAULT_GRAPH && !emptyCustomGraph;
  };

  render() {
    if (this.shouldRedirect()) {
      return null;
    }

    return (
      <ProjectActivityApp
        addCustomEvent={this.addCustomEvent}
        addVersion={this.addVersion}
        analyses={this.state.analyses}
        analysesLoading={this.state.analysesLoading}
        changeEvent={this.changeEvent}
        deleteAnalysis={this.deleteAnalysis}
        deleteEvent={this.deleteEvent}
        graphLoading={!this.state.initialized || this.state.graphLoading}
        initializing={!this.state.initialized}
        measuresHistory={this.state.measuresHistory}
        metrics={this.state.metrics}
        project={this.props.component}
        query={this.state.query}
        updateQuery={this.updateQuery}
      />
    );
  }
}
