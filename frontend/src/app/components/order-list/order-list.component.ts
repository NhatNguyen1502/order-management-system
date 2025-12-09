import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/models';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Orders</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <p>Order management functionality - Create orders from products and view order history.</p>
        <p class="info-text">This is a skeleton implementation. In a full application, you would see:</p>
        <ul>
          <li>List of all orders with status</li>
          <li>Order details including items and customer info</li>
          <li>Ability to update order status</li>
          <li>Order creation from product catalog</li>
        </ul>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    mat-card {
      margin: 20px;
    }
    .info-text {
      margin-top: 20px;
      font-style: italic;
      color: #666;
    }
  `]
})
export class OrderListComponent implements OnInit {
  orders: Order[] = [];

  constructor(
    private orderService: OrderService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    // Placeholder for loading orders
  }
}
