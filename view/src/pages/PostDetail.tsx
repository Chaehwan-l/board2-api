// view/src/pages/PostDetail.tsx
import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import { format } from 'date-fns';
import { useAuth } from '../context/AuthContext';
import { Eye, Clock, MessageSquare, Trash2, Edit } from 'lucide-react';

export default function PostDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  // ✅ 버튼 노출 여부를 판단하기 위해 displayName(닉네임)을 가져옵니다.
  const { token, userPk, displayName } = useAuth();
  
  const [post, setPost] = useState<any>(null);
  const [commentInput, setCommentInput] = useState('');

  const fetchPost = async () => {
    try {
      const config = token ? { headers: { Authorization: `Bearer ${token}` } } : {};
      const res = await axios.get(`/posts/${id}`, config);
      if (res.data.success) {
        setPost(res.data.data);
      }
    } catch (err) {
      console.error(err);
      alert('게시글을 불러올 수 없습니다.');
      navigate('/');
    }
  };

  useEffect(() => {
    fetchPost();
  }, [id, token]);

  const handleDeletePost = async () => {
    if (!window.confirm('정말 삭제하시겠습니까?')) return;
    try {
      const config = token ? { headers: { Authorization: `Bearer ${token}` } } : {};
      await axios.delete(`/posts/${id}`, config);
      alert('삭제되었습니다.');
      navigate('/');
    } catch (err) {
      alert('삭제에 실패했습니다.');
    }
  };

  const handleCommentSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!commentInput.trim()) return;

    try {
      const config = token ? { headers: { Authorization: `Bearer ${token}` } } : {};
      await axios.post(`/posts/${id}/comments`, { content: commentInput }, config);
      setCommentInput('');
      fetchPost();
    } catch (err) {
      alert('댓글 작성에 실패했습니다.');
    }
  };

  const handleDeleteComment = async (commentId: number) => {
    if (!window.confirm('댓글을 삭제하시겠습니까?')) return;
    try {
      const config = token ? { headers: { Authorization: `Bearer ${token}` } } : {};
      await axios.delete(`/posts/${id}/comments/${commentId}`, config);
      fetchPost();
    } catch (err) {
      alert('댓글 삭제에 실패했습니다.');
    }
  };

  if (!post) return <div className="text-center py-20 text-gray-500">로딩 중...</div>;

  // ✅ 핵심 수정 포인트: PK가 아닌 닉네임으로 본인 여부를 판단합니다.
  const isAuthor = displayName && post.authorNickname === displayName;

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* 게시글 영역 */}
      <div className="bg-white p-6 sm:p-8 rounded-xl shadow-sm border border-gray-200">
        <div className="border-b border-gray-100 pb-6 mb-6">
          <h1 className="text-3xl font-bold text-gray-900 mb-4">{post.title}</h1>
          <div className="flex flex-wrap items-center justify-between text-sm text-gray-500 gap-4">
            <div className="flex items-center gap-4">
              <span className="font-semibold text-gray-700">{post.authorNickname}</span>
              <span className="flex items-center gap-1"><Clock className="w-4 h-4" /> {format(new Date(post.createdAt), 'yyyy. MM. dd. HH:mm')}</span>
            </div>
            <div className="flex items-center gap-4">
              <span className="flex items-center gap-1"><Eye className="w-4 h-4" /> {post.viewCount}</span>
            </div>
          </div>
        </div>

        <div className="prose max-w-none min-h-[200px] text-gray-800 whitespace-pre-wrap mb-8">
          {post.content}
        </div>

        {/* 첨부파일 영역 */}
        {post.attachments && post.attachments.length > 0 && (
          <div className="bg-gray-50 p-4 rounded-lg mb-8 border border-gray-200">
            <h3 className="text-sm font-semibold text-gray-700 mb-2">첨부파일</h3>
            <ul className="space-y-1">
              {post.attachments.map((file: any) => (
                <li key={file.id}>
                  <a href={file.fileUrl} target="_blank" rel="noreferrer" className="text-indigo-600 hover:underline text-sm flex items-center gap-1">
                    💾 {file.originalFileName}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        )}

        <div className="flex justify-between items-center pt-4 border-t border-gray-100">
          <button onClick={() => navigate(-1)} className="px-4 py-2 border border-gray-300 rounded-lg text-gray-600 hover:bg-gray-50">
            목록으로
          </button>
          
          {/* ✅ 본인이 작성한 글일 때만 수정/삭제 버튼 노출 */}
          {isAuthor && (
            <div className="flex gap-2">
              <Link to={`/posts/${post.id}/edit`} className="flex items-center gap-1 px-4 py-2 bg-indigo-50 text-indigo-600 rounded-lg hover:bg-indigo-100 font-medium">
                <Edit className="w-4 h-4" /> 수정
              </Link>
              <button onClick={handleDeletePost} className="flex items-center gap-1 px-4 py-2 bg-red-50 text-red-600 rounded-lg hover:bg-red-100 font-medium">
                <Trash2 className="w-4 h-4" /> 삭제
              </button>
            </div>
          )}
        </div>
      </div>

      {/* 댓글 영역 */}
      <div className="bg-white p-6 sm:p-8 rounded-xl shadow-sm border border-gray-200">
        <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2 mb-6">
          <MessageSquare className="w-5 h-5" /> 댓글 {post.comments ? post.comments.length : 0}개
        </h3>

        {/* 댓글 작성 폼 (로그인한 유저만) */}
        {userPk ? (
          <form onSubmit={handleCommentSubmit} className="mb-8 flex gap-2">
            <input
              type="text"
              value={commentInput}
              onChange={(e) => setCommentInput(e.target.value)}
              placeholder="댓글을 남겨보세요."
              className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
            />
            <button type="submit" className="px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 font-medium whitespace-nowrap">
              등록
            </button>
          </form>
        ) : (
          <div className="mb-8 p-4 bg-gray-50 text-center text-sm text-gray-500 rounded-lg border border-gray-200">
            댓글을 작성하려면 <Link to="/login" className="text-indigo-600 font-semibold underline">로그인</Link>이 필요합니다.
          </div>
        )}

        {/* 댓글 목록 */}
        <div className="space-y-4">
          {post.comments && post.comments.length > 0 ? (
            post.comments.map((comment: any) => (
              <div key={comment.id} className="pb-4 border-b border-gray-100 last:border-0 last:pb-0">
                <div className="flex justify-between items-start mb-1">
                  <span className="font-semibold text-gray-800 text-sm">{comment.authorNickname}</span>
                  <div className="flex items-center gap-3 text-xs text-gray-400">
                    <span>{format(new Date(comment.createdAt), 'yyyy. MM. dd. HH:mm')}</span>
                    {/* ✅ 댓글 삭제 버튼 로직도 닉네임 비교로 수정 */}
                    {displayName && comment.authorNickname === displayName && (
                      <button onClick={() => handleDeleteComment(comment.id)} className="text-red-400 hover:text-red-600">
                        삭제
                      </button>
                    )}
                  </div>
                </div>
                <p className="text-gray-700 text-sm whitespace-pre-wrap">{comment.content}</p>
              </div>
            ))
          ) : (
            <div className="text-center text-gray-500 text-sm py-4">첫 댓글을 남겨보세요.</div>
          )}
        </div>
      </div>
    </div>
  );
}