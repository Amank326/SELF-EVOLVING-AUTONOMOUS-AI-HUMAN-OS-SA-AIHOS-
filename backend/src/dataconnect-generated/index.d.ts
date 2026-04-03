import { ConnectorConfig, DataConnect, QueryRef, QueryPromise, MutationRef, MutationPromise } from 'firebase/data-connect';

export const connectorConfig: ConnectorConfig;

export type TimestampString = string;
export type UUIDString = string;
export type Int64String = string;
export type DateString = string;




export interface Application_Key {
  id: UUIDString;
  __typename?: 'Application_Key';
}

export interface CreateSystemEventData {
  systemEvent_insert: SystemEvent_Key;
}

export interface ListActiveRulesData {
  rules: ({
    id: UUIDString;
    name: string;
    description?: string | null;
  } & Rule_Key)[];
}

export interface ListSystemEventsData {
  systemEvents: ({
    id: UUIDString;
    eventType: string;
    message: string;
    timestamp: TimestampString;
  } & SystemEvent_Key)[];
}

export interface Optimization_Key {
  id: UUIDString;
  __typename?: 'Optimization_Key';
}

export interface Rule_Key {
  id: UUIDString;
  __typename?: 'Rule_Key';
}

export interface SystemEvent_Key {
  id: UUIDString;
  __typename?: 'SystemEvent_Key';
}

export interface SystemMetric_Key {
  id: UUIDString;
  __typename?: 'SystemMetric_Key';
}

export interface UpdateRuleData {
  rule_update?: Rule_Key | null;
}

export interface UpdateRuleVariables {
  id: UUIDString;
  isActive?: boolean | null;
}

export interface User_Key {
  id: UUIDString;
  __typename?: 'User_Key';
}

interface CreateSystemEventRef {
  /* Allow users to create refs without passing in DataConnect */
  (): MutationRef<CreateSystemEventData, undefined>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect): MutationRef<CreateSystemEventData, undefined>;
  operationName: string;
}
export const createSystemEventRef: CreateSystemEventRef;

export function createSystemEvent(): MutationPromise<CreateSystemEventData, undefined>;
export function createSystemEvent(dc: DataConnect): MutationPromise<CreateSystemEventData, undefined>;

interface ListSystemEventsRef {
  /* Allow users to create refs without passing in DataConnect */
  (): QueryRef<ListSystemEventsData, undefined>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect): QueryRef<ListSystemEventsData, undefined>;
  operationName: string;
}
export const listSystemEventsRef: ListSystemEventsRef;

export function listSystemEvents(): QueryPromise<ListSystemEventsData, undefined>;
export function listSystemEvents(dc: DataConnect): QueryPromise<ListSystemEventsData, undefined>;

interface UpdateRuleRef {
  /* Allow users to create refs without passing in DataConnect */
  (vars: UpdateRuleVariables): MutationRef<UpdateRuleData, UpdateRuleVariables>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect, vars: UpdateRuleVariables): MutationRef<UpdateRuleData, UpdateRuleVariables>;
  operationName: string;
}
export const updateRuleRef: UpdateRuleRef;

export function updateRule(vars: UpdateRuleVariables): MutationPromise<UpdateRuleData, UpdateRuleVariables>;
export function updateRule(dc: DataConnect, vars: UpdateRuleVariables): MutationPromise<UpdateRuleData, UpdateRuleVariables>;

interface ListActiveRulesRef {
  /* Allow users to create refs without passing in DataConnect */
  (): QueryRef<ListActiveRulesData, undefined>;
  /* Allow users to pass in custom DataConnect instances */
  (dc: DataConnect): QueryRef<ListActiveRulesData, undefined>;
  operationName: string;
}
export const listActiveRulesRef: ListActiveRulesRef;

export function listActiveRules(): QueryPromise<ListActiveRulesData, undefined>;
export function listActiveRules(dc: DataConnect): QueryPromise<ListActiveRulesData, undefined>;

