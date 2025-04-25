# 🛒 Orders Transaction Challenge

## 📝 Challenge Overview

In this challenge, you'll work with a CSV file named `Orders.csv`, which contains orders and their corresponding items separated by `,` and line breaks.

📌 **Goal:**  
Insert each order and its items (order details) into a database using a **single transaction**. If **any part of the process fails** (either inserting the order or any of its items), the **entire transaction must be rolled back** to maintain data integrity.

✅ **Key Points:**
- Each order includes **one or more** items (order details).
- All data is inserted **at once** using a **single transaction**.
- If something fails, the **transaction is canceled and rolled back**.

---

## 📂 Files and Directories
📄 Orders.csv -> CSV file containing the orders and their details. 
📁 storeFrontSQL -> Contains the SQL script to create the required database tables.

## 📚 Notes

- Make sure your script handles exceptions and properly rolls back the transaction if needed.
- This challenge is ideal for showcasing your understanding of:
  - File I/O (CSV parsing)
  - Transactions and error handling
  - Database design and scripting

---
## 🚀 Good luck, and happy coding!
