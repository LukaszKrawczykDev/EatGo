SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 0) + 1, false);
SELECT setval('restaurants_id_seq', COALESCE((SELECT MAX(id) FROM restaurants), 0) + 1, false);
SELECT setval('dishes_id_seq', COALESCE((SELECT MAX(id) FROM dishes), 0) + 1, false);
SELECT setval('addresses_id_seq', COALESCE((SELECT MAX(id) FROM addresses), 0) + 1, false);
SELECT setval('orders_id_seq', COALESCE((SELECT MAX(id) FROM orders), 0) + 1, false);
SELECT setval('order_items_id_seq', COALESCE((SELECT MAX(id) FROM order_items), 0) + 1, false);
SELECT setval('reviews_id_seq', COALESCE((SELECT MAX(id) FROM reviews), 0) + 1, false);

