import React, { useState } from 'react';
import { useNavigate } from 'react-router';
import axios from 'axios';
import { useAuth } from '../context/AuthContext';
import { Upload, X } from 'lucide-react';

export default function PostCreate() {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [files, setFiles] = useState<File[]>([]);
  const { token } = useAuth();
  const navigate = useNavigate();

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      setFiles(Array.from(e.target.files));
    }
  };

  const removeFile = (index: number) => {
    setFiles(files.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;

    const formData = new FormData();
    
    // 핵심 로직: Spring의 @RequestPart가 DTO로 인식할 수 있도록 Blob을 사용해 application/json 명시
    const requestBlob = new Blob(
      [JSON.stringify({ title, content })], 
      { type: 'application/json' }
    );
    formData.append('request', requestBlob);

    files.forEach(file => {
      formData.append('files', file);
    });

    try {
      const res = await axios.post('/posts', formData, {
        headers: {
          Authorization: `Bearer ${token}`,
          // 주의: Axios에서 FormData를 보낼 때는 'Content-Type': 'multipart/form-data'를 
          // 직접 적으면 안 됩니다. (브라우저가 생성하는 Boundary 값이 누락되어 파싱 실패함)
        },
      });
      if (res.data.success) {
        navigate(`/posts/${res.data.data}`); // 백엔드가 넘겨준 postId로 이동
      }
    } catch (err) {
      console.error(err);
      alert('게시글 작성에 실패했습니다.');
    }
  };

  return (
    <div className="max-w-3xl mx-auto">
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="p-8 border-b border-gray-100">
          <h1 className="text-2xl font-bold text-gray-900">게시글 작성</h1>
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
            <label className="block text-sm font-medium text-gray-700 mb-2">첨부파일</label>
            <div className="flex items-center gap-4">
              <label className="cursor-pointer bg-white border border-gray-300 text-gray-700 px-4 py-2 rounded-lg text-sm font-medium hover:bg-gray-50 transition-colors flex items-center gap-2">
                <Upload className="w-4 h-4" /> 파일 선택
                <input type="file" multiple onChange={handleFileChange} className="hidden" />
              </label>
              <span className="text-sm text-gray-500">{files.length}개 파일 선택됨</span>
            </div>
            {files.length > 0 && (
              <ul className="mt-4 space-y-2">
                {files.map((file, index) => (
                  <li key={index} className="flex items-center justify-between bg-gray-50 px-4 py-2 rounded-lg border border-gray-100">
                    <span className="text-sm text-gray-700 truncate">{file.name}</span>
                    <button type="button" onClick={() => removeFile(index)} className="text-gray-400 hover:text-red-600 transition-colors">
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
              등록
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
