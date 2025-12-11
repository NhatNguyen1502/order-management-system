export interface Product {
  productId: string;
  name: string;
  description: string;
  price: number;
  category: string;
  createdAt: number;
  updatedAt: number;
}

export interface Order {
  orderId: string;
  customerId: string;
  items: OrderItem[];
  status: string;
  totalAmount: number;
  createdAt: number;
  updatedAt: number;
}

export interface OrderItem {
  productId: string;
  productName: string;
  quantity: number;
  price: number;
}

export interface Inventory {
  productId: string;
  availableQuantity: number;
  reservedQuantity: number;
  updatedAt: number;
}

export interface CreateOrderRequest {
  customerId: string;
  items: OrderItem[];
}

export interface CreateProductRequest {
  name: string;
  description: string;
  price: number;
  category: string;
}
