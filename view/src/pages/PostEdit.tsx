import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router';
import axios from 'axios';
import { useAuth } from '../context/AuthContext';
import { Upload, X, FileText } from 'lucide-react';

export default function PostEdit() {
  const { id } = useParams();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [existingAttachments, setExistingAttachments] = useState<any[]>([]);
  const [deletedAttachmentIds, setDeletedAttachmentIds] = useState<number[]>([]);
  const [newFiles, setNewFiles] = useState<File[]>([]);
  const { token, userId } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const fetchPost = async () => {
      try {
        const config = token ? { headers: { Authorization: `Bearer ${token}` } } : {};
        const res = await axios.get(`/posts/${id}`, config);
        if (res.data.success) {
          const post = res.data.data;
          if (String(post.authorId) !== userId) {
            alert('권한이 없습니다.');
            navigate('/');
            return;
          }
          setTitle(post.title);
          setContent(post.content);
          setExistingAttachments(post.attachments || []);
        }
      } catch (err) {
        console.error(err);
        navigate('/');
      }
    };
    fetchPost();
  }, [id, userId, navigate, token]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      setNewFiles(Array.from(e.target.files));
    }
  };

  const removeNewFile = (index: number) => {
    setNewFiles(newFiles.filter((_, i) => i !== index));
  };

  const removeExistingAttachment = (attachmentId: number) => {
    setDeletedAttachmentIds([...deletedAttachmentIds, attachmentId]);
    setExistingAttachments(existingAttachments.filter(a => a.id !== attachmentId));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;

    const formData = new FormData();
    formData.append('request', JSON.stringify({ title, content, deletedAttachmentIds }));
    newFiles.forEach(file => {
      formData.append('newFiles', file);
    });

    try {
      const res = await axios.put(`/posts/${id}`, formData, {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'multipart/form-data',
        },
      });
      if (res.data.success) {
        navigate(`/posts/${id}`);
      }
    } catch (err) {
      console.error(err);
      alert('게시글 수정에 실패했습니다.');
    }
  };

  return (
    <div className="max-w-3xl mx-auto">
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="p-8 border-b border-gray-100">
          <h1 className="text-2xl font-bold text-gray-900">게시글 수정</h1>
        </div>
        <form onSubmit={handleSubmit} className="p-8 space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">제목</label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all"
              placeholder="제목을 입력하세요"
              required
              maxLength={100}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">내용</label>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none resize-none min-h-[300px] transition-all"
              placeholder="내용을 입력하세요..."
              required
            />
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">기존 첨부파일</label>
            {existingAttachments.length > 0 ? (
              <ul className="space-y-2 mb-4">
                {existingAttachments.map((file) => (
                  <li key={file.id} className="flex items-center justify-between bg-gray-50 px-4 py-2 rounded-lg border border-gray-100">
                    <span className="text-sm text-gray-700 truncate flex items-center gap-2">
                      <FileText className="w-4 h-4 text-gray-400" /> {file.fileName}
                    </span>
                    <button type="button" onClick={() => removeExistingAttachment(file.id)} className="text-gray-400 hover:text-red-600 transition-colors">
                      <X className="w-4 h-4" />
                    </button>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-gray-500 mb-4">기존 첨부파일이 없습니다.</p>
            )}

            <label className="block text-sm font-medium text-gray-700 mb-2">새 첨부파일 추가</label>
            <div className="flex items-center gap-4">
              <label className="cursor-pointer bg-white border border-gray-300 text-gray-700 px-4 py-2 rounded-lg text-sm font-medium hover:bg-gray-50 transition-colors flex items-center gap-2">
                <Upload className="w-4 h-4" /> 파일 선택
                <input type="file" multiple onChange={handleFileChange} className="hidden" />
              </label>
              <span className="text-sm text-gray-500">{newFiles.length}개의 새 파일 선택됨</span>
            </div>
            {newFiles.length > 0 && (
              <ul className="mt-4 space-y-2">
                {newFiles.map((file, index) => (
                  <li key={index} className="flex items-center justify-between bg-indigo-50 px-4 py-2 rounded-lg border border-indigo-100">
                    <span className="text-sm text-indigo-700 truncate">{file.name}</span>
                    <button type="button" onClick={() => removeNewFile(index)} className="text-indigo-400 hover:text-red-600 transition-colors">
                      <X className="w-4 h-4" />
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
          <div className="pt-4 flex justify-end gap-3">
            <button type="button" onClick={() => navigate(-1)} className="px-6 py-2.5 border border-gray-300 text-gray-700 rounded-xl font-medium hover:bg-gray-50 transition-colors">
              취소
            </button>
            <button type="submit" className="px-6 py-2.5 bg-indigo-600 text-white rounded-xl font-medium hover:bg-indigo-700 transition-colors">
              수정하기
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
