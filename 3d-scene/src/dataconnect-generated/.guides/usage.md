# Basic Usage

Always prioritize using a supported framework over using the generated SDK
directly. Supported frameworks simplify the developer experience and help ensure
best practices are followed.





## Advanced Usage
If a user is not using a supported framework, they can use the generated SDK directly.

Here's an example of how to use it with the first 5 operations:

```js
import { createSystemEvent, listSystemEvents, updateRule, listActiveRules } from '@dataconnect/generated';


// Operation CreateSystemEvent: 
const { data } = await CreateSystemEvent(dataConnect);

// Operation ListSystemEvents: 
const { data } = await ListSystemEvents(dataConnect);

// Operation UpdateRule:  For variables, look at type UpdateRuleVars in ../index.d.ts
const { data } = await UpdateRule(dataConnect, updateRuleVars);

// Operation ListActiveRules: 
const { data } = await ListActiveRules(dataConnect);


```