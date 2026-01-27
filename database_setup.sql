-- 1. Create the Database
CREATE DATABASE IF NOT EXISTS library_db;
USE library_db;

-- 2. Create Books Table
CREATE TABLE IF NOT EXISTS books (
    book_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    author VARCHAR(100),
    quantity INT DEFAULT 0
);

-- 3. Create Members Table
CREATE TABLE IF NOT EXISTS members (
    member_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE
);

-- 4. Create Issue Records Table (The Master Log)
CREATE TABLE IF NOT EXISTS issue_records (
    issue_id INT AUTO_INCREMENT PRIMARY KEY,
    book_id INT,
    member_id INT,
    issue_date DATE NOT NULL,
    return_date DATE DEFAULT NULL,
    renewal_count INT DEFAULT 0,
    
    -- Status tracks: PENDING, RETURNED, LOST, DAMAGED, REPORTED_LOST
    status VARCHAR(20) DEFAULT 'PENDING', 
    
    fine_amount DECIMAL(10,2) DEFAULT 0.00,
    
    -- Payment status tracks: PENDING, PAID
    payment_status VARCHAR(20) DEFAULT 'PENDING', 
    
    lost_report_date DATE DEFAULT NULL,
    
    FOREIGN KEY (book_id) REFERENCES books(book_id),
    FOREIGN KEY (member_id) REFERENCES members(member_id)
);