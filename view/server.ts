import 'dotenv/config';

import express from 'express';
import { createServer as createViteServer } from 'vite';
import multer from 'multer';
import Database from 'better-sqlite3';
import { v4 as uuidv4 } from 'uuid';
import bcrypt from 'bcryptjs';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = 3000;

// Setup DB
const db = new Database('database.sqlite');
db.pragma('journal_mode = WAL');

db.exec(`
  CREATE TABLE IF NOT EXISTS users (
    userId TEXT PRIMARY KEY,
    email TEXT UNIQUE,
    password TEXT,
    nickname TEXT
  );
  CREATE TABLE IF NOT EXISTS sessions (
    token TEXT PRIMARY KEY,
    userId TEXT,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS posts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT,
    content TEXT,
    viewCount INTEGER DEFAULT 0,
    authorId TEXT,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS comments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    postId INTEGER,
    authorId TEXT,
    content TEXT,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS attachments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    postId INTEGER,
    fileName TEXT,
    s3Key TEXT
  );
  CREATE TABLE IF NOT EXISTS search_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    userId TEXT,
    keyword TEXT,
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP
  );
`);

app.use(express.json());

// Setup uploads
const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir);
}
const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadDir),
  filename: (req, file, cb) => cb(null, `${Date.now()}-${file.originalname}`),
});
const upload = multer({ storage });
app.use('/uploads', express.static(uploadDir));

// Auth Middleware
const authenticate = (req: any, res: any, next: any) => {
  const token = req.headers.authorization?.split(' ')[1];
  if (!token) return res.status(401).json({ success: false, message: 'Unauthorized' });
  const session = db.prepare('SELECT * FROM sessions WHERE token = ?').get(token) as any;
  if (!session) return res.status(401).json({ success: false, message: 'Unauthorized' });
  req.user = db.prepare('SELECT * FROM users WHERE userId = ?').get(session.userId);
  next();
};

const optionalAuthenticate = (req: any, res: any, next: any) => {
  const token = req.headers.authorization?.split(' ')[1];
  if (token) {
    const session = db.prepare('SELECT * FROM sessions WHERE token = ?').get(token) as any;
    if (session) {
      req.user = db.prepare('SELECT * FROM users WHERE userId = ?').get(session.userId);
    }
  }
  next();
};

// --- AUTH API ---
app.post('/auth/register', (req, res) => {
  const { userId, email, password, nickname } = req.body;
  try {
    const hash = bcrypt.hashSync(password, 10);
    db.prepare('INSERT INTO users (userId, email, password, nickname) VALUES (?, ?, ?, ?)').run(userId, email, hash, nickname);
    res.json({ success: true, message: 'Registered successfully', data: null });
  } catch (e: any) {
    res.status(400).json({ success: false, message: e.message });
  }
});

app.post('/auth/login', (req, res) => {
  const { userId, password } = req.body;
  const user = db.prepare('SELECT * FROM users WHERE userId = ?').get(userId) as any;
  if (!user || !bcrypt.compareSync(password, user.password)) {
    return res.status(401).json({ success: false, message: 'Invalid credentials' });
  }
  const token = uuidv4();
  db.prepare('INSERT INTO sessions (token, userId) VALUES (?, ?)').run(token, userId);
  res.json({ success: true, message: 'Logged in', data: token });
});

app.post('/auth/logout', authenticate, (req: any, res) => {
  const token = req.headers.authorization?.split(' ')[1];
  db.prepare('DELETE FROM sessions WHERE token = ?').run(token);
  res.json({ success: true, message: 'Logged out', data: null });
});

app.get('/auth/me', authenticate, (req: any, res) => {
  res.json({ success: true, message: 'Success', data: req.user.userId });
});

// --- POSTS API ---
app.get('/posts', optionalAuthenticate, (req: any, res) => {
  const page = parseInt(req.query.page as string) || 0;
  const size = parseInt(req.query.size as string) || 10;
  const offset = page * size;
  
  const totalElements = (db.prepare('SELECT COUNT(*) as count FROM posts').get() as any).count;
  const posts = db.prepare(`
    SELECT p.*, u.nickname as authorNickname 
    FROM posts p 
    JOIN users u ON p.authorId = u.userId 
    ORDER BY p.createdAt DESC 
    LIMIT ? OFFSET ?
  `).all(size, offset) as any[];

  res.json({
    success: true,
    message: 'Success',
    data: {
      totalPages: Math.ceil(totalElements / size),
      totalElements,
      pageable: { pageNumber: page, pageSize: size },
      content: posts
    }
  });
});

app.post('/posts', authenticate, upload.array('files'), (req: any, res) => {
  const requestData = JSON.parse(req.body.request || '{}');
  const { title, content } = requestData;
  const files = req.files as Express.Multer.File[];

  const info = db.prepare('INSERT INTO posts (title, content, authorId) VALUES (?, ?, ?)').run(title, content, req.user.userId);
  const postId = info.lastInsertRowid;

  if (files) {
    const insertAttachment = db.prepare('INSERT INTO attachments (postId, fileName, s3Key) VALUES (?, ?, ?)');
    for (const file of files) {
      insertAttachment.run(postId, file.originalname, file.filename);
    }
  }

  res.json({ success: true, message: 'Post created', data: postId.toString() });
});

