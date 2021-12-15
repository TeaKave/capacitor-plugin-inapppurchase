import { WebPlugin } from '@capacitor/core';

import type { BillingPlugin } from './definitions';

export class BillingWeb extends WebPlugin implements BillingPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
