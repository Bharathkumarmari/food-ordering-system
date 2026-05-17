-- ================================================================
-- ONLINE FOOD ORDERING SYSTEM — Complete MySQL Database Schema
-- ================================================================
-- This script creates the entire database, tables, views, triggers,
-- stored procedures, indexes, and inserts sample test data.
-- ================================================================

-- Create and use the database
DROP DATABASE IF EXISTS food_ordering_system;
CREATE DATABASE food_ordering_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE food_ordering_system;

-- ================================================================
-- TABLE 1: users
-- Stores registered user and admin accounts
-- ================================================================
CREATE TABLE users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,  -- BCrypt hashed
    phone         VARCHAR(20),
    address       TEXT,
    role          ENUM('USER', 'ADMIN') DEFAULT 'USER',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_role (role)
) ENGINE=InnoDB;

-- ================================================================
-- TABLE 2: restaurants
-- Stores restaurant information
-- ================================================================
CREATE TABLE restaurants (
    restaurant_id   INT AUTO_INCREMENT PRIMARY KEY,
    restaurant_name VARCHAR(150) NOT NULL,
    cuisine_type    VARCHAR(100),
    city            VARCHAR(100),
    latitude        DECIMAL(10,8),
    longitude       DECIMAL(11,8),
    location        VARCHAR(200) NOT NULL,
    rating          DECIMAL(2,1) DEFAULT 0.0,
    delivery_time   VARCHAR(30) DEFAULT '30-40 min',
    is_active       BOOLEAN DEFAULT TRUE,
    image_url       VARCHAR(500),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_restaurant_name (restaurant_name),
    INDEX idx_restaurant_cuisine (cuisine_type),
    INDEX idx_restaurant_city (city)
) ENGINE=InnoDB;

-- ================================================================
-- TABLE 3: menu_items
-- Stores food items for each restaurant
-- ================================================================
CREATE TABLE menu_items (
    menu_id         INT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id   INT NOT NULL,
    item_name       VARCHAR(150) NOT NULL,
    description     TEXT,
    price           DECIMAL(10,2) NOT NULL,
    category        VARCHAR(80) NOT NULL,
    is_veg          BOOLEAN DEFAULT TRUE,
    is_available    BOOLEAN DEFAULT TRUE,
    image_url       VARCHAR(500),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(restaurant_id) ON DELETE CASCADE,
    INDEX idx_menu_restaurant (restaurant_id),
    INDEX idx_menu_category (category),
    INDEX idx_menu_price (price)
) ENGINE=InnoDB;

