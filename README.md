# Project 3 – SQL Client & Accountant Applications

**Course:** CNT 4714 Summer 2025  
**Author:** _Medha Subramaniyan_  
**Date:** July 6, 2025  

---

## Overview

This project delivers two Swing‐based Java GUIs for interacting with three MySQL schemas:

1. **ClientApp** – A general SQL client that can connect to any of:
   - **project3**  
   - **bikedb**  
   - **operationslog**  
   …using a pair of `.properties` files (DB URL + username/password).  
   It supports arbitrary DML/DDL (but only one statement at a time), shows results in a `JTable`, and logs every query/update by non‐accountant users into `operationscount`.

2. **AccountantApp** – A specialized read-only client:
   - Always connects to **operationslog**  
   - Always authenticates as `theaccountant`  
   - Only allows `SELECT`, `SHOW`, or `DESC` statements  
   - Displays results in a `JTable` (no logging)

Both applications share:

- `DBConnectionUtil.java` – loads a DB‐props file (`driver` + `url`) and a user‐props file (`user` + `password`), opens a JDBC `Connection`.  
- `ResultSetTableModel.java` – wraps any scrollable `ResultSet` into a Swing `TableModel`.

---
