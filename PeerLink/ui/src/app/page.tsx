'use client';

import { useState } from 'react';
import FileUpload from '@/components/FileUpload';
import FileDownload from '@/components/FileDownload';
import InviteCode from '@/components/InviteCode';
import axios from 'axios';

export default function Home() {
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [isDownloading, setIsDownloading] = useState(false);
  const [port, setPort] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState<'upload' | 'download'>('upload');

  const handleFileUpload = async (file: File) => {
    setUploadedFile(file);
    setIsUploading(true);
    
    try {
      const formData = new FormData();
      formData.append('file', file);
      
      const response = await axios.post('/api/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      
      setPort(response.data.port);
    } catch (error) {
      console.error('Error uploading file:', error);
      alert('Failed to upload file. Please try again.');
    } finally {
      setIsUploading(false);
    }
  };
  
  const handleDownload = async (port: number) => {
    setIsDownloading(true);
    
    try {
      // Request download from Java backend
      const response = await axios.get(`/api/download/${port}`, {
        responseType: 'blob',
      });
      
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      
      // Try to get filename from response headers
      // Axios normalizes headers to lowercase, but we need to handle different cases
      const headers = response.headers;
      let contentDisposition = '';
      
      // Look for content-disposition header regardless of case
      for (const key in headers) {
        if (key.toLowerCase() === 'content-disposition') {
          contentDisposition = headers[key];
          break;
        }
      }
      
      let filename = 'downloaded-file';
      
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="(.+)"/);
        if (filenameMatch && filenameMatch.length === 2) {
          filename = filenameMatch[1];
        }
      }
      
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (error) {
      console.error('Error downloading file:', error);
      alert('Failed to download file. Please check the invite code and try again.');
    } finally {
      setIsDownloading(false);
    }
  };

  return (
    <div className="w-full max-w-[1440px] mx-auto pb-24">
      {/* Hero Band */}
      <div className="w-full py-[96px] px-8 border-b border-[#3c3c3c] flex flex-col items-start justify-center">
        <h1 className="display-xl mb-4">THE ULTIMATE P2P SHARE.</h1>
        <p className="title-md max-w-2xl text-[#bbbbbb]">Securely send and receive files directly with peers. No servers. No limits. Engineered precision for your data.</p>
      </div>

      <div className="w-full px-8 py-[96px]">
        <div className="flex space-x-8 border-b border-[#3c3c3c] mb-12">
          <button
            className={`category-tab ${activeTab === 'upload' ? 'category-tab-active' : ''}`}
            onClick={() => setActiveTab('upload')}
          >
            SHARE A FILE
          </button>
          <button
            className={`category-tab ${activeTab === 'download' ? 'category-tab-active' : ''}`}
            onClick={() => setActiveTab('download')}
          >
            RECEIVE A FILE
          </button>
        </div>
        
        <div className="max-w-3xl">
          {activeTab === 'upload' ? (
            <div>
              <FileUpload onFileUpload={handleFileUpload} isUploading={isUploading} />
              
              {uploadedFile && !isUploading && (
                <div className="mt-8 p-6 bg-[#1a1a1a] border border-[#3c3c3c]">
                  <p className="body-md">
                    SELECTED FILE: <span className="font-bold text-white uppercase">{uploadedFile.name}</span> ({Math.round(uploadedFile.size / 1024)} KB)
                  </p>
                </div>
              )}
              
              {isUploading && (
                <div className="mt-8 text-left">
                  <p className="body-md text-white">UPLOADING FILE...</p>
                </div>
              )}
              
              <InviteCode port={port} />
            </div>
          ) : (
            <div>
              <FileDownload onDownload={handleDownload} isDownloading={isDownloading} />
              
              {isDownloading && (
                <div className="mt-8 text-left">
                  <p className="body-md text-white">DOWNLOADING FILE...</p>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
      
      <footer className="w-full px-8 py-16 border-t border-[#3c3c3c]">
        <p className="caption text-[#7e7e7e]">PEERLINK &copy; {new Date().getFullYear()} — SECURE P2P FILE SHARING</p>
      </footer>
    </div>
  );
}
