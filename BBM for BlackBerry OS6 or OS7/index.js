const express = require('express');
const bodyParser = require('body-parser');
const Database = require('better-sqlite3');
const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');

const SECRET = "replace-with-a-strong-secret";
const db = new Database('./data.db');

const app = express();
app.use(bodyParser.json());

// Helpers
function createToken(userId) {
  return jwt.sign({ sub: userId }, SECRET, { expiresIn: '30d' });
}
function authMiddleware(req, res, next) {
  const auth = req.headers.authorization;
  if (!auth) return res.status(401).send({ error: 'no auth' });
  const parts = auth.split(' ');
  if (parts.length !== 2) return res.status(401).send({ error: 'invalid auth' });
  try {
    const payload = jwt.verify(parts[1], SECRET);
    req.userId = payload.sub;
    next();
  } catch (e) {
    res.status(401).send({ error: 'invalid token' });
  }
}

// Signup / Login (very minimal)
app.post('/api/signup', (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) return res.status(400).send({ error: 'missing' });
  const exists = db.prepare('SELECT id FROM users WHERE email = ?').get(email);
  if (exists) return res.status(400).send({ error: 'user exists' });
  const id = uuidv4();
  db.prepare('INSERT INTO users(id,email,password) VALUES(?,?,?)').run(id, email, password);
  const token = createToken(id);
  res.send({ token, userId: id, email });
});

app.post('/api/login', (req, res) => {
  const { email, password } = req.body;
  const row = db.prepare('SELECT id,password FROM users WHERE email = ?').get(email);
  if (!row || row.password !== password) return res.status(401).send({ error: 'invalid' });
  const token = createToken(row.id);
  res.send({ token, userId: row.id, email });
});

// Send a message
app.post('/api/send', authMiddleware, (req, res) => {
  const { toUserId, body } = req.body;
  if (!toUserId || !body) return res.status(400).send({ error: 'missing' });
  const id = uuidv4();
  const ts = Date.now();
  db.prepare('INSERT INTO messages(id,from_user,to_user,body,ts,delivered) VALUES(?,?,?,?,?,0)')
    .run(id, req.userId, toUserId, body, ts);
  // In a real server you would notify connected clients via pub/sub / push
  res.send({ id, ts });
});

// Polling endpoint - long poll until messages after "since"
app.get('/api/poll', authMiddleware, (req, res) => {
  const since = parseInt(req.query.since || '0', 10);
  const userId = req.userId;

  const checkForMessages = () => {
    const rows = db.prepare('SELECT id,from_user,body,ts,delivered FROM messages WHERE to_user = ? AND ts > ? ORDER BY ts ASC')
      .all(userId, since);
    return rows;
  };

  let messages = checkForMessages();
  if (messages.length > 0) {
    // mark delivered
    const ids = messages.map(m => m.id);
    const stmt = db.prepare(`UPDATE messages SET delivered = 1 WHERE id = ?`);
    ids.forEach(id => stmt.run(id));
    return res.send({ messages });
  }

  // Block until messages or timeout (25s)
  const start = Date.now();
  const interval = setInterval(() => {
    messages = checkForMessages();
    if (messages.length > 0) {
      clearInterval(interval);
      const ids = messages.map(m => m.id);
      const stmt = db.prepare(`UPDATE messages SET delivered = 1 WHERE id = ?`);
      ids.forEach(id => stmt.run(id));
      return res.send({ messages });
    }
    if (Date.now() - start > 25000) {
      clearInterval(interval);
      return res.send({ messages: [] });
    }
  }, 1000);
});

// Simple message history
app.get('/api/messages', authMiddleware, (req, res) => {
  const other = req.query.with;
  if (!other) return res.status(400).send({ error: 'missing with' });
  const userId = req.userId;
  const rows = db.prepare('SELECT id,from_user,to_user,body,ts FROM messages WHERE (from_user = ? AND to_user = ?) OR (from_user = ? AND to_user = ?) ORDER BY ts ASC')
    .all(userId, other, other, userId);
  res.send({ messages: rows });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log('server listening on', PORT);
});