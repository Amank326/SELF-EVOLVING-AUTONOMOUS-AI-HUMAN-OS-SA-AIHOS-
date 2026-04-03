import { queryRef, executeQuery, mutationRef, executeMutation, validateArgs } from 'firebase/data-connect';

export const connectorConfig = {
  connector: 'example',
  service: 'sa-aihos',
  location: 'us-east4'
};

export const createSystemEventRef = (dc) => {
  const { dc: dcInstance} = validateArgs(connectorConfig, dc, undefined);
  dcInstance._useGeneratedSdk();
  return mutationRef(dcInstance, 'CreateSystemEvent');
}
createSystemEventRef.operationName = 'CreateSystemEvent';

export function createSystemEvent(dc) {
  return executeMutation(createSystemEventRef(dc));
}

export const listSystemEventsRef = (dc) => {
  const { dc: dcInstance} = validateArgs(connectorConfig, dc, undefined);
  dcInstance._useGeneratedSdk();
  return queryRef(dcInstance, 'ListSystemEvents');
}
listSystemEventsRef.operationName = 'ListSystemEvents';

export function listSystemEvents(dc) {
  return executeQuery(listSystemEventsRef(dc));
}

export const updateRuleRef = (dcOrVars, vars) => {
  const { dc: dcInstance, vars: inputVars} = validateArgs(connectorConfig, dcOrVars, vars, true);
  dcInstance._useGeneratedSdk();
  return mutationRef(dcInstance, 'UpdateRule', inputVars);
}
updateRuleRef.operationName = 'UpdateRule';

export function updateRule(dcOrVars, vars) {
  return executeMutation(updateRuleRef(dcOrVars, vars));
}

export const listActiveRulesRef = (dc) => {
  const { dc: dcInstance} = validateArgs(connectorConfig, dc, undefined);
  dcInstance._useGeneratedSdk();
  return queryRef(dcInstance, 'ListActiveRules');
}
listActiveRulesRef.operationName = 'ListActiveRules';

export function listActiveRules(dc) {
  return executeQuery(listActiveRulesRef(dc));
}

