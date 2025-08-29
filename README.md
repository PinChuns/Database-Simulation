## Database Management System in Java

---

### Project Overview
This project is a simplified relational database server implemented in Java. It processes a subset of SQL-like commands and supports persistent storage, querying, updating, and deleting records. The work emphasizes persistence, command parsing, error handling, robustness, and code quality.

---

### Features
- Supported commands: `USE`, `CREATE`, `INSERT`, `SELECT`, `UPDATE`, `ALTER`, `DELETE`, `DROP`, `JOIN`.
- Persistent storage via tab-separated files under `databases/` (server automatically maintains files; database and table names are case-insensitive).
- Auto-generated primary key `id` (unique, never recycled).
- Foreign-key relationships by convention (no composite keys supported).
- Response format: each command begins with `[OK]` or `[ERROR]`; the server must return results rather than only printing to console.
- `JOIN` output: a new `id` column is generated for the result table; source table `id`s and join keys are not retained; attributes are named `Table.Attribute`.

---
### Requirements
- Java 17
- Maven (wrapper included, run with ./mvnw)

---

### Installation & Usage
#### 1. Clone the repository
```bash
git clone https://github.com/your-username/dbserver-assignment.git
cd dbserver-assignment/cw-db
```
#### 2. Compile and run the server
```bash
./mvnw exec:java@server
```
The server listens on localhost:8888

#### 3. Run the client for testing
```bash
./mvnw exec:java@client
```

---
### Examples
Some example queries: 
```sql
CREATE DATABASE markbook;
USE markbook;
CREATE TABLE marks (name, mark, pass);
INSERT INTO marks VALUES ('Simon', 65, TRUE);
SELECT * FROM marks WHERE pass == TRUE;
```

---
### Testing
Run built-in tests:
```bash
./mvnw test
```
Include and extend the provided template tests

---

### Technical Details
- Java + OOP; custom in-memory structures (e.g., `Map`, `List`) to represent database entities.
- File I/O with platform-independent separators (`File.separator`).
- Robust error handling: the server must **not** crash on malformed input.
- The main entry point is `DBServer`, which must include a `public DBServer()` constructor and a `public String handleCommand(String)` method (as required by the assignment specification).

---

### Project Structure

    cw-db
    ├── databases/          # Persistent storage (tab-separated files)
    ├── src/
    │   ├── main/
    │   │   └── java/edu/uob/
    │   │       ├── DBServer.java   # Main database server
    │   │       └── DBClient.java   # Provided command-line client
    │   └── test/                   # Unit tests (JUnit)
    ├── pom.xml             # Maven project configuration
    ├── mvnw, mvnw.cmd      # Maven wrapper scripts
    └── README.md

---

### Limitations & Roadmap
- No type system; comparisons are best-effort. The `LIKE` operator is case-sensitive substring matching, not full SQL wildcard semantics.
- Only inner join; no composite keys.
- Planned: richer parser, additional indexes, more comprehensive test coverage.

---

### Academic Integrity & Disclaimer
This project was developed as part of a university coursework assignment.  
It is shared publicly for learning and portfolio purposes only.  
Do **not** reuse this code for academic submissions.  

