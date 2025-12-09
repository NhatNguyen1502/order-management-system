import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Product, CreateProductRequest } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private apiUrl = '/api/products';

  constructor(private http: HttpClient) { }

  getProducts(category?: string, page: number = 1, pageSize: number = 10): Observable<{ products: Product[], total: number }> {
    let url = `${this.apiUrl}?page=${page}&pageSize=${pageSize}`;
    if (category) {
      url += `&category=${category}`;
    }
    return this.http.get<{ products: Product[], total: number }>(url);
  }

  getProduct(id: string): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/${id}`);
  }

  createProduct(product: CreateProductRequest): Observable<{ productId: string }> {
    return this.http.post<{ productId: string }>(this.apiUrl, product);
  }

  updateProduct(id: string, product: Partial<Product>): Observable<{ success: boolean }> {
    return this.http.put<{ success: boolean }>(`${this.apiUrl}/${id}`, product);
  }

  deleteProduct(id: string): Observable<{ success: boolean }> {
    return this.http.delete<{ success: boolean }>(`${this.apiUrl}/${id}`);
  }
}
