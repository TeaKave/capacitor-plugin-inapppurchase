# capacitor-plugin-inapppurchase

This plugin allows In-App Purchases to be made from Capacitor applications.

## Install

```bash
npm install capacitor-plugin-inapppurchase
npx cap sync
```

## API

<docgen-index>

* [`getProducts(...)`](#getproducts)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### getProducts(...)

```typescript
getProducts(products: { values: Product[]; }) => any
```

| Param          | Type                         |
| -------------- | ---------------------------- |
| **`products`** | <code>{ values: {}; }</code> |

**Returns:** <code>any</code>

--------------------


### Interfaces


#### Product

| Prop                 | Type                 |
| -------------------- | -------------------- |
| **`id`**             | <code>string</code>  |
| **`isSubscription`** | <code>boolean</code> |

</docgen-api>
