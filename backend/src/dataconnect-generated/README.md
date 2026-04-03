# Generated TypeScript README
This README will guide you through the process of using the generated JavaScript SDK package for the connector `example`. It will also provide examples on how to use your generated SDK to call your Data Connect queries and mutations.

***NOTE:** This README is generated alongside the generated SDK. If you make changes to this file, they will be overwritten when the SDK is regenerated.*

# Table of Contents
- [**Overview**](#generated-javascript-readme)
- [**Accessing the connector**](#accessing-the-connector)
  - [*Connecting to the local Emulator*](#connecting-to-the-local-emulator)
- [**Queries**](#queries)
  - [*ListSystemEvents*](#listsystemevents)
  - [*ListActiveRules*](#listactiverules)
- [**Mutations**](#mutations)
  - [*CreateSystemEvent*](#createsystemevent)
  - [*UpdateRule*](#updaterule)

# Accessing the connector
A connector is a collection of Queries and Mutations. One SDK is generated for each connector - this SDK is generated for the connector `example`. You can find more information about connectors in the [Data Connect documentation](https://firebase.google.com/docs/data-connect#how-does).

You can use this generated SDK by importing from the package `@dataconnect/generated` as shown below. Both CommonJS and ESM imports are supported.

You can also follow the instructions from the [Data Connect documentation](https://firebase.google.com/docs/data-connect/web-sdk#set-client).

```typescript
import { getDataConnect } from 'firebase/data-connect';
import { connectorConfig } from '@dataconnect/generated';

const dataConnect = getDataConnect(connectorConfig);
```

## Connecting to the local Emulator
By default, the connector will connect to the production service.

To connect to the emulator, you can use the following code.
You can also follow the emulator instructions from the [Data Connect documentation](https://firebase.google.com/docs/data-connect/web-sdk#instrument-clients).

```typescript
import { connectDataConnectEmulator, getDataConnect } from 'firebase/data-connect';
import { connectorConfig } from '@dataconnect/generated';

const dataConnect = getDataConnect(connectorConfig);
connectDataConnectEmulator(dataConnect, 'localhost', 9399);
```

After it's initialized, you can call your Data Connect [queries](#queries) and [mutations](#mutations) from your generated SDK.

# Queries

There are two ways to execute a Data Connect Query using the generated Web SDK:
- Using a Query Reference function, which returns a `QueryRef`
  - The `QueryRef` can be used as an argument to `executeQuery()`, which will execute the Query and return a `QueryPromise`
- Using an action shortcut function, which returns a `QueryPromise`
  - Calling the action shortcut function will execute the Query and return a `QueryPromise`

The following is true for both the action shortcut function and the `QueryRef` function:
- The `QueryPromise` returned will resolve to the result of the Query once it has finished executing
- If the Query accepts arguments, both the action shortcut function and the `QueryRef` function accept a single argument: an object that contains all the required variables (and the optional variables) for the Query
- Both functions can be called with or without passing in a `DataConnect` instance as an argument. If no `DataConnect` argument is passed in, then the generated SDK will call `getDataConnect(connectorConfig)` behind the scenes for you.

Below are examples of how to use the `example` connector's generated functions to execute each query. You can also follow the examples from the [Data Connect documentation](https://firebase.google.com/docs/data-connect/web-sdk#using-queries).

## ListSystemEvents
You can execute the `ListSystemEvents` query using the following action shortcut function, or by calling `executeQuery()` after calling the following `QueryRef` function, both of which are defined in [dataconnect-generated/index.d.ts](./index.d.ts):
```typescript
listSystemEvents(): QueryPromise<ListSystemEventsData, undefined>;

interface ListSystemEventsRef {
  ...
  /* Allow users to create refs without passing in DataConnect */
  (): QueryRef<ListSystemEventsData, undefined>;
}
export const listSystemEventsRef: ListSystemEventsRef;
```
You can also pass in a `DataConnect` instance to the action shortcut function or `QueryRef` function.
```typescript
listSystemEvents(dc: DataConnect): QueryPromise<ListSystemEventsData, undefined>;

interface ListSystemEventsRef {
  ...
  (dc: DataConnect): QueryRef<ListSystemEventsData, undefined>;
}
export const listSystemEventsRef: ListSystemEventsRef;
```

If you need the name of the operation without creating a ref, you can retrieve the operation name by calling the `operationName` property on the listSystemEventsRef:
```typescript
const name = listSystemEventsRef.operationName;
console.log(name);
```

### Variables
The `ListSystemEvents` query has no variables.
### Return Type
Recall that executing the `ListSystemEvents` query returns a `QueryPromise` that resolves to an object with a `data` property.

The `data` property is an object of type `ListSystemEventsData`, which is defined in [dataconnect-generated/index.d.ts](./index.d.ts). It has the following fields:
```typescript
export interface ListSystemEventsData {
  systemEvents: ({
    id: UUIDString;
    eventType: string;
    message: string;
    timestamp: TimestampString;
  } & SystemEvent_Key)[];
}
```
### Using `ListSystemEvents`'s action shortcut function

```typescript
import { getDataConnect } from 'firebase/data-connect';
import { connectorConfig, listSystemEvents } from '@dataconnect/generated';


// Call the `listSystemEvents()` function to execute the query.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await listSystemEvents();

// You can also pass in a `DataConnect` instance to the action shortcut function.
const dataConnect = getDataConnect(connectorConfig);
const { data } = await listSystemEvents(dataConnect);

console.log(data.systemEvents);

// Or, you can use the `Promise` API.
listSystemEvents().then((response) => {
  const data = response.data;
  console.log(data.systemEvents);
});
```

### Using `ListSystemEvents`'s `QueryRef` function

```typescript
import { getDataConnect, executeQuery } from 'firebase/data-connect';
import { connectorConfig, listSystemEventsRef } from '@dataconnect/generated';


// Call the `listSystemEventsRef()` function to get a reference to the query.
const ref = listSystemEventsRef();

// You can also pass in a `DataConnect` instance to the `QueryRef` function.
const dataConnect = getDataConnect(connectorConfig);
const ref = listSystemEventsRef(dataConnect);

// Call `executeQuery()` on the reference to execute the query.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await executeQuery(ref);

console.log(data.systemEvents);

// Or, you can use the `Promise` API.
executeQuery(ref).then((response) => {
  const data = response.data;
  console.log(data.systemEvents);
});
```

## ListActiveRules
You can execute the `ListActiveRules` query using the following action shortcut function, or by calling `executeQuery()` after calling the following `QueryRef` function, both of which are defined in [dataconnect-generated/index.d.ts](./index.d.ts):
```typescript
listActiveRules(): QueryPromise<ListActiveRulesData, undefined>;

interface ListActiveRulesRef {
  ...
  /* Allow users to create refs without passing in DataConnect */
  (): QueryRef<ListActiveRulesData, undefined>;
}
export const listActiveRulesRef: ListActiveRulesRef;
```
You can also pass in a `DataConnect` instance to the action shortcut function or `QueryRef` function.
```typescript
listActiveRules(dc: DataConnect): QueryPromise<ListActiveRulesData, undefined>;

interface ListActiveRulesRef {
  ...
  (dc: DataConnect): QueryRef<ListActiveRulesData, undefined>;
}
export const listActiveRulesRef: ListActiveRulesRef;
```

If you need the name of the operation without creating a ref, you can retrieve the operation name by calling the `operationName` property on the listActiveRulesRef:
```typescript
const name = listActiveRulesRef.operationName;
console.log(name);
```

### Variables
The `ListActiveRules` query has no variables.
### Return Type
Recall that executing the `ListActiveRules` query returns a `QueryPromise` that resolves to an object with a `data` property.

The `data` property is an object of type `ListActiveRulesData`, which is defined in [dataconnect-generated/index.d.ts](./index.d.ts). It has the following fields:
```typescript
export interface ListActiveRulesData {
  rules: ({
    id: UUIDString;
    name: string;
    description?: string | null;
  } & Rule_Key)[];
}
```
### Using `ListActiveRules`'s action shortcut function

```typescript
import { getDataConnect } from 'firebase/data-connect';
import { connectorConfig, listActiveRules } from '@dataconnect/generated';


// Call the `listActiveRules()` function to execute the query.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await listActiveRules();

// You can also pass in a `DataConnect` instance to the action shortcut function.
const dataConnect = getDataConnect(connectorConfig);
const { data } = await listActiveRules(dataConnect);

console.log(data.rules);

// Or, you can use the `Promise` API.
listActiveRules().then((response) => {
  const data = response.data;
  console.log(data.rules);
});
```

### Using `ListActiveRules`'s `QueryRef` function

```typescript
import { getDataConnect, executeQuery } from 'firebase/data-connect';
import { connectorConfig, listActiveRulesRef } from '@dataconnect/generated';


// Call the `listActiveRulesRef()` function to get a reference to the query.
const ref = listActiveRulesRef();

// You can also pass in a `DataConnect` instance to the `QueryRef` function.
const dataConnect = getDataConnect(connectorConfig);
const ref = listActiveRulesRef(dataConnect);

// Call `executeQuery()` on the reference to execute the query.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await executeQuery(ref);

console.log(data.rules);

// Or, you can use the `Promise` API.
executeQuery(ref).then((response) => {
  const data = response.data;
  console.log(data.rules);
});
```

# Mutations

There are two ways to execute a Data Connect Mutation using the generated Web SDK:
- Using a Mutation Reference function, which returns a `MutationRef`
  - The `MutationRef` can be used as an argument to `executeMutation()`, which will execute the Mutation and return a `MutationPromise`
- Using an action shortcut function, which returns a `MutationPromise`
  - Calling the action shortcut function will execute the Mutation and return a `MutationPromise`

The following is true for both the action shortcut function and the `MutationRef` function:
- The `MutationPromise` returned will resolve to the result of the Mutation once it has finished executing
- If the Mutation accepts arguments, both the action shortcut function and the `MutationRef` function accept a single argument: an object that contains all the required variables (and the optional variables) for the Mutation
- Both functions can be called with or without passing in a `DataConnect` instance as an argument. If no `DataConnect` argument is passed in, then the generated SDK will call `getDataConnect(connectorConfig)` behind the scenes for you.

Below are examples of how to use the `example` connector's generated functions to execute each mutation. You can also follow the examples from the [Data Connect documentation](https://firebase.google.com/docs/data-connect/web-sdk#using-mutations).

## CreateSystemEvent
You can execute the `CreateSystemEvent` mutation using the following action shortcut function, or by calling `executeMutation()` after calling the following `MutationRef` function, both of which are defined in [dataconnect-generated/index.d.ts](./index.d.ts):
```typescript
createSystemEvent(): MutationPromise<CreateSystemEventData, undefined>;

interface CreateSystemEventRef {
  ...
  /* Allow users to create refs without passing in DataConnect */
  (): MutationRef<CreateSystemEventData, undefined>;
}
export const createSystemEventRef: CreateSystemEventRef;
```
You can also pass in a `DataConnect` instance to the action shortcut function or `MutationRef` function.
```typescript
createSystemEvent(dc: DataConnect): MutationPromise<CreateSystemEventData, undefined>;

interface CreateSystemEventRef {
  ...
  (dc: DataConnect): MutationRef<CreateSystemEventData, undefined>;
}
export const createSystemEventRef: CreateSystemEventRef;
```

If you need the name of the operation without creating a ref, you can retrieve the operation name by calling the `operationName` property on the createSystemEventRef:
```typescript
const name = createSystemEventRef.operationName;
console.log(name);
```

### Variables
The `CreateSystemEvent` mutation has no variables.
### Return Type
Recall that executing the `CreateSystemEvent` mutation returns a `MutationPromise` that resolves to an object with a `data` property.

The `data` property is an object of type `CreateSystemEventData`, which is defined in [dataconnect-generated/index.d.ts](./index.d.ts). It has the following fields:
```typescript
export interface CreateSystemEventData {
  systemEvent_insert: SystemEvent_Key;
}
```
### Using `CreateSystemEvent`'s action shortcut function

```typescript
import { getDataConnect } from 'firebase/data-connect';
import { connectorConfig, createSystemEvent } from '@dataconnect/generated';


// Call the `createSystemEvent()` function to execute the mutation.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await createSystemEvent();

// You can also pass in a `DataConnect` instance to the action shortcut function.
const dataConnect = getDataConnect(connectorConfig);
const { data } = await createSystemEvent(dataConnect);

console.log(data.systemEvent_insert);

// Or, you can use the `Promise` API.
createSystemEvent().then((response) => {
  const data = response.data;
  console.log(data.systemEvent_insert);
});
```

### Using `CreateSystemEvent`'s `MutationRef` function

```typescript
import { getDataConnect, executeMutation } from 'firebase/data-connect';
import { connectorConfig, createSystemEventRef } from '@dataconnect/generated';


// Call the `createSystemEventRef()` function to get a reference to the mutation.
const ref = createSystemEventRef();

// You can also pass in a `DataConnect` instance to the `MutationRef` function.
const dataConnect = getDataConnect(connectorConfig);
const ref = createSystemEventRef(dataConnect);

// Call `executeMutation()` on the reference to execute the mutation.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await executeMutation(ref);

console.log(data.systemEvent_insert);

// Or, you can use the `Promise` API.
executeMutation(ref).then((response) => {
  const data = response.data;
  console.log(data.systemEvent_insert);
});
```

## UpdateRule
You can execute the `UpdateRule` mutation using the following action shortcut function, or by calling `executeMutation()` after calling the following `MutationRef` function, both of which are defined in [dataconnect-generated/index.d.ts](./index.d.ts):
```typescript
updateRule(vars: UpdateRuleVariables): MutationPromise<UpdateRuleData, UpdateRuleVariables>;

interface UpdateRuleRef {
  ...
  /* Allow users to create refs without passing in DataConnect */
  (vars: UpdateRuleVariables): MutationRef<UpdateRuleData, UpdateRuleVariables>;
}
export const updateRuleRef: UpdateRuleRef;
```
You can also pass in a `DataConnect` instance to the action shortcut function or `MutationRef` function.
```typescript
updateRule(dc: DataConnect, vars: UpdateRuleVariables): MutationPromise<UpdateRuleData, UpdateRuleVariables>;

interface UpdateRuleRef {
  ...
  (dc: DataConnect, vars: UpdateRuleVariables): MutationRef<UpdateRuleData, UpdateRuleVariables>;
}
export const updateRuleRef: UpdateRuleRef;
```

If you need the name of the operation without creating a ref, you can retrieve the operation name by calling the `operationName` property on the updateRuleRef:
```typescript
const name = updateRuleRef.operationName;
console.log(name);
```

### Variables
The `UpdateRule` mutation requires an argument of type `UpdateRuleVariables`, which is defined in [dataconnect-generated/index.d.ts](./index.d.ts). It has the following fields:

```typescript
export interface UpdateRuleVariables {
  id: UUIDString;
  isActive?: boolean | null;
}
```
### Return Type
Recall that executing the `UpdateRule` mutation returns a `MutationPromise` that resolves to an object with a `data` property.

The `data` property is an object of type `UpdateRuleData`, which is defined in [dataconnect-generated/index.d.ts](./index.d.ts). It has the following fields:
```typescript
export interface UpdateRuleData {
  rule_update?: Rule_Key | null;
}
```
### Using `UpdateRule`'s action shortcut function

```typescript
import { getDataConnect } from 'firebase/data-connect';
import { connectorConfig, updateRule, UpdateRuleVariables } from '@dataconnect/generated';

// The `UpdateRule` mutation requires an argument of type `UpdateRuleVariables`:
const updateRuleVars: UpdateRuleVariables = {
  id: ..., 
  isActive: ..., // optional
};

// Call the `updateRule()` function to execute the mutation.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await updateRule(updateRuleVars);
// Variables can be defined inline as well.
const { data } = await updateRule({ id: ..., isActive: ..., });

// You can also pass in a `DataConnect` instance to the action shortcut function.
const dataConnect = getDataConnect(connectorConfig);
const { data } = await updateRule(dataConnect, updateRuleVars);

console.log(data.rule_update);

// Or, you can use the `Promise` API.
updateRule(updateRuleVars).then((response) => {
  const data = response.data;
  console.log(data.rule_update);
});
```

### Using `UpdateRule`'s `MutationRef` function

```typescript
import { getDataConnect, executeMutation } from 'firebase/data-connect';
import { connectorConfig, updateRuleRef, UpdateRuleVariables } from '@dataconnect/generated';

// The `UpdateRule` mutation requires an argument of type `UpdateRuleVariables`:
const updateRuleVars: UpdateRuleVariables = {
  id: ..., 
  isActive: ..., // optional
};

// Call the `updateRuleRef()` function to get a reference to the mutation.
const ref = updateRuleRef(updateRuleVars);
// Variables can be defined inline as well.
const ref = updateRuleRef({ id: ..., isActive: ..., });

// You can also pass in a `DataConnect` instance to the `MutationRef` function.
const dataConnect = getDataConnect(connectorConfig);
const ref = updateRuleRef(dataConnect, updateRuleVars);

// Call `executeMutation()` on the reference to execute the mutation.
// You can use the `await` keyword to wait for the promise to resolve.
const { data } = await executeMutation(ref);

console.log(data.rule_update);

// Or, you can use the `Promise` API.
executeMutation(ref).then((response) => {
  const data = response.data;
  console.log(data.rule_update);
});
```

