import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router';
import axios from 'axios';
import { format } from 'date-fns';
import { useAuth } from '../context/AuthContext';
import { Trash2, Edit2, Download, MessageCircle, Clock, Eye } from 'lucide-react';

export default function PostDetail() {
  const { id } = useParams();
  const [post, setPost] = useState<any>(null);
  const [comment, setComment] = useState('');
  const { token, userId } = useAuth();
  const navigate = useNavigate();

  const fetchPost = async () => {
    try {
      const config = token ? { headers: { Authorization: `Bearer ${token}` } } : {};
      const res = await axios.get(`/posts/${id}`, config);
      if (res.data.success) {
        setPost(res.data.data);
      }
    } catch (err) {
      console.error(err);
      navigate('/');
    }
  };

  useEffect(() => {
    fetchPost();
  }, [id]);

  const handleDelete = async () => {
    if (!window.confirm('정말로 이 게시글을 삭제하시겠습니까?')) return;
    try {
      await axios.delete(`/posts/${id}`, { headers: { Authorization: `Bearer ${token}` } });
      navigate('/');
    } catch (err) {
      console.error(err);
    }
  };

  const handleCommentSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!comment.trim()) return;
    try {
      await axios.post(`/posts/${id}/comments`, { content: comment }, { headers: { Authorization: `Bearer ${token}` } });
      setComment('');
      fetchPost();
    } catch (err) {
      console.error(err);
    }
  };

  const handleDeleteComment = async (commentId: number) => {
    if (!window.confirm('이 댓글을 삭제하시겠습니까?')) return;
    try {
      await axios.delete(`/posts/comments/${commentId}`, { headers: { Authorization: `Bearer ${token}` } });
      fetchPost();
    } catch (err) {
      console.error(err);
    }
  };

  if (!post) return <div className="flex justify-center py-20"><div className="animate-pulse flex space-x-4"><div className="rounded-full bg-slate-200 h-10 w-10"></div><div className="flex-1 space-y-6 py-1"><div className="h-2 bg-slate-200 rounded"></div><div className="space-y-3"><div className="grid grid-cols-3 gap-4"><div className="h-2 bg-slate-200 rounded col-span-2"></div><div className="h-2 bg-slate-200 rounded col-span-1"></div></div><div className="h-2 bg-slate-200 rounded"></div></div></div></div></div>;

  const isAuthor = userId === String(post.authorId);

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="p-8 border-b border-gray-100">
          <div className="flex justify-between items-start mb-6">
            <h1 className="text-3xl font-bold text-gray-900 leading-tight">{post.title}</h1>
            {isAuthor && (
              <div className="flex gap-2 shrink-0 ml-4">
                <Link to={`/posts/${id}/edit`} className="p-2 text-gray-500 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors">
                  <Edit2 className="w-5 h-5" />
                </Link>
                <button onClick={handleDelete} className="p-2 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors">
                  <Trash2 className="w-5 h-5" />
                </button>
              </div>
            )}
          </div>
          
          <div className="flex items-center gap-6 text-sm text-gray-500">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-600 font-bold">
                {post.authorNickname.charAt(0).toUpperCase()}
              </div>
              <span className="font-medium text-gray-900">{post.authorNickname}</span>
            </div>
            <div className="flex items-center gap-1.5"><Clock className="w-4 h-4" /> {format(new Date(post.createdAt), 'yyyy. MM. dd. HH:mm')}</div>
            <div className="flex items-center gap-1.5"><Eye className="w-4 h-4" /> 조회 {post.viewCount}</div>
          </div>
        </div>
        
        <div className="p-8 prose prose-indigo max-w-none text-gray-700 whitespace-pre-wrap">
          {post.content}
        </div>

        {post.attachments && post.attachments.length > 0 && (
          <div className="p-8 bg-gray-50 border-t border-gray-100">
            <h3 className="text-sm font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <Download className="w-4 h-4" /> 첨부파일
            </h3>
            <ul className="space-y-2">
              {post.attachments.map((file: any) => (
                <li key={file.id}>
                  <a href={`/uploads/${file.s3Key}`} target="_blank" rel="noopener noreferrer" className="flex items-center gap-2 text-sm text-indigo-600 hover:text-indigo-800 hover:underline bg-white p-3 rounded-lg border border-gray-200 shadow-sm transition-all">
                    <Download className="w-4 h-4" /> {file.fileName}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="p-6 border-b border-gray-100 flex items-center gap-2">
          <MessageCircle className="w-5 h-5 text-gray-500" />
          <h3 className="text-lg font-bold text-gray-900">댓글 ({post.comments?.length || 0})</h3>
        </div>
        
        <div className="p-6 space-y-6">
          {token ? (
            <form onSubmit={handleCommentSubmit} className="flex gap-4 items-start">
              <div className="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center text-gray-500 font-bold shrink-0">
                {userId?.charAt(0).toUpperCase()}
              </div>
              <div className="flex-1 relative">
                <textarea
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  placeholder="댓글을 작성해보세요..."
                  className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none resize-none min-h-[100px] transition-all"
                  required
                />
                <button type="submit" className="absolute bottom-3 right-3 bg-indigo-600 text-white px-4 py-1.5 rounded-lg text-sm font-medium hover:bg-indigo-700 transition-colors">
                  등록
                </button>
              </div>
            </form>
          ) : (
            <div className="bg-gray-50 p-6 rounded-xl text-center border border-gray-200">
              <p className="text-gray-600 mb-3">댓글을 작성하려면 로그인해주세요.</p>
              <Link to="/login" className="inline-block bg-white border border-gray-300 text-gray-700 px-6 py-2 rounded-lg text-sm font-medium hover:bg-gray-50 transition-colors">
                로그인
              </Link>
            </div>
          )}

          <div className="space-y-6 mt-8">
            {post.comments?.map((c: any) => (
              <div key={c.id} className="flex gap-4">
                <div className="w-10 h-10 rounded-full bg-indigo-50 flex items-center justify-center text-indigo-600 font-bold shrink-0">
                  {c.authorNickname.charAt(0).toUpperCase()}
                </div>
                <div className="flex-1 bg-gray-50 p-4 rounded-2xl rounded-tl-none border border-gray-100">
                  <div className="flex justify-between items-start mb-2">
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-gray-900 text-sm">{c.authorNickname}</span>
                      <span className="text-xs text-gray-500">{format(new Date(c.createdAt), 'yyyy. MM. dd. HH:mm')}</span>
                    </div>
                    {userId === String(c.authorId) && (
                      <button onClick={() => handleDeleteComment(c.id)} className="text-gray-400 hover:text-red-600 transition-colors">
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                  <p className="text-gray-700 text-sm whitespace-pre-wrap">{c.content}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
