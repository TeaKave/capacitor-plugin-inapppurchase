export interface BillingPlugin {
  getProducts(products: { values: Product[] }): Promise<{ items: string }>;
}

export interface Product {
  id: string;
  isSubscription: boolean;
}
