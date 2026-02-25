// view/src/pages/PostList.tsx
import React, { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import axios from 'axios';
import { format } from 'date-fns';
import { Search, Eye, Clock, Edit3 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function PostList() {
  const [posts, setPosts] = useState<any[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [searchParams, setSearchParams] = useSearchParams();
  const [keyword, setKeyword] = useState(searchParams.get('keyword') || '');
  const [history, setHistory] = useState<string[]>([]);
  // 토큰 유무뿐만 아니라 userPk(로그인 식별자)도 가져옵니다.
  const { token, userPk } = useAuth(); 
  
  const page = parseInt(searchParams.get('page') || '0');

  useEffect(() => {
    const fetchPosts = async () => {
      const currentKeyword = searchParams.get('keyword');
      const url = currentKeyword 
        ? `/posts/search?keyword=${encodeURIComponent(currentKeyword)}&page=${page}&size=10`
        : `/posts?page=${page}&size=10`;
      
      const config = token ? { headers: { Authorization: `Bearer ${token}` } } : {};
      
      try {
        const res = await axios.get(url, config);
        if (res.data.success) {
          setPosts(res.data.data.content);
          setTotalPages(res.data.data.totalPages);
        }
      } catch (err) {
        console.error(err);
      }
    };

    fetchPosts();
  }, [searchParams, token]);

  useEffect(() => {
    if (token) {
      axios.get('/posts/search/history', { headers: { Authorization: `Bearer ${token}` } })
        .then(res => {
          if (res.data.success) {
            setHistory(res.data.data.keywords);
          }
        })
        .catch(() => {});
    }
  }, [token, searchParams]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (keyword.trim()) {
      setSearchParams({ keyword: keyword.trim(), page: '0' });
    } else {
      setSearchParams({ page: '0' });
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <h1 className="text-2xl font-bold text-gray-900 shrink-0">커뮤니티 게시판</h1>
        
        <div className="flex items-center gap-3 w-full md:w-auto">
          <div className="relative w-full md:w-80">
            <form onSubmit={handleSearch} className="relative">
              <input
                type="text"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                placeholder="게시글 검색..."
                className="w-full pl-10 pr-4 py-2 bg-white border border-gray-300 rounded-full focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all shadow-sm"
              />
              <Search className="absolute left-3 top-2.5 w-5 h-5 text-gray-400" />
            </form>
            {history.length > 0 && (
              <div className="absolute top-full mt-2 w-full bg-white border border-gray-100 rounded-xl shadow-lg z-20 p-2">
                <div className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2 px-2">최근 검색어</div>
                <div className="flex flex-wrap gap-2">
                  {history.map((h, i) => (
                    <button
                      key={i}
                      onClick={() => {
                        setKeyword(h);
                        setSearchParams({ keyword: h, page: '0' });
                      }}
                      className="px-3 py-1 bg-gray-100 hover:bg-gray-200 text-sm rounded-full text-gray-700 transition-colors"
                    >
                      {h}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* ✅ 글쓰기 버튼 (로그인한 사용자에게만 보임) */}
          {(token || userPk) && (
            <Link
              to="/posts/create"
              className="shrink-0 flex items-center gap-1.5 bg-indigo-600 text-white px-4 py-2 rounded-full hover:bg-indigo-700 transition-colors font-medium shadow-sm"
            >
              <Edit3 className="w-4 h-4" />
              <span>글쓰기</span>
            </Link>
          )}
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="divide-y divide-gray-200">
          {posts.length === 0 ? (
            <div className="p-8 text-center text-gray-500">게시글이 없습니다.</div>
          ) : (
            posts.map(post => (
              <Link key={post.id} to={`/posts/${post.id}`} className="block hover:bg-gray-50 transition-colors p-4 sm:p-6">
                <div className="flex justify-between items-start gap-4">
                  <div className="flex-1 min-w-0">
                    <h2 className="text-lg font-semibold text-gray-900 truncate mb-1">{post.title}</h2>
                    <p className="text-sm text-gray-500 line-clamp-2 mb-3">{post.content}</p>
                    <div className="flex items-center gap-4 text-xs text-gray-500">
                      <span className="font-medium text-gray-700">{post.authorNickname}</span>
                      <span className="flex items-center gap-1"><Clock className="w-3.5 h-3.5" /> {format(new Date(post.createdAt), 'yyyy. MM. dd.')}</span>
                    </div>
                  </div>
                  <div className="flex flex-col items-end gap-2 text-xs text-gray-500 shrink-0">
                    <span className="flex items-center gap-1"><Eye className="w-4 h-4" /> {post.viewCount}</span>
                  </div>
                </div>
              </Link>
            ))
          )}
        </div>
      </div>

      {totalPages > 1 && (
        <div className="flex justify-center gap-2 mt-8">
          {Array.from({ length: totalPages }).map((_, i) => (
            <button
              key={i}
              onClick={() => {
                const params = new URLSearchParams(searchParams);
                params.set('page', i.toString());
                setSearchParams(params);
              }}
              className={`w-10 h-10 rounded-lg font-medium flex items-center justify-center transition-colors ${
                page === i 
                  ? 'bg-indigo-600 text-white shadow-md' 
                  : 'bg-white text-gray-700 hover:bg-gray-50 border border-gray-200'
              }`}
            >
              {i + 1}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}