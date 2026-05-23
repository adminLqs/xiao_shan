-- 商品发布功能调试SQL脚本

-- 1. 检查product_skus表是否存在
SHOW TABLES LIKE 'product_skus';

-- 2. 查看表结构
DESC product_skus;

-- 3. 查看products表结构
DESC products;

-- 4. 查询所有商品及其ID（用于调试）
SELECT id, name, created_at FROM products ORDER BY created_at DESC LIMIT 10;

-- 5. 查询指定商品的SKU数据（替换productId）
-- SELECT * FROM product_skus WHERE product_id = 1;

-- 6. 插入测试SKU数据（如果表存在但为空）
-- INSERT INTO product_skus (product_id, sku_name, spec_info, price, original_price, stock, sort_order)
-- VALUES (1, '默认-单品', '{"默认":"单品"}', 99.00, 199.00, 100, 0);