app.get('/posts/search', optionalAuthenticate, (req: any, res) => {
  const keyword = req.query.keyword as string;
  const page = parseInt(req.query.page as string) || 0;
  const size = parseInt(req.query.size as string) || 10;
  const offset = page * size;

  if (keyword && keyword.trim().length >= 2 && req.user) {
    db.prepare('INSERT INTO search_history (userId, keyword) VALUES (?, ?)').run(req.user.userId, keyword.trim());
  }

  const query = `%trim(keyword)%`; // Actually we should use %keyword%
  const likeKeyword = `%${keyword}%`;
  
  const totalElements = (db.prepare('SELECT COUNT(*) as count FROM posts WHERE title LIKE ? OR content LIKE ?').get(likeKeyword, likeKeyword) as any).count;
  const posts = db.prepare(`
    SELECT p.*, u.nickname as authorNickname 
    FROM posts p 
    JOIN users u ON p.authorId = u.userId 
    WHERE p.title LIKE ? OR p.content LIKE ?
    ORDER BY p.createdAt DESC 
    LIMIT ? OFFSET ?
  `).all(likeKeyword, likeKeyword, size, offset) as any[];

  res.json({
    success: true,
    message: 'Success',
    data: {
      totalPages: Math.ceil(totalElements / size),
      totalElements,
      pageable: { pageNumber: page, pageSize: size },
      content: posts
    }
  });
});

app.get('/posts/search/history', authenticate, (req: any, res) => {
  const history = db.prepare(`
    SELECT DISTINCT keyword FROM search_history 
    WHERE userId = ? 
    ORDER BY createdAt DESC LIMIT 10
  `).all(req.user.userId) as any[];

  res.json({
    success: true,
    message: 'Success',
    data: { keywords: history.map(h => h.keyword) }
  });
});

app.get('/posts/:postId', optionalAuthenticate, (req: any, res) => {
  const postId = req.params.postId;
  db.prepare('UPDATE posts SET viewCount = viewCount + 1 WHERE id = ?').run(postId);
  
  const post = db.prepare(`
    SELECT p.*, u.nickname as authorNickname 
    FROM posts p JOIN users u ON p.authorId = u.userId WHERE p.id = ?
  `).get(postId) as any;

  if (!post) return res.status(404).json({ success: false, message: 'Not found' });

  const attachments = db.prepare('SELECT id, fileName, s3Key FROM attachments WHERE postId = ?').all(postId);
  const comments = db.prepare(`
    SELECT c.id, c.content, c.createdAt, c.authorId, u.nickname as authorNickname 
    FROM comments c JOIN users u ON c.authorId = u.userId WHERE c.postId = ?
  `).all(postId);

  res.json({
    success: true,
    message: 'Success',
    data: { ...post, attachments, comments }
  });
});

app.put('/posts/:postId', authenticate, upload.array('newFiles'), (req: any, res) => {
  const postId = req.params.postId;
  const post = db.prepare('SELECT * FROM posts WHERE id = ?').get(postId) as any;
  if (!post || post.authorId !== req.user.userId) return res.status(403).json({ success: false, message: 'Forbidden' });

  const requestData = JSON.parse(req.body.request || '{}');
  const { title, content, deletedAttachmentIds } = requestData;
  const newFiles = req.files as Express.Multer.File[];

  db.prepare('UPDATE posts SET title = ?, content = ? WHERE id = ?').run(title, content, postId);

  if (deletedAttachmentIds && deletedAttachmentIds.length > 0) {
    const placeholders = deletedAttachmentIds.map(() => '?').join(',');
    db.prepare(`DELETE FROM attachments WHERE id IN (${placeholders}) AND postId = ?`).run(...deletedAttachmentIds, postId);
  }

  if (newFiles) {
    const insertAttachment = db.prepare('INSERT INTO attachments (postId, fileName, s3Key) VALUES (?, ?, ?)');
    for (const file of newFiles) {
      insertAttachment.run(postId, file.originalname, file.filename);
    }
  }

  res.json({ success: true, message: 'Post updated', data: postId });
});

app.delete('/posts/:postId', authenticate, (req: any, res) => {
  const postId = req.params.postId;
  const post = db.prepare('SELECT * FROM posts WHERE id = ?').get(postId) as any;
  if (!post || post.authorId !== req.user.userId) return res.status(403).json({ success: false, message: 'Forbidden' });

  db.prepare('DELETE FROM posts WHERE id = ?').run(postId);
  db.prepare('DELETE FROM comments WHERE postId = ?').run(postId);
  db.prepare('DELETE FROM attachments WHERE postId = ?').run(postId);

  res.json({ success: true, message: 'Post deleted', data: null });
});

app.post('/posts/:postId/comments', authenticate, (req: any, res) => {
  const postId = req.params.postId;
  const { content } = req.body;
  db.prepare('INSERT INTO comments (postId, authorId, content) VALUES (?, ?, ?)').run(postId, req.user.userId, content);
  res.json({ success: true, message: 'Comment added', data: null });
});

app.delete('/posts/comments/:commentId', authenticate, (req: any, res) => {
  const commentId = req.params.commentId;
  const comment = db.prepare('SELECT * FROM comments WHERE id = ?').get(commentId) as any;
  if (!comment || comment.authorId !== req.user.userId) return res.status(403).json({ success: false, message: 'Forbidden' });

  db.prepare('DELETE FROM comments WHERE id = ?').run(commentId);
  res.json({ success: true, message: 'Comment deleted', data: null });
});

// Vite middleware for development
async function startServer() {
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    app.use(express.static(path.join(__dirname, 'dist')));
    app.get('*', (req, res) => {
      res.sendFile(path.join(__dirname, 'dist', 'index.html'));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Server running on http://localhost:${PORT}`);
  });
}

startServer();
