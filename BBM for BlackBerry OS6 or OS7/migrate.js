// Creates a simple SQLite schema
const Database = require('better-sqlite3');
const db = new Database('./data.db');

db.exec(`
CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY,
  email TEXT UNIQUE,
  password TEXT
);

CREATE TABLE IF NOT EXISTS messages (
  id TEXT PRIMARY KEY,
  from_user TEXT,
  to_user TEXT,
  body TEXT,
  ts INTEGER,
  delivered INTEGER DEFAULT 0
);
`);

console.log('migrations applied');