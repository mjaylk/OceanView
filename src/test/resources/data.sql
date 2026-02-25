-- 1. users first (no dependencies)
INSERT INTO `users` (`user_id`, `username`, `password_hash`, `role`, `status`) 
VALUES (1, 'admin', 'hashedpassword123', 'ADMIN', 'ACTIVE');

-- 2. rooms second (no dependencies)
INSERT INTO `rooms` (`room_id`, `room_number`, `room_type`, `rate_per_night`, `status`, `max_guests`, `description`) 
VALUES (1, '101', 'STANDARD', 150.00, 'AVAILABLE', 2, 'Test room');

-- 3. guests third (depends on users → user_id=1 must exist)
INSERT INTO `guests` (`guest_id`, `full_name`, `address`, `contact_number`, `email`, `user_id`) 
VALUES (1, 'Test Guest', '123 Test St', '0771234567', 'test@test.com', 1);

-- 4. reservations last (depends on users, rooms, guests)
INSERT INTO `reservations` (`reservation_number`, `guest_id`, `room_id`, `check_in_date`, `check_out_date`, `status`, `notes`, `created_by`, `nights`, `rate_per_night`, `subtotal`, `discount`, `tax`, `total_amount`, `amount_paid`, `payment_status`) 
VALUES ('RES-TEST-001', 1, 1, '2026-03-01', '2026-03-03', 'CONFIRMED', 'Test reservation', 1, 2, 150.00, 300.00, 0.00, 0.00, 300.00, 0.00, 'UNPAID');
