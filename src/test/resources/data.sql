INSERT INTO `users` (`username`, `password_hash`, `role`, `status`) 
VALUES ('admin', 'hashedpassword123', 'ADMIN', 'ACTIVE');

INSERT INTO `rooms` (`room_number`, `room_type`, `rate_per_night`, `status`, `max_guests`, `description`) 
VALUES ('101', 'STANDARD', 150.00, 'AVAILABLE', 2, 'Test room');

INSERT INTO `guests` (`full_name`, `address`, `contact_number`, `email`, `user_id`) 
VALUES ('Test Guest', '123 Test St', '0771234567', 'test@test.com', 1);

INSERT INTO `reservations` (`reservation_number`, `guest_id`, `room_id`, `check_in_date`, `check_out_date`, `status`, `notes`, `created_by`, `nights`, `rate_per_night`, `subtotal`, `discount`, `tax`, `total_amount`, `amount_paid`, `payment_status`) 
VALUES ('RES-TEST-001', 1, 1, '2026-03-01', '2026-03-03', 'CONFIRMED', 'Test reservation', 1, 2, 150.00, 300.00, 0.00, 0.00, 300.00, 0.00, 'UNPAID');

