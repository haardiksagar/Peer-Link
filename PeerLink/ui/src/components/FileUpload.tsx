'use client';

import { useState, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { FiUpload } from 'react-icons/fi';

interface FileUploadProps {
  onFileUpload: (file: File) => void;
  isUploading: boolean;
}

export default function FileUpload({ onFileUpload, isUploading }: FileUploadProps) {
  const [dragActive, setDragActive] = useState(false);

  const onDrop = useCallback((acceptedFiles: File[]) => {
    if (acceptedFiles.length > 0) {
      onFileUpload(acceptedFiles[0]);
    }
  }, [onFileUpload]);

  const { getRootProps, getInputProps } = useDropzone({
    onDrop,
    multiple: false,
    onDragEnter: () => setDragActive(true),
    onDragLeave: () => setDragActive(false),
    onDropAccepted: () => setDragActive(false),
    onDropRejected: () => setDragActive(false),
  });

  return (
    <div
      {...getRootProps()}
      className={`
        w-full py-8 px-6 bg-[#1a1a1a]/40 backdrop-blur-md border border-[#3c3c3c] rounded-2xl text-center cursor-pointer transition-colors
        ${dragActive ? 'border-white' : 'hover:border-[#7e7e7e]'}
        ${isUploading ? 'opacity-50 pointer-events-none' : ''}
      `}
    >
      <input {...getInputProps()} />
      <div className="flex flex-col items-center justify-center space-y-4">
        <div className="btn-icon">
          <FiUpload className="w-6 h-6 text-white" />
        </div>
        <div>
          <h3 className="title-lg text-white mb-2 uppercase">Select a File to Share</h3>
          <p className="body-md text-[#bbbbbb]">
            Drag & drop or click to browse. Secure P2P transfer.
          </p>
        </div>
      </div>
    </div>
  );
}
