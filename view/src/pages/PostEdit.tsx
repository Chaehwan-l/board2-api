// view/src/pages/PostEdit.tsx
import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../context/AuthContext';
import { Upload, X } from 'lucide-react'; // 아이콘 추가

export default function PostEdit() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { token, displayName } = useAuth();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  
  // 백엔드 연동을 위한 파일 상태 관리
  const [existingFiles, setExistingFiles] = useState<any[]>([]); // 기존에 업로드되어 있던 파일 목록
  const [deletedAttachmentIds, setDeletedAttachmentIds] = useState<number[]>([]); // 유저가 삭제 버튼을 누른 기존 파일의 ID 목록
  const [newFiles, setNewFiles] = useState<File[]>([]); // 새롭게 추가할 파일 목록

  useEffect(() => {
    const fetchPost = async () => {
      try {
        const config = token ? { headers: { Authorization: `Bearer ${token}` } } : {};
        const res = await axios.get(`/posts/${id}`, config);
        
        if (res.data.success) {
          const postData = res.data.data;
          
          if (!displayName || postData.authorNickname !== displayName) {
            alert('수정 권한이 없습니다.');
            navigate(`/posts/${id}`, { replace: true });
            return;
          }
          
          setTitle(postData.title);
          setContent(postData.content);
          
          // 기존 첨부파일이 있다면 상태에 세팅
          if (postData.attachments) {
            setExistingFiles(postData.attachments);
          }
        }
      } catch (err) {
        alert('게시글을 불러올 수 없습니다.');
        navigate(-1);
      }
    };
    fetchPost();
  }, [id, token, displayName, navigate]);

  // 새 파일 추가 핸들러
  const handleNewFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      setNewFiles([...newFiles, ...Array.from(e.target.files)]);
    }
  };

  // 추가하려던 새 파일 취소
  const removeNewFile = (index: number) => {
    setNewFiles(newFiles.filter((_, i) => i !== index));
  };

  // 기존에 있던 파일 삭제 (실제 삭제는 저장 시 백엔드에서 처리되도록 ID만 보관)
  const removeExistingFile = (fileId: number) => {
    setDeletedAttachmentIds([...deletedAttachmentIds, fileId]);
    setExistingFiles(existingFiles.filter(f => f.id !== fileId)); // 화면에서 숨김
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;

    try {
      const config = token ? { headers: { Authorization: `Bearer ${token}` } } : {};
      const formData = new FormData();
      
      // 1. JSON 데이터 구성 (수정된 제목, 내용, 그리고 삭제할 기존 파일 ID 목록 포함)
      const requestData = {
        title,
        content,
        deletedAttachmentIds // PostUpdateRequest DTO에 매핑됨
      };

      const requestBlob = new Blob(
        [JSON.stringify(requestData)], 
        { type: 'application/json' }
      );
      
      formData.append('request', requestBlob);
      
      // 2. 새로 추가할 파일 멀티파트 매핑 (백엔드 컨트롤러의 @RequestPart("newFiles")와 이름 일치)
      newFiles.forEach(file => {
        formData.append('newFiles', file);
      });
      
      const res = await axios.put(`/posts/${id}`, formData, config);
      
      if (res.data.success) {
        alert('수정되었습니다.');
        navigate(`/posts/${id}`);
      }
    } catch (err) {
      console.error(err);
      alert('게시글 수정에 실패했습니다.');
    }
  };

  return (
    <div className="max-w-3xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">게시글 수정</h1>
      
      <form onSubmit={handleSubmit} className="space-y-6 bg-white p-6 rounded-xl shadow-sm border border-gray-200">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">제목</label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all"
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">내용</label>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={10}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition-all resize-y"
            required
          />
        </div>

        {/* 파일 첨부 영역 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">첨부파일 관리</label>
          
          {/* 기존 업로드 파일 목록 */}
          {existingFiles.length > 0 && (
            <div className="mb-4">
              <span className="text-xs text-gray-500 mb-1 block">기존 업로드 파일</span>
              <ul className="space-y-2">
                {existingFiles.map(file => (
                  <li key={file.id} className="flex items-center justify-between py-2 px-3 bg-gray-50 rounded-lg border border-gray-200">
                    <span className="text-sm text-gray-600 truncate">💾 {file.originalFileName}</span>
                    <button type="button" onClick={() => removeExistingFile(file.id)} className="text-gray-400 hover:text-red-500 p-1" title="삭제">
                      <X className="w-4 h-4" />
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* 새 파일 추가 UI */}
          <div className="mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-gray-300 border-dashed rounded-lg hover:border-indigo-500 transition-colors">
            <div className="space-y-1 text-center">
              <Upload className="mx-auto h-12 w-12 text-gray-400" />
              <div className="flex text-sm text-gray-600 justify-center">
                <label className="relative cursor-pointer bg-white rounded-md font-medium text-indigo-600 hover:text-indigo-500 focus-within:outline-none focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-indigo-500">
                  <span>새 파일 추가</span>
                  <input type="file" multiple className="sr-only" onChange={handleNewFileChange} />
                </label>
              </div>
            </div>
          </div>
          
          {/* 추가 예정인 새 파일 목록 */}
          {newFiles.length > 0 && (
            <div className="mt-4">
              <span className="text-xs text-indigo-500 mb-1 block">추가 예정 파일</span>
              <ul className="space-y-2">
                {newFiles.map((file, index) => (
                  <li key={index} className="flex items-center justify-between py-2 px-3 bg-indigo-50 rounded-lg border border-indigo-100">
                    <span className="text-sm text-indigo-700 truncate">{file.name}</span>
                    <button type="button" onClick={() => removeNewFile(index)} className="text-indigo-400 hover:text-red-500 p-1" title="취소">
                      <X className="w-4 h-4" />
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>

        <div className="flex justify-end gap-3 pt-4 border-t border-gray-100">
          <button type="button" onClick={() => navigate(-1)} className="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 font-medium hover:bg-gray-50 transition-colors">
            취소
          </button>
          <button type="submit" className="px-6 py-2 bg-indigo-600 text-white rounded-lg font-medium hover:bg-indigo-700 transition-colors shadow-sm">
            수정완료
          </button>
        </div>
      </form>
    </div>
  );
}