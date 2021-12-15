export interface BillingPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
