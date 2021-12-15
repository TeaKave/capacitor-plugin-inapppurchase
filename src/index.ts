import { registerPlugin } from '@capacitor/core';

import type { BillingPlugin } from './definitions';

const Billing = registerPlugin<BillingPlugin>('Billing', {
  web: () => import('./web').then(m => new m.BillingWeb()),
});

export * from './definitions';
export { Billing };
