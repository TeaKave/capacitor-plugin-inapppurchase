import { WebPlugin } from '@capacitor/core';
import { BillingPlugin, Product } from './definitions';

export class BillingWeb extends WebPlugin implements BillingPlugin {
  constructor() {
    super({
      name: 'BillingPlugin',
      platforms: ['web'],
    });
  }

  // @ts-ignore
  async getProducts(products: {
    values: Product[];
  }): Promise<{ items: string }> {
    return { items: 'web' };
  }

  // @ts-ignore
  async subscribe(products: { values: Product[] }): Promise<{ items: string }> {
    return { items: 'web' };
  }

  // @ts-ignore
  async buy(products: { values: Product[] }): Promise<{ items: string }> {
    return { items: 'web' };
  }

  // @ts-ignore
  async consume(products: { values: Product[] }): Promise<{ items: string }> {
    return { items: 'web' };
  }

  // @ts-ignore
  async restorePurchases(products: {
    values: Product[];
  }): Promise<{ items: string }> {
    return { items: 'web' };
  }
}
