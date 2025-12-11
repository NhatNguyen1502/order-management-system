import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Inventory } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class InventoryService {
  private apiUrl = 'http://localhost:8080/api/inventory';

  constructor(private http: HttpClient) { }

  getInventory(productId: string): Observable<Inventory> {
    return this.http.get<Inventory>(`${this.apiUrl}/${productId}`);
  }

  updateInventory(productId: string, quantity: number, operation: string): Observable<{ success: boolean, newQuantity: number }> {
    return this.http.post<{ success: boolean, newQuantity: number }>(`${this.apiUrl}/update`, {
      productId,
      quantity,
      operation
    });
  }
}