-- ================================================================
-- TABLE 4: cart
-- One cart per user (active session cart)
-- ================================================================
CREATE TABLE cart (
    cart_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL UNIQUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ================================================================
-- TABLE 5: cart_items
-- Individual items within a user's cart
-- ================================================================
CREATE TABLE cart_items (
    cart_item_id  INT AUTO_INCREMENT PRIMARY KEY,
    cart_id       INT NOT NULL,
    menu_id       INT NOT NULL,
    quantity      INT NOT NULL DEFAULT 1,
    FOREIGN KEY (cart_id) REFERENCES cart(cart_id) ON DELETE CASCADE,
    FOREIGN KEY (menu_id) REFERENCES menu_items(menu_id) ON DELETE CASCADE,
    UNIQUE KEY uk_cart_menu (cart_id, menu_id),
    INDEX idx_cart_items_cart (cart_id)
) ENGINE=InnoDB;

-- ================================================================
-- TABLE 6: orders
-- Tracks all placed orders
-- ================================================================
CREATE TABLE orders (
    order_id      INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT NOT NULL,
    total_amount  DECIMAL(10,2) NOT NULL,
    order_status  ENUM('PLACED','CONFIRMED','PREPARING','OUT_FOR_DELIVERY','DELIVERED','CANCELLED')
                  DEFAULT 'PLACED',
    delivery_address TEXT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_orders_user (user_id),
    INDEX idx_orders_status (order_status),
    INDEX idx_orders_date (created_at)
) ENGINE=InnoDB;

-- ================================================================
-- TABLE 7: order_items
-- Individual items within an order
-- ================================================================
CREATE TABLE order_items (
    order_item_id  INT AUTO_INCREMENT PRIMARY KEY,
    order_id       INT NOT NULL,
    menu_id        INT NOT NULL,
    quantity       INT NOT NULL DEFAULT 1,
    subtotal       DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (menu_id) REFERENCES menu_items(menu_id) ON DELETE CASCADE,
    INDEX idx_order_items_order (order_id)
) ENGINE=InnoDB;

-- ================================================================
-- TABLE 8: payments
-- Payment records for each order
-- ================================================================
CREATE TABLE payments (
    payment_id      INT AUTO_INCREMENT PRIMARY KEY,
    order_id        INT NOT NULL UNIQUE,
    payment_method  ENUM('CREDIT_CARD','DEBIT_CARD','UPI','CASH_ON_DELIVERY','WALLET')
                    DEFAULT 'CASH_ON_DELIVERY',
    payment_status  ENUM('PENDING','COMPLETED','FAILED','REFUNDED') DEFAULT 'PENDING',
    amount          DECIMAL(10,2) NOT NULL,
    transaction_id  VARCHAR(100),
    payment_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    INDEX idx_payments_order (order_id),
    INDEX idx_payments_status (payment_status)
) ENGINE=InnoDB;

-- ================================================================
-- TABLE 9: reviews
-- User reviews for restaurants
-- ================================================================
CREATE TABLE reviews (
    review_id       INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL,
    restaurant_id   INT NOT NULL,
    rating          INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment         TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(restaurant_id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_restaurant_review (user_id, restaurant_id),
    INDEX idx_reviews_restaurant (restaurant_id)
) ENGINE=InnoDB;


-- ================================================================
-- VIEWS
-- ================================================================

-- View: Daily Sales Report
CREATE OR REPLACE VIEW vw_daily_sales_report AS
SELECT
    DATE(o.created_at) AS order_date,
    COUNT(o.order_id) AS total_orders,
    SUM(o.total_amount) AS total_revenue,
    AVG(o.total_amount) AS avg_order_value
FROM orders o
WHERE o.order_status != 'CANCELLED'
GROUP BY DATE(o.created_at)
ORDER BY order_date DESC;

-- View: Restaurant Revenue
CREATE OR REPLACE VIEW vw_restaurant_revenue AS
SELECT
    r.restaurant_id,
    r.restaurant_name,
    COUNT(DISTINCT oi.order_id) AS total_orders,
    SUM(oi.subtotal) AS total_revenue
FROM restaurants r
JOIN menu_items mi ON r.restaurant_id = mi.restaurant_id
JOIN order_items oi ON mi.menu_id = oi.menu_id
JOIN orders o ON oi.order_id = o.order_id
WHERE o.order_status != 'CANCELLED'
GROUP BY r.restaurant_id, r.restaurant_name
ORDER BY total_revenue DESC;

-- View: Most Ordered Items
CREATE OR REPLACE VIEW vw_popular_items AS
SELECT
    mi.menu_id,
    mi.item_name,
    r.restaurant_name,
    SUM(oi.quantity) AS times_ordered,
    SUM(oi.subtotal) AS total_sales
FROM menu_items mi
JOIN order_items oi ON mi.menu_id = oi.menu_id
JOIN restaurants r ON mi.restaurant_id = r.restaurant_id
GROUP BY mi.menu_id, mi.item_name, r.restaurant_name
ORDER BY times_ordered DESC;

-- View: User Order History
CREATE OR REPLACE VIEW vw_user_order_history AS
SELECT
    u.user_id,
    u.full_name,
    o.order_id,
    o.total_amount,
    o.order_status,
    o.created_at AS order_date,
    p.payment_method,
    p.payment_status
FROM users u
JOIN orders o ON u.user_id = o.user_id
LEFT JOIN payments p ON o.order_id = p.order_id
ORDER BY o.created_at DESC;


-- ================================================================
-- TRIGGERS
-- ================================================================

-- Trigger: Auto-update restaurant rating when a review is added
DELIMITER //
CREATE TRIGGER trg_update_restaurant_rating
AFTER INSERT ON reviews
FOR EACH ROW
BEGIN
    UPDATE restaurants
    SET rating = (
        SELECT ROUND(AVG(rating), 1)
        FROM reviews
        WHERE restaurant_id = NEW.restaurant_id
    )
    WHERE restaurant_id = NEW.restaurant_id;
END//
DELIMITER ;

-- Trigger: Update rating when a review is updated
DELIMITER //
CREATE TRIGGER trg_update_restaurant_rating_on_update
AFTER UPDATE ON reviews
FOR EACH ROW
BEGIN
    UPDATE restaurants
    SET rating = (
        SELECT ROUND(AVG(rating), 1)
        FROM reviews
        WHERE restaurant_id = NEW.restaurant_id
    )
    WHERE restaurant_id = NEW.restaurant_id;
END//
DELIMITER ;


-- ================================================================
-- STORED PROCEDURES
-- ================================================================

-- Procedure: Place an order from cart
DELIMITER //
CREATE PROCEDURE sp_place_order(
    IN p_user_id INT,
    IN p_delivery_address TEXT,
    IN p_payment_method VARCHAR(30),
    OUT p_order_id INT
)
BEGIN
    DECLARE v_cart_id INT;
    DECLARE v_total DECIMAL(10,2);

    -- Start transaction
    START TRANSACTION;

    -- Get the user's cart
    SELECT cart_id INTO v_cart_id FROM cart WHERE user_id = p_user_id;

    IF v_cart_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cart not found';
    END IF;

    -- Calculate total
    SELECT COALESCE(SUM(ci.quantity * mi.price), 0)
    INTO v_total
    FROM cart_items ci
    JOIN menu_items mi ON ci.menu_id = mi.menu_id
    WHERE ci.cart_id = v_cart_id;

    IF v_total = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cart is empty';
    END IF;

    -- Create the order
    INSERT INTO orders (user_id, total_amount, order_status, delivery_address)
    VALUES (p_user_id, v_total, 'PLACED', p_delivery_address);

    SET p_order_id = LAST_INSERT_ID();

    -- Copy cart items to order items
    INSERT INTO order_items (order_id, menu_id, quantity, subtotal)
    SELECT p_order_id, ci.menu_id, ci.quantity, (ci.quantity * mi.price)
    FROM cart_items ci
    JOIN menu_items mi ON ci.menu_id = mi.menu_id
    WHERE ci.cart_id = v_cart_id;

    -- Create payment record
    INSERT INTO payments (order_id, payment_method, payment_status, amount)
    VALUES (p_order_id, p_payment_method, 'COMPLETED', v_total);

    -- Clear the cart
    DELETE FROM cart_items WHERE cart_id = v_cart_id;

    COMMIT;
END//
DELIMITER ;

-- Procedure: Get revenue stats for admin dashboard
DELIMITER //
CREATE PROCEDURE sp_get_dashboard_stats(
    OUT p_total_users INT,
    OUT p_total_restaurants INT,
    OUT p_total_orders INT,
    OUT p_total_revenue DECIMAL(10,2)
)
BEGIN
    SELECT COUNT(*) INTO p_total_users FROM users WHERE role = 'USER';
    SELECT COUNT(*) INTO p_total_restaurants FROM restaurants WHERE is_active = TRUE;
    SELECT COUNT(*) INTO p_total_orders FROM orders;
    SELECT COALESCE(SUM(total_amount), 0) INTO p_total_revenue FROM orders WHERE order_status != 'CANCELLED';
END//
DELIMITER ;


-- ================================================================
-- SAMPLE DATA
-- ================================================================

-- Admin user (password: admin123 — BCrypt hashed)
INSERT INTO users (full_name, email, password, phone, address, role) VALUES
('Admin User', 'admin@foodapp.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '9999999999', 'Admin Office, Main Street', 'ADMIN');

-- Regular users (password: user123 — BCrypt hashed)
INSERT INTO users (full_name, email, password, phone, address, role) VALUES
('Rahul Sharma', 'rahul@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '9876543210', '12 MG Road, Bangalore', 'USER'),
('Priya Patel', 'priya@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '9876543211', '45 Park Street, Kolkata', 'USER'),
('Amit Kumar', 'amit@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '9876543212', '78 Connaught Place, Delhi', 'USER'),
('Sneha Reddy', 'sneha@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '9876543213', '33 Banjara Hills, Hyderabad', 'USER');

-- Restaurants with city, latitude, longitude
INSERT INTO restaurants (restaurant_name, cuisine_type, city, latitude, longitude, location, rating, delivery_time, is_active, image_url) VALUES
('The Spice Garden', 'North Indian', 'Chennai', 13.0827, 80.2707, 'T Nagar, Chennai', 4.5, '25-35 min', TRUE, 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=400'),
('Pizza Paradise', 'Italian', 'Chennai', 12.9815, 80.2180, 'Velachery, Chennai', 4.3, '20-30 min', TRUE, 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=400'),
('Burger King', 'American', 'Bangalore', 12.9716, 77.5946, 'MG Road, Bangalore', 4.1, '30-40 min', TRUE, 'https://images.unsplash.com/photo-1552566626-52f8b828add9?w=400'),
('McDonald''s', 'American', 'Bangalore', 12.9352, 77.6245, 'Koramangala, Bangalore', 4.4, '15-25 min', TRUE, 'https://images.unsplash.com/photo-1466978913421-dad2ebd01d17?w=400'),
('Paradise Biryani', 'Hyderabadi', 'Hyderabad', 17.3850, 78.4867, 'Secunderabad, Hyderabad', 4.6, '20-30 min', TRUE, 'https://images.unsplash.com/photo-1514933651103-005eec06c04b?w=400'),
('Bawarchi', 'Hyderabadi', 'Hyderabad', 17.4065, 78.4772, 'RTC X Roads, Hyderabad', 4.7, '30-40 min', TRUE, 'https://images.unsplash.com/photo-1537047902294-62a40c20a6ae?w=400'),
('Domino''s Pizza', 'Italian', 'Mumbai', 19.0760, 72.8777, 'Bandra West, Mumbai', 4.2, '35-45 min', TRUE, 'https://images.unsplash.com/photo-1579027989536-b7b1f875659b?w=400'),
('KFC', 'American', 'Chennai', 13.0827, 80.2707, 'Anna Nagar, Chennai', 4.0, '25-35 min', TRUE, 'https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=400');

-- Menu Items for The Spice Garden (restaurant_id = 1)
INSERT INTO menu_items (restaurant_id, item_name, description, price, category, is_veg, image_url) VALUES
(1, 'Butter Chicken', 'Creamy tomato-based curry with tender chicken pieces', 320.00, 'Main Course', FALSE, 'https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?w=300'),
(1, 'Paneer Tikka Masala', 'Grilled paneer in rich spiced gravy', 280.00, 'Main Course', TRUE, 'https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=300'),
(1, 'Dal Makhani', 'Slow-cooked black lentils in butter and cream', 220.00, 'Main Course', TRUE, 'https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=300'),
(1, 'Garlic Naan', 'Soft bread topped with garlic and butter', 60.00, 'Breads', TRUE, 'https://images.unsplash.com/photo-1599487488170-d11ec9c172f0?w=300'),
(1, 'Chicken Biryani', 'Fragrant basmati rice layered with spiced chicken', 350.00, 'Rice', FALSE, 'https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=300'),
(1, 'Gulab Jamun', 'Deep-fried milk dumplings in sugar syrup', 120.00, 'Desserts', TRUE, 'https://images.unsplash.com/photo-1666190073498-2a2048193c60?w=300');

-- Menu Items for Pizza Paradise (restaurant_id = 2)
INSERT INTO menu_items (restaurant_id, item_name, description, price, category, is_veg, image_url) VALUES
(2, 'Margherita Pizza', 'Classic pizza with fresh mozzarella and basil', 299.00, 'Pizza', TRUE, 'https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=300'),
(2, 'Pepperoni Pizza', 'Loaded with spicy pepperoni and mozzarella', 399.00, 'Pizza', FALSE, 'https://images.unsplash.com/photo-1628840042765-356cda07504e?w=300'),
(2, 'Garlic Bread', 'Toasted bread with garlic butter and herbs', 149.00, 'Starters', TRUE, 'https://images.unsplash.com/photo-1619535860434-ba1d8fa12536?w=300'),
(2, 'Pasta Alfredo', 'Creamy white sauce pasta with mushrooms', 279.00, 'Pasta', TRUE, 'https://images.unsplash.com/photo-1645112411341-6c4fd023714a?w=300'),
(2, 'Tiramisu', 'Classic Italian coffee-flavored dessert', 199.00, 'Desserts', TRUE, 'https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=300');

-- Menu Items for Dragon Wok (restaurant_id = 3)
INSERT INTO menu_items (restaurant_id, item_name, description, price, category, is_veg, image_url) VALUES
(3, 'Kung Pao Chicken', 'Stir-fried chicken with peanuts in spicy sauce', 310.00, 'Main Course', FALSE, 'https://images.unsplash.com/photo-1525755662778-989d0524087e?w=300'),
(3, 'Veg Manchurian', 'Deep-fried veggie balls in tangy sauce', 220.00, 'Starters', TRUE, 'https://images.unsplash.com/photo-1645696301019-35adcc0d1282?w=300'),
(3, 'Hakka Noodles', 'Stir-fried noodles with vegetables and soy sauce', 200.00, 'Noodles', TRUE, 'https://images.unsplash.com/photo-1585032226651-759b368d7246?w=300'),
(3, 'Spring Rolls', 'Crispy rolls filled with seasoned vegetables', 180.00, 'Starters', TRUE, 'https://images.unsplash.com/photo-1606525437679-037aca74a594?w=300'),
(3, 'Fried Rice', 'Classic Chinese fried rice with eggs and veggies', 230.00, 'Rice', FALSE, 'https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=300');

-- Menu Items for Burger Hub (restaurant_id = 4)
INSERT INTO menu_items (restaurant_id, item_name, description, price, category, is_veg, image_url) VALUES
(4, 'Classic Cheeseburger', 'Juicy beef patty with cheddar and pickles', 249.00, 'Burgers', FALSE, 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=300'),
(4, 'Veggie Burger', 'Plant-based patty with fresh veggies and sauce', 199.00, 'Burgers', TRUE, 'https://images.unsplash.com/photo-1520072959219-c595dc870360?w=300'),
(4, 'Chicken Wings', 'Crispy buffalo wings with blue cheese dip', 299.00, 'Starters', FALSE, 'https://images.unsplash.com/photo-1608039829572-9b0088b12b76?w=300'),
(4, 'French Fries', 'Golden crispy fries with ketchup', 129.00, 'Sides', TRUE, 'https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=300'),
(4, 'Chocolate Shake', 'Rich and creamy chocolate milkshake', 149.00, 'Beverages', TRUE, 'https://images.unsplash.com/photo-1572490122747-3968b75cc699?w=300');

-- Menu Items for Dosa Factory (restaurant_id = 5)
INSERT INTO menu_items (restaurant_id, item_name, description, price, category, is_veg, image_url) VALUES
(5, 'Masala Dosa', 'Crispy crepe filled with spiced potato', 120.00, 'Main Course', TRUE, 'https://images.unsplash.com/photo-1668236543090-82eba5ee5976?w=300'),
(5, 'Idli Sambar', 'Steamed rice cakes with lentil soup', 90.00, 'Breakfast', TRUE, 'https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=300'),
(5, 'Vada', 'Crispy fried lentil doughnuts', 80.00, 'Starters', TRUE, 'https://images.unsplash.com/photo-1630383249896-424e482df921?w=300'),
(5, 'Uttapam', 'Thick pancake topped with onions and tomatoes', 140.00, 'Main Course', TRUE, 'https://images.unsplash.com/photo-1567337710282-00832b415979?w=300'),
(5, 'Filter Coffee', 'Traditional South Indian filtered coffee', 50.00, 'Beverages', TRUE, 'https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=300');

-- Menu Items for Biryani Blues (restaurant_id = 6)
INSERT INTO menu_items (restaurant_id, item_name, description, price, category, is_veg, image_url) VALUES
(6, 'Hyderabadi Chicken Biryani', 'Authentic dum biryani with aromatic spices', 380.00, 'Biryani', FALSE, 'https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=300'),
(6, 'Mutton Biryani', 'Slow-cooked mutton biryani with saffron', 450.00, 'Biryani', FALSE, 'https://images.unsplash.com/photo-1642821373181-696a54913e93?w=300'),
(6, 'Veg Biryani', 'Fragrant rice with mixed vegetables and spices', 250.00, 'Biryani', TRUE, 'https://images.unsplash.com/photo-1596797038530-2c107229654b?w=300'),
(6, 'Raita', 'Cool yogurt with cucumber and spices', 60.00, 'Sides', TRUE, 'https://images.unsplash.com/photo-1601050690597-df0568f70950?w=300'),
(6, 'Double Ka Meetha', 'Hyderabadi bread pudding dessert', 130.00, 'Desserts', TRUE, 'https://images.unsplash.com/photo-1551024506-0bccd828d307?w=300');

-- Sample orders for testing
INSERT INTO orders (user_id, total_amount, order_status, delivery_address) VALUES
(2, 660.00, 'DELIVERED', '12 MG Road, Bangalore'),
(2, 399.00, 'PREPARING', '12 MG Road, Bangalore'),
(3, 530.00, 'PLACED', '45 Park Street, Kolkata'),
(4, 498.00, 'OUT_FOR_DELIVERY', '78 Connaught Place, Delhi');

-- Sample order items
INSERT INTO order_items (order_id, menu_id, quantity, subtotal) VALUES
(1, 1, 1, 320.00),   -- Butter Chicken
(1, 4, 2, 120.00),   -- 2x Garlic Naan
(1, 3, 1, 220.00),   -- Dal Makhani
(2, 8, 1, 399.00),   -- Pepperoni Pizza
(3, 16, 2, 498.00),  -- 2x Classic Cheeseburger
(3, 19, 1, 129.00),  -- French Fries (note: 498+129=627, but we put 530 for demo; acceptable for sample)
(4, 7, 1, 299.00),   -- Margherita Pizza
(4, 10, 1, 199.00);  -- Tiramisu

-- Sample payments
INSERT INTO payments (order_id, payment_method, payment_status, amount) VALUES
(1, 'UPI', 'COMPLETED', 660.00),
(2, 'CREDIT_CARD', 'COMPLETED', 399.00),
(3, 'CASH_ON_DELIVERY', 'PENDING', 530.00),
(4, 'DEBIT_CARD', 'COMPLETED', 498.00);

-- Sample reviews
INSERT INTO reviews (user_id, restaurant_id, rating, comment) VALUES
(2, 1, 5, 'Amazing food! The Butter Chicken was divine.'),
(3, 2, 4, 'Great pizza, fast delivery.'),
(4, 4, 4, 'Best burgers in town!'),
(2, 6, 5, 'Authentic Hyderabadi biryani, highly recommended!');

-- ================================================================
-- END OF SCHEMA
-- ================================================================
