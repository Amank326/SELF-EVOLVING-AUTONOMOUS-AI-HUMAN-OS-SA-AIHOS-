const { queryRef, executeQuery, mutationRef, executeMutation, validateArgs } = require('firebase/data-connect');

const connectorConfig = {
  connector: 'example',
  service: 'sa-aihos',
  location: 'us-east4'
};
exports.connectorConfig = connectorConfig;

const createSystemEventRef = (dc) => {
  const { dc: dcInstance} = validateArgs(connectorConfig, dc, undefined);
  dcInstance._useGeneratedSdk();
  return mutationRef(dcInstance, 'CreateSystemEvent');
}
createSystemEventRef.operationName = 'CreateSystemEvent';
exports.createSystemEventRef = createSystemEventRef;

exports.createSystemEvent = function createSystemEvent(dc) {
  return executeMutation(createSystemEventRef(dc));
};

const listSystemEventsRef = (dc) => {
  const { dc: dcInstance} = validateArgs(connectorConfig, dc, undefined);
  dcInstance._useGeneratedSdk();
  return queryRef(dcInstance, 'ListSystemEvents');
}
listSystemEventsRef.operationName = 'ListSystemEvents';
exports.listSystemEventsRef = listSystemEventsRef;

exports.listSystemEvents = function listSystemEvents(dc) {
  return executeQuery(listSystemEventsRef(dc));
};

const updateRuleRef = (dcOrVars, vars) => {
  const { dc: dcInstance, vars: inputVars} = validateArgs(connectorConfig, dcOrVars, vars, true);
  dcInstance._useGeneratedSdk();
  return mutationRef(dcInstance, 'UpdateRule', inputVars);
}
updateRuleRef.operationName = 'UpdateRule';
exports.updateRuleRef = updateRuleRef;

exports.updateRule = function updateRule(dcOrVars, vars) {
  return executeMutation(updateRuleRef(dcOrVars, vars));
};

const listActiveRulesRef = (dc) => {
  const { dc: dcInstance} = validateArgs(connectorConfig, dc, undefined);
  dcInstance._useGeneratedSdk();
  return queryRef(dcInstance, 'ListActiveRules');
}
listActiveRulesRef.operationName = 'ListActiveRules';
exports.listActiveRulesRef = listActiveRulesRef;

exports.listActiveRules = function listActiveRules(dc) {
  return executeQuery(listActiveRulesRef(dc));
};
